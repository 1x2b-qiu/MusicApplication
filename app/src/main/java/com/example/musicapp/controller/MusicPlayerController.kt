package com.example.musicapp.controller

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicapp.domain.model.LyricLine
import com.example.musicapp.domain.model.LyricMatcher
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.usecase.stats.AddListenDurationUseCase
import com.example.musicapp.domain.usecase.music.GetLikedSongIdsUseCase
import com.example.musicapp.domain.usecase.download.GetLocalSongPathUseCase
import com.example.musicapp.domain.usecase.music.GetSongLyricsUseCase
import com.example.musicapp.domain.usecase.music.GetSongUrlUseCase
import com.example.musicapp.domain.usecase.music.LikeSongUseCase
import com.example.musicapp.domain.usecase.auth.ObserveLoginStateUseCase
import com.example.musicapp.domain.usecase.history.RecordRecentPlayUseCase
import com.example.musicapp.domain.usecase.stats.RecordWeekPlayUseCase
import com.example.musicapp.service.MusicPlaybackService
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
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

// 播放模式：随机 / 列表循环 / 单曲循环
enum class PlayerPlayMode {
    Shuffle,
    Loop,
    Single
}

// 全局播放状态，供 UI（迷你栏、全屏播放器等）订阅
// currentSong：已真正进入播放链路；previewSong：未开播时仅展示封面/歌名
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
    // 当前歌曲在队列中的下标，供上下首切歌使用
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

// 全局音乐播放控制器（Hilt 单例）
// 拉 URL / 队列 / 歌词 / PlaybackState / 听歌统计与收藏；
// 持有进程内唯一 ExoPlayer，由 MusicPlaybackService 挂到 MediaSession
@Singleton
class MusicPlayerController @Inject constructor(
    // Application Context：创建 ExoPlayer、启动 MusicPlaybackService
    @ApplicationContext private val context: Context,
    // 按歌曲 id 拉取可播流媒体 URL
    private val getSongUrlUseCase: GetSongUrlUseCase,
    // 已下载时返回本地文件路径
    private val getLocalSongPathUseCase: GetLocalSongPathUseCase,
    // 拉取并解析 LRC 歌词
    private val getSongLyricsUseCase: GetSongLyricsUseCase,
    // 写入最近播放记录
    private val recordRecentPlayUseCase: RecordRecentPlayUseCase,
    // 累加本周播放次数
    private val recordWeekPlayUseCase: RecordWeekPlayUseCase,
    // 累加听歌时长（本地统计）
    private val addListenDurationUseCase: AddListenDurationUseCase,
    // 红心收藏 / 取消收藏
    private val likeSongUseCase: LikeSongUseCase,
    // 拉取当前用户红心歌单 id 列表
    private val getLikedSongIdsUseCase: GetLikedSongIdsUseCase,
    // 观察登录态，用于预热收藏与鉴权提示
    private val observeLoginStateUseCase: ObserveLoginStateUseCase
) {
    // 底层播放器（懒加载）；音频焦点与「拔出耳机暂停」交给 Media3
    // 播放/暂停状态通过 Listener 回写 playbackState
    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { player ->
                player.addListener(
                    object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _playbackState.update { it.copy(isPlaying = isPlaying) }
                            if (isPlaying) {
                                markListeningStarted()
                                startPositionTracking()
                            } else {
                                // 暂停/停止时结算听歌时长，并停掉进度轮询
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _playbackState = MutableStateFlow(PlaybackState())
    // UI 订阅的全局播放状态
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // 播放中每 200ms 同步进度与歌词
    private var positionJob: Job? = null
    // 异步拉取歌词；切歌时取消旧任务
    private var lyricJob: Job? = null
    // 异步拉取播放 URL；切歌时取消，避免旧请求回写新状态
    private var urlJob: Job? = null
    // 本地缓存的红心歌单 id，用于即时判断收藏态
    private var likedSongIds: Set<Long> = emptySet()
    private var currentUserId: Long? = null
    // 本次连续播放段起点（elapsedRealtime）；null 表示当前未在计时
    private var listeningSinceElapsedRealtime: Long? = null

    init {
        // 启动时根据登录态预热红心列表，避免首屏收藏图标闪错
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

    // 设置迷你栏预览曲；不触发真正播放。队列为空时用该曲填成单曲队列
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

    // 播放指定歌曲：结算上一曲时长 → 更新状态并拉歌词 → 启动服务 → 异步取 URL 后 prepare/play
    // 切歌会取消进行中的 URL/歌词任务，并用 songId 校验防止过期回调污染状态
    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        settleListenDuration()
        val resolvedQueue = queue.ifEmpty { listOf(song) }
        val queueIndex = resolvedQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        positionJob?.cancel()
        lyricJob?.cancel()
        urlJob?.cancel()

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

        ensurePlaybackService()
        loadLyrics(song.id)
        syncFavoriteForCurrentSong()

        val requestSongId = song.id
        urlJob = scope.launch {
            try {
                // 已下载优先播本地文件，否则拉在线流地址
                val localPath = getLocalSongPathUseCase(requestSongId)
                val playUri = if (localPath != null) {
                    Uri.fromFile(File(localPath)).toString()
                } else {
                    val songUrl = getSongUrlUseCase(requestSongId)
                    songUrl.url
                }
                // 用户已切到别的歌：丢弃本次结果
                if (_playbackState.value.currentSong?.id != requestSongId) return@launch

                if (playUri.isNullOrBlank()) {
                    _playbackState.update {
                        it.copy(
                            isLoading = false,
                            error = "该歌曲暂无播放权限"
                        )
                    }
                } else {
                    _playbackState.update {
                        it.copy(isLoading = false, playUrl = playUri, error = null)
                    }
                    recordPlayStats(song)
                    player.setMediaItem(buildMediaItem(song, playUri))
                    player.prepare()
                    player.playWhenReady = true
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

    // 播放/暂停：无 currentSong 时用 preview 走 playSong；
    // 已有曲目时先 ensurePlaybackService（暂停期间服务可能被系统回收）再 play/pause
    fun togglePlayPause() {
        val state = _playbackState.value
        if (state.currentSong == null) {
            state.previewSong?.let { preview ->
                playSong(preview, state.queue.ifEmpty { listOf(preview) })
            }
            return
        }
        ensurePlaybackService()
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    // 单曲播完：单曲循环重播，其余模式切下一首
    private fun onPlaybackEnded() {
        when (_playbackState.value.playMode) {
            PlayerPlayMode.Single -> {
                player.seekTo(0)
                player.play()
            }
            PlayerPlayMode.Loop, PlayerPlayMode.Shuffle -> skipToNext()
        }
    }

    // 按当前播放模式切到下一首
    fun skipToNext() {
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return
        val nextIndex = resolveNextIndex(state)
        playSong(queue[nextIndex], queue)
    }

    // 上一首：Shuffle 随机；Loop 环形回退；Single 在队首则回到开头，否则播上一曲
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
                if (state.queueIndex <= 0) {
                    player.seekTo(0)
                    syncPositionAndLyric()
                } else {
                    playSong(queue[state.queueIndex - 1], queue)
                }
            }
        }
    }

    // 循环切换播放模式：随机 → 列表循环 → 单曲循环 → 随机
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

    // 解析「下一首」下标；单曲循环在手动/自动下一首时仍按列表前进
    private fun resolveNextIndex(state: PlaybackState): Int {
        val queue = state.queue
        return when (state.playMode) {
            PlayerPlayMode.Shuffle -> resolveRandomIndex(queue, state.queueIndex)
            PlayerPlayMode.Loop, PlayerPlayMode.Single ->
                (state.queueIndex + 1) % queue.size
        }
    }

    // 随机选一首，尽量避开当前下标（队列长度 > 1）
    private fun resolveRandomIndex(queue: List<Song>, currentIndex: Int): Int {
        if (queue.size <= 1) return 0
        var next = Random.nextInt(queue.size)
        while (next == currentIndex) {
            next = Random.nextInt(queue.size)
        }
        return next
    }

    // 跳转到指定进度，并立即刷新进度/歌词展示
    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        syncPositionAndLyric()
    }

    // 从当前队列按索引播放；越界则忽略
    fun playQueueItemAt(index: Int) {
        val state = _playbackState.value
        if (index !in state.queue.indices) return
        playSong(state.queue[index], state.queue)
    }

    // 切换收藏（乐观更新）；未登录报错，请求失败则 revertFavoriteState 回滚
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

    // 从服务端刷新红心 id 集合，并同步当前曲收藏态
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

    // 用 likedSongIds 同步当前/预览曲的 isFavorite
    private fun syncFavoriteForCurrentSong() {
        val songId = _playbackState.value.currentSong?.id
            ?: _playbackState.value.previewSong?.id
            ?: return
        val isFavorite = songId in likedSongIds
        _playbackState.update { state ->
            if (state.isFavorite == isFavorite) state else state.copy(isFavorite = isFavorite)
        }
    }

    // 收藏请求失败时回滚本地缓存与 UI 状态
    private fun revertFavoriteState(songId: Long, attemptedFavorite: Boolean, message: String) {
        likedSongIds = if (attemptedFavorite) likedSongIds - songId else likedSongIds + songId
        _playbackState.update {
            it.copy(isFavorite = !attemptedFavorite, error = message)
        }
    }

    // 记录最近播放与周播放次数（失败静默忽略）
    private fun recordPlayStats(song: Song) {
        scope.launch {
            runCatching { recordRecentPlayUseCase(song) }
            runCatching { recordWeekPlayUseCase() }
        }
    }

    // 开始一段听歌计时；已在计时则不重置，避免短暂抖动重复开段
    private fun markListeningStarted() {
        if (listeningSinceElapsedRealtime == null) {
            listeningSinceElapsedRealtime = SystemClock.elapsedRealtime()
        }
    }

    // 结束当前听歌段并累加时长；切歌/暂停时调用
    private fun settleListenDuration() {
        val since = listeningSinceElapsedRealtime ?: return
        listeningSinceElapsedRealtime = null
        val elapsedMs = SystemClock.elapsedRealtime() - since
        if (elapsedMs <= 0L) return
        scope.launch {
            runCatching { addListenDurationUseCase(elapsedMs) }
        }
    }

    // 异步加载歌词；仅当仍是同一首歌时写入状态，并按需启动进度跟踪
    private fun loadLyrics(songId: Long) {
        lyricJob?.cancel()
        lyricJob = scope.launch {
            runCatching {
                getSongLyricsUseCase(songId)
            }.onSuccess { lyrics ->
                _playbackState.update { state ->
                    if (state.currentSong?.id != songId) return@update state
                    state.copy(lyrics = lyrics)
                }
                if (_playbackState.value.currentSong?.id == songId) {
                    syncPositionAndLyric()
                }
                if (player.isPlaying) {
                    startPositionTracking()
                }
            }
        }
    }

    // 播放中轮询进度与歌词（约 200ms）；暂停时由 Listener 取消
    private fun startPositionTracking() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive && player.isPlaying) {
                syncPositionAndLyric()
                delay(200)
            }
        }
    }

    // 从 Player 读取进度/时长，解析当前歌词句并更新状态（无变化则跳过）
    private fun syncPositionAndLyric() {
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val durationMs = player.duration.coerceAtLeast(0L)
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

    // 有匹配歌词用原句；否则退回歌名；都没有则用默认文案（展示时统一加引号）
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

    // 将 Song + 可播 URL 转为 Media3 MediaItem；Metadata 供通知栏/锁屏，mediaId 用歌曲 id
    private fun buildMediaItem(song: Song, url: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.name)
            .setArtist(song.artists)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.coverUrl?.takeIf { it.isNotBlank() }?.let(Uri::parse))
            .build()
        return MediaItem.Builder()
            .setUri(Uri.parse(url))
            .setMediaId(song.id.toString())
            .setMediaMetadata(metadata)
            .build()
    }

    // 启动 MediaSession 服务；由 Media3 在真正播放时升为前台
    // 用 startService 而非 startForegroundService，避免拉 URL 期间前台超时
    // 暂停后服务可能被系统回收，故 togglePlayPause 恢复播放前也会再调一次
    private fun ensurePlaybackService() {
        val intent = Intent(context, MusicPlaybackService::class.java)
        context.startService(intent)
    }
}
