package com.example.musicapp.controller.player

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicapp.domain.model.LyricLine
import com.example.musicapp.domain.model.LyricMatcher
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.usecase.AddListenDurationUseCase
import com.example.musicapp.domain.usecase.GetLikedSongIdsUseCase
import com.example.musicapp.domain.usecase.GetSongLyricsUseCase
import com.example.musicapp.domain.usecase.GetSongUrlUseCase
import com.example.musicapp.domain.usecase.LikeSongUseCase
import com.example.musicapp.domain.usecase.ObserveLoginStateUseCase
import com.example.musicapp.domain.usecase.RecordRecentPlayUseCase
import com.example.musicapp.domain.usecase.RecordWeekPlayUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

// 播放模式：随机 / 列表循环 / 单曲循环
enum class PlayerPlayMode {
    Shuffle,
    Loop,
    Single
}

// 全局播放状态
data class PlaybackState(
    // 当前正在播放的歌曲；为 null 表示尚未真正开始播
    val currentSong: Song? = null,
    // 迷你栏预览歌曲；未播放时用于展示封面和歌名
    val previewSong: Song? = null,
    // 是否正在播放，由 ExoPlayer 回调同步
    val isPlaying: Boolean = false,
    // 是否正在拉取播放地址
    val isLoading: Boolean = false,
    // 当前歌曲是否已收藏，与网易云红心歌单同步
    val isFavorite: Boolean = false,
    // 最近一次成功获取到的流媒体地址
    val playUrl: String? = null,
    // 播放或拉取地址时的错误信息
    val error: String? = null,
    // 当前播放队列
    val queue: List<Song> = emptyList(),
    // 当前歌曲在队列中的下标，供 skipToNext 使用
    val queueIndex: Int = 0,
    // 当前歌曲的 LRC 歌词
    val lyrics: List<LyricLine> = emptyList(),
    // 顶栏/迷你场景展示的当前歌词句
    val currentLyricLine: String = "听点音乐吧",
    // ExoPlayer 播放进度（毫秒）
    val currentPositionMs: Long = 0L,
    // ExoPlayer 时长（毫秒）；未就绪时为 0
    val durationMs: Long = 0L,
    // 全局播放模式，驱动自动切歌与手动上下首
    val playMode: PlayerPlayMode = PlayerPlayMode.Shuffle
) {
    // 迷你栏展示用：优先显示正在播的歌，否则显示预览歌
    val displaySong: Song?
        get() = currentSong ?: previewSong
}

// 全局音乐播放控制器
// Hilt 单例，整个 App 共享同一个 ExoPlayer 实例
// 负责拉取播放地址、驱动 ExoPlayer、维护 PlaybackState
// 不依赖 ViewModel / NavGraph，由各处通过 Hilt 注入获取
@Singleton
class MusicPlayerController @Inject constructor(
    @ApplicationContext context: Context,
    private val getSongUrlUseCase: GetSongUrlUseCase,
    private val getSongLyricsUseCase: GetSongLyricsUseCase,
    // 播放成功后写入本地最近播放
    private val recordRecentPlayUseCase: RecordRecentPlayUseCase,
    // 本周播放次数 +1
    private val recordWeekPlayUseCase: RecordWeekPlayUseCase,
    // 累加实际听歌时长
    private val addListenDurationUseCase: AddListenDurationUseCase,
    private val likeSongUseCase: LikeSongUseCase,
    private val getLikedSongIdsUseCase: GetLikedSongIdsUseCase,
    private val observeLoginStateUseCase: ObserveLoginStateUseCase
) {
    // 底层播放器，真正负责解码和出声
    // lazy：首次访问时才创建，避免 App 启动时即占用音频资源
    val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().also { player ->
            // 监听 ExoPlayer 播放状态变化，同步到 PlaybackState.isPlaying
            player.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playbackState.update { it.copy(isPlaying = isPlaying) }
                        if (isPlaying) {
                            markListeningStarted()
                            startPositionTracking()
                        } else {
                            settleListenDuration()
                            syncPositionAndLyric()
                            positionJob?.cancel()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> syncPositionAndLyric()
                            Player.STATE_ENDED -> onPlaybackEnded()
                        }
                    }
                }
            )
        }
    }

    // 控制器自己的协程作用域
    // SupervisorJob：某个播放任务异常不会拖垮整个 scope
    // Main.immediate：状态更新和 ExoPlayer 调用保持在主线程
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // 内部可变状态
    private val _playbackState = MutableStateFlow(PlaybackState())
    // 对外只读，UI 层 collect 这个 Flow
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var positionJob: Job? = null
    private var lyricJob: Job? = null
    // 拉取播放地址的任务；切歌时取消，避免旧 URL 覆盖新歌
    private var urlJob: Job? = null
    // 当前登录用户收藏的歌曲 ID 缓存，用于切歌时同步红心状态
    private var likedSongIds: Set<Long> = emptySet()
    private var currentUserId: Long? = null
    // 当前连续出声区间的起点（elapsedRealtime），用于结算实际听歌时长
    private var listeningSinceElapsedRealtime: Long? = null

    init {
        // 初始化时只取一次登录态，用于同步红心列表
        scope.launch {
            val loginState = observeLoginStateUseCase().first()
            currentUserId = loginState.userId?.takeIf { loginState.isLoggedIn }
            if (currentUserId != null) {
                refreshLikedSongIds(currentUserId!!)
            } else {
                likedSongIds = emptySet()
                _playbackState.update { it.copy(isFavorite = false) }
            }
        }
    }

    // 设置迷你栏预览歌曲
    // 典型场景：首页加载出精选歌、搜索页点选歌曲但尚未播放
    // 若当前还没有队列，则把这首歌作为初始队列
    fun setPreviewSong(song: Song) {
        _playbackState.update { state ->
            state.copy(
                previewSong = song,
                queue = state.queue.ifEmpty { listOf(song) },
                currentLyricLine = formatFallbackLyric(song)
            )
        }
        syncFavoriteForCurrentSong()
    }

    // 播放指定歌曲
    // song：要播放的歌曲
    // queue：播放队列；为空时退化为只播当前这一首
    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        // 切歌前先结算上一首已听时长
        settleListenDuration()
        // 没传队列时，用当前歌曲单首组成队列
        val resolvedQueue = queue.ifEmpty { listOf(song) }
        // 找到当前歌曲在队列中的位置，供下一首切换使用
        val queueIndex = resolvedQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        positionJob?.cancel()
        lyricJob?.cancel()
        urlJob?.cancel()

        // 先同步 UI 状态：切歌、进入 loading、清空旧错误和旧地址
        _playbackState.update {
            it.copy(
                currentSong = song,
                queue = resolvedQueue,
                queueIndex = queueIndex,
                isLoading = true,
                error = null,
                playUrl = null,
                lyrics = emptyList(),
                currentLyricLine = formatFallbackLyric(song),
                currentPositionMs = 0L,
                durationMs = 0L
            )
        }

        loadLyrics(song.id)
        syncFavoriteForCurrentSong()

        // 异步向服务端请求真实可播放地址；切歌时会取消，并二次校验 songId
        val requestSongId = song.id
        urlJob = scope.launch {
            try {
                val songUrl = getSongUrlUseCase(requestSongId)
                // 请求返回时可能已切到别的歌，丢弃过期结果
                if (_playbackState.value.currentSong?.id != requestSongId) return@launch

                val url = songUrl.url
                if (url.isNullOrBlank()) {
                    _playbackState.update {
                        it.copy(
                            isLoading = false,
                            error = "该歌曲暂无播放权限"
                        )
                    }
                } else {
                    _playbackState.update {
                        it.copy(isLoading = false, playUrl = url, error = null)
                    }
                    recordPlayStats(song)
                    exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                if (_playbackState.value.currentSong?.id != requestSongId) return@launch
                _playbackState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "获取播放地址失败"
                    )
                }
            }
        }
    }

    // 播放/暂停切换
    // 若还没有 currentSong，则先把 previewSong 真正播起来
    // 若已在播，则直接控制 ExoPlayer 暂停/继续
    fun togglePlayPause() {
        val state = _playbackState.value
        if (state.currentSong == null) {
            state.previewSong?.let { preview ->
                playSong(preview, state.queue.ifEmpty { listOf(preview) })
            }
            return
        }
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    // 当前曲播完：单曲重播；列表循环 / 随机则按模式切下一首
    private fun onPlaybackEnded() {
        when (_playbackState.value.playMode) {
            PlayerPlayMode.Single -> {
                exoPlayer.seekTo(0)
                exoPlayer.play()
            }
            PlayerPlayMode.Loop, PlayerPlayMode.Shuffle -> skipToNext()
        }
    }

    // 播放队列中的下一首（行为随 playMode 变化）
    fun skipToNext() {
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return
        val nextIndex = resolveNextIndex(state)
        playSong(queue[nextIndex], queue)
    }

    // 播放队列中的上一首（行为随 playMode 变化）
    fun skipToPrevious() {
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return
        when (state.playMode) {
            PlayerPlayMode.Shuffle -> {
                playSong(queue[resolveRandomIndex(queue, state.queueIndex)], queue)
            }
            PlayerPlayMode.Loop -> {
                val previousIndex = if (state.queueIndex <= 0) {
                    queue.lastIndex
                } else {
                    state.queueIndex - 1
                }
                playSong(queue[previousIndex], queue)
            }
            PlayerPlayMode.Single -> {
                // 手动上一首仍切队列；已在队首则回到当前曲开头
                if (state.queueIndex <= 0) {
                    exoPlayer.seekTo(0)
                    syncPositionAndLyric()
                } else {
                    playSong(queue[state.queueIndex - 1], queue)
                }
            }
        }
    }

    // 随机 → 列表循环 → 单曲循环 → 随机
    fun cyclePlayMode() {
        _playbackState.update { state ->
            state.copy(
                playMode = when (state.playMode) {
                    PlayerPlayMode.Shuffle -> PlayerPlayMode.Loop
                    PlayerPlayMode.Loop -> PlayerPlayMode.Single
                    PlayerPlayMode.Single -> PlayerPlayMode.Shuffle
                }
            )
        }
    }

    // 供 skipToNext 使用：含手动下一首（单曲模式播完不会走到这里，点下一首会）
    private fun resolveNextIndex(state: PlaybackState): Int {
        val queue = state.queue
        return when (state.playMode) {
            PlayerPlayMode.Shuffle -> resolveRandomIndex(queue, state.queueIndex)
            // Loop 自动切歌，以及 Loop / Single 下手动下一首：顺序 + 环绕
            PlayerPlayMode.Loop, PlayerPlayMode.Single ->
                (state.queueIndex + 1) % queue.size
        }
    }

    // 队列多于 1 首时避开当前下标，避免「随机」抽到同一首
    private fun resolveRandomIndex(queue: List<Song>, currentIndex: Int): Int {
        if (queue.size <= 1) return 0
        var next = Random.nextInt(queue.size)
        while (next == currentIndex) {
            next = Random.nextInt(queue.size)
        }
        return next
    }

    // 跳转到指定播放进度
    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
        syncPositionAndLyric()
    }

    // 从播放队列指定位置开始播放
    fun playQueueItemAt(index: Int) {
        val state = _playbackState.value
        if (index !in state.queue.indices) return
        playSong(state.queue[index], state.queue)
    }

    // 切换当前歌曲收藏状态，调用网易云 like 接口
    fun toggleFavorite() {
        val state = _playbackState.value
        val songId = state.currentSong?.id ?: state.previewSong?.id ?: return
        if (currentUserId == null) {
            _playbackState.update { it.copy(error = "请先登录后收藏") }
            return
        }

        val targetFavorite = !state.isFavorite
        _playbackState.update { it.copy(isFavorite = targetFavorite, error = null) }
        likedSongIds = if (targetFavorite) likedSongIds + songId else likedSongIds - songId

        scope.launch {
            runCatching {
                likeSongUseCase(songId, like = targetFavorite)
            }.onSuccess { result ->
                if (!result.success) {
                    revertFavoriteState(songId, targetFavorite, "收藏操作失败")
                }
            }.onFailure { throwable ->
                revertFavoriteState(
                    songId = songId,
                    attemptedFavorite = targetFavorite,
                    message = throwable.message ?: "收藏操作失败"
                )
            }
        }
    }

    // 拉取用户红心歌单 ID 并刷新当前歌曲收藏状态
    private fun refreshLikedSongIds(userId: Long) {
        scope.launch {
            runCatching {
                getLikedSongIdsUseCase(userId)
            }.onSuccess { ids ->
                likedSongIds = ids.toSet()
                syncFavoriteForCurrentSong()
            }
        }
    }

    // 根据缓存的红心列表同步当前展示歌曲的收藏状态
    private fun syncFavoriteForCurrentSong() {
        val songId = _playbackState.value.currentSong?.id
            ?: _playbackState.value.previewSong?.id
            ?: return
        val isFavorite = songId in likedSongIds
        _playbackState.update { state ->
            if (state.isFavorite == isFavorite) state else state.copy(isFavorite = isFavorite)
        }
    }

    // 接口失败时回滚收藏状态
    private fun revertFavoriteState(songId: Long, attemptedFavorite: Boolean, message: String) {
        likedSongIds = if (attemptedFavorite) likedSongIds - songId else likedSongIds + songId
        _playbackState.update {
            it.copy(isFavorite = !attemptedFavorite, error = message)
        }
    }

    // 异步写入最近播放与本周次数，失败不影响当前播放流程
    private fun recordPlayStats(song: Song) {
        scope.launch {
            runCatching { recordRecentPlayUseCase(song) }
            runCatching { recordWeekPlayUseCase() }
        }
    }

    // 标记一段连续出声区间的起点
    private fun markListeningStarted() {
        if (listeningSinceElapsedRealtime == null) {
            listeningSinceElapsedRealtime = SystemClock.elapsedRealtime()
        }
    }

    // 结算自上次出声以来的实际听歌时长并落库
    private fun settleListenDuration() {
        val since = listeningSinceElapsedRealtime ?: return
        listeningSinceElapsedRealtime = null
        val elapsedMs = SystemClock.elapsedRealtime() - since
        if (elapsedMs <= 0L) return
        scope.launch {
            runCatching { addListenDurationUseCase(elapsedMs) }
        }
    }

    // 异步拉取歌曲歌词；切歌时会取消上一次请求，避免旧歌词覆盖新歌
    private fun loadLyrics(songId: Long) {
        lyricJob?.cancel()
        lyricJob = scope.launch {
            runCatching {
                getSongLyricsUseCase(songId)
            }.onSuccess { lyrics ->
                _playbackState.update { state ->
                    // 请求返回时可能已切到别的歌，丢弃过期结果
                    if (state.currentSong?.id != songId) return@update state
                    state.copy(lyrics = lyrics)
                }
                if (_playbackState.value.currentSong?.id == songId) {
                    syncPositionAndLyric()
                }
                if (exoPlayer.isPlaying) {
                    startPositionTracking()
                }
            }
        }
    }

    // 播放中每 200ms 同步进度与当前歌词句
    private fun startPositionTracking() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive && exoPlayer.isPlaying) {
                syncPositionAndLyric()
                delay(200)
            }
        }
    }

    private fun syncPositionAndLyric() {
        val positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
        val durationMs = exoPlayer.duration.coerceAtLeast(0L)
        val state = _playbackState.value
        val lyricLine = resolveLyricLine(
            lyrics = state.lyrics,
            positionMs = positionMs,
            song = state.currentSong ?: state.previewSong
        )
        if (state.currentPositionMs == positionMs &&
            state.durationMs == durationMs &&
            state.currentLyricLine == lyricLine
        ) {
            return
        }
        _playbackState.update {
            it.copy(
                currentPositionMs = positionMs,
                durationMs = durationMs,
                currentLyricLine = lyricLine
            )
        }
    }

    // 优先返回当前进度匹配的歌词；无歌词时回退到歌名，都没有则显示默认文案
    private fun resolveLyricLine(
        lyrics: List<LyricLine>,
        positionMs: Long,
        song: Song?
    ): String {
        val lyricText = LyricMatcher.currentLine(lyrics, positionMs)
        return when {
            lyricText != null -> "\"$lyricText\""
            song != null -> formatFallbackLyric(song)
            else -> "听点音乐吧"
        }
    }

    private fun formatFallbackLyric(song: Song): String = "\"${song.name}\""
}
