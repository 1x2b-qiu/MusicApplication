package com.leo.lune.controller

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.LyricLine
import com.leo.lune.domain.model.PlaybackSource
import com.leo.lune.domain.model.SettingKeys
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.SettingsRepository
import com.leo.lune.domain.usecase.playback.PreparePlaybackUseCase
import com.leo.lune.manager.FavoriteManager
import com.leo.lune.manager.LyricManager
import com.leo.lune.manager.PlayStatsRecorderManager
import com.leo.lune.manager.PlaybackSnapshotManager
import com.leo.lune.manager.QueueManager
import com.leo.lune.manager.SleepTimerManager
import com.leo.lune.service.MusicPlaybackService
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

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
    // 当前实际播放音质文案（标准 / 高品质 / 无损）；未开播为空
    val qualityLabel: String = "",
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
    // 当前高亮歌词行下标；仅在行切换时更新（低频）
    val activeLyricIndex: Int = 0,
    // 全局播放模式，驱动自动切歌与手动上下首
    val playMode: PlayerPlayMode = PlayerPlayMode.Loop
) {
    // 迷你栏展示用：优先显示正在播的歌，否则显示预览歌
    val displaySong: Song?
        get() = currentSong ?: previewSong
}

// 高频播放进度快照（约 200ms 更新）
// 独立于 PlaybackState：仅进度条/时间文本订阅，避免整棵播放页 UI 树跟随高频重组
data class PlaybackPosition(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

// 全局音乐播放控制器（Hilt 单例）
// 拉 URL / 队列 / 歌词 / PlaybackState / 听歌统计与收藏；
// 持有进程内唯一 ExoPlayer，由 MusicPlaybackService 挂到 MediaSession
@Singleton
class MusicPlayerController @Inject constructor(
    // Application Context：创建 ExoPlayer、启动 MusicPlaybackService
    @ApplicationContext private val context: Context,
    // 播放设置（默认音质 / 音频焦点策略）
    private val settingsRepository: SettingsRepository,
    // 歌词管理器（加载 / 匹配 / 展示文案）
    private val lyricManager: LyricManager,
    // 听歌统计记录器（最近播放 / 周次数 / 时长）
    private val playStatsRecorderManager: PlayStatsRecorderManager,
    // 队列导航器（播放模式 + 上/下一首下标解析）
    private val queueManager: QueueManager,
    // 红心收藏管理器（缓存 + 乐观更新 + 回滚）
    val favoriteManager: FavoriteManager,
    // 定时关闭（倒计时到期后暂停）
    private val sleepTimerManager: SleepTimerManager,
    // 播放快照持久化（进程级恢复）
    private val snapshotManager: PlaybackSnapshotManager,
    // 播放准备 UseCase：URL 解析 + 封面加载并行编排
    private val preparePlaybackUseCase: PreparePlaybackUseCase
) {
    // 是否由 ExoPlayer 处理音频焦点；与「同时播放」开关相反，创建前可读到缓存值
    @Volatile
    private var handleAudioFocus: Boolean = true

    // 标记 ExoPlayer 是否已懒加载创建，避免设置变更时过早触发创建
    @Volatile
    private var playerReady: Boolean = false

    // 底层播放器（懒加载）；音频焦点与「拔出耳机暂停」交给 Media3
    // 播放/暂停状态通过 Listener 回写 playbackState
    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setAudioAttributes(mediaAudioAttributes(), handleAudioFocus)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { player ->
                playerReady = true
                player.addListener(
                    object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _playbackState.update { it.copy(isPlaying = isPlaying) }
                            if (isPlaying) {
                                playStatsRecorderManager.markListeningStarted()
                                startPositionTracking()
                            } else {
                                // 暂停/停止时结算听歌时长，并停掉进度轮询
                                playStatsRecorderManager.settleListenDuration()
                                syncPositionAndLyric()
                                positionJob?.cancel()
                                saveSnapshot()
                            }
                        }

                        @RequiresApi(Build.VERSION_CODES.O)
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_READY -> syncPositionAndLyric()
                                Player.STATE_ENDED -> onPlaybackEnded()
                            }
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            // 自动播完或手动切到「已预取的下一首」：播放列表已无缝衔接，只需对齐业务状态
                            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                                reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
                            ) {
                                val songId = mediaItem?.mediaId?.toLongOrNull() ?: return
                                if (songId == prefetchedNextSongId) {
                                    onPrefetchedSongStarted(songId)
                                }
                            }
                        }
                    }
                )
            }
    }

    // 媒体用途的 AudioAttributes（USAGE_MEDIA + MUSIC）
    private fun mediaAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
    }

    // mixWithOthers=true → 不处理焦点（可与导航等同时出声）；false → 独占焦点
    private fun applyMixWithOthers(enabled: Boolean) {
        handleAudioFocus = !enabled
        if (!playerReady) return
        player.setAudioAttributes(mediaAudioAttributes(), handleAudioFocus)
    }

    private companion object {
        // 距歌曲结束不足该时长时，预取下一首 URL 并追加进 Media3 播放列表
        const val PREFETCH_WINDOW_MS = 15_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _playbackState = MutableStateFlow(PlaybackState())
    // UI 订阅的全局播放状态
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _playbackPosition = MutableStateFlow(PlaybackPosition())
    // UI 订阅的高频播放进度（约 200ms）；只应由进度条等局部控件就近订阅
    val playbackPosition: StateFlow<PlaybackPosition> = _playbackPosition.asStateFlow()

    // 播放中每 200ms 同步进度与歌词
    private var positionJob: Job? = null
    // 异步拉取播放 URL；切歌时取消，避免旧请求回写新状态
    private var urlJob: Job? = null
    // 异步预取下一首 URL；切歌/清空时取消
    private var prefetchJob: Job? = null
    // 已追加进 Media3 播放列表的下一首歌曲 id；用于预取去重与自动切歌对账
    private var prefetchedNextSongId: Long? = null
    // 预取下一首对应的音质文案，切到该曲时写入 PlaybackState
    private var prefetchedNextQualityLabel: String? = null

    init {
        // 订阅「与其他应用同时播放」，即时调整 ExoPlayer 音频焦点策略
        scope.launch {
            settingsRepository.observeValue(SettingKeys.PLAYBACK_MIX_WITH_OTHERS)
                .map { it == "true" }
                .distinctUntilChanged()
                .collect { mixWithOthers ->
                    applyMixWithOthers(mixWithOthers)
                }
        }
        // 订阅 QueueManager 的播放模式，同步进 PlaybackState
        scope.launch {
            queueManager.playMode.collect { mode ->
                _playbackState.update { state ->
                    if (state.playMode == mode) state else state.copy(playMode = mode)
                }
            }
        }
        // 订阅 FavoriteManager 的收藏态，同步进 PlaybackState
        scope.launch {
            favoriteManager.isFavorite.collect { fav ->
                _playbackState.update { state ->
                    if (state.isFavorite == fav) state else state.copy(isFavorite = fav)
                }
            }
        }
        // 订阅 LyricManager 的歌词列表，加载完成后同步进 PlaybackState 并刷新进度
        scope.launch {
            lyricManager.lyrics.collect { lyrics ->
                _playbackState.update { state ->
                    if (state.lyrics === lyrics) state else state.copy(lyrics = lyrics)
                }
                if (lyrics.isNotEmpty()) {
                    syncPositionAndLyric()
                    if (player.isPlaying) startPositionTracking()
                }
            }
        }
        // 恢复上次播放快照：队列/下标/当前曲设为 preview，不自动播放
        scope.launch {
            val snapshot = snapshotManager.restore() ?: return@launch
            _playbackState.update { state ->
                state.copy(
                    previewSong = snapshot.currentSong,
                    queue = snapshot.queue,
                    queueIndex = snapshot.queueIndex,
                    currentLyricLine = lyricManager.fallbackLyric(snapshot.currentSong)
                )
            }
            // 与 setPreviewSong / playSong 一致：登记当前曲，供红心列表到达后补同步
            favoriteManager.syncForSong(snapshot.currentSong.id)
        }
    }

    // 设置迷你栏预览曲；不触发真正播放。队列为空时用该曲填成单曲队列
    fun setPreviewSong(song: Song) {
        _playbackState.update { state ->
            state.copy(
                previewSong = song,
                queue = state.queue.ifEmpty { listOf(song) },
                currentLyricLine = lyricManager.fallbackLyric(song)
            )
        }
        favoriteManager.syncForSong(song.id)
    }

    // 播放指定歌曲：结算上一曲时长 → 更新状态并拉歌词 → 启动服务 → 异步取 URL 后 prepare/play
    // 切歌会取消进行中的 URL/歌词任务，并用 songId 校验防止过期回调污染状态
    // localQuality：本地下载列表点选某档时传入，仅本次播放用该档；其它入口不传则优先最高音质
    @RequiresApi(Build.VERSION_CODES.O)
    fun playSong(
        song: Song,
        queue: List<Song> = emptyList(),
        localQuality: DownloadQuality? = null
    ) {
        playStatsRecorderManager.settleListenDuration()
        val resolvedQueue = queue.ifEmpty { listOf(song) }
        val queueIndex = resolvedQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        positionJob?.cancel()
        lyricManager.cancelLoading()
        urlJob?.cancel()
        prefetchJob?.cancel()
        prefetchedNextSongId = null
        prefetchedNextQualityLabel = null

        _playbackState.update {
            it.copy(
                currentSong = song,
                queue = resolvedQueue,
                queueIndex = queueIndex,
                isLoading = true,
                isPlaying = false,
                error = null,
                playUrl = null,
                qualityLabel = "",
                lyrics = emptyList(),
                currentLyricLine = lyricManager.fallbackLyric(song),
                activeLyricIndex = 0
            )
        }
        _playbackPosition.value = PlaybackPosition()

        ensurePlaybackService()
        lyricManager.loadLyrics(song.id)
        favoriteManager.syncForSong(song.id)

        val requestSongId = song.id
        urlJob = scope.launch {
            try {
                // UseCase 并行执行 URL 解析 + 封面加载（本地文件优先，封面失败降级）
                val preparation = preparePlaybackUseCase(song, localQuality)

                // 用户已切到别的歌：丢弃本次结果
                if (_playbackState.value.currentSong?.id != requestSongId) return@launch

                _playbackState.update {
                    it.copy(
                        isLoading = false,
                        playUrl = preparation.source.rawUri,
                        qualityLabel = preparation.source.qualityLabel,
                        error = null
                    )
                }
                playStatsRecorderManager.recordPlayStats(song)
                saveSnapshot()

                // URI 转换是 Android 特有操作（Uri.fromFile / content://），保留在 Controller
                val playUri = toPlayUri(preparation.source)
                player.setMediaItem(buildMediaItem(song, playUri, preparation.artworkBytes))
                player.prepare()
                player.playWhenReady = true

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

    // 播放/暂停：暂停直接 pause；续播仅在播放器仍可直接 play 时走 play()，
    // 否则（仅有 preview / 无 MediaItem / IDLE / ENDED / 出错）走 playSong 重新拉地址并 prepare。
    // 后台久置后服务回收或流地址失效时，裸 play() 会无响应，必须回退到 playSong。
    @RequiresApi(Build.VERSION_CODES.O)
    fun togglePlayPause() {
        val state = _playbackState.value
        val song = state.currentSong ?: state.previewSong ?: return
        if (player.isPlaying) {
            pause()
            return
        }
        val canResume = state.currentSong != null &&
            player.mediaItemCount > 0 &&
            player.playbackState != Player.STATE_IDLE &&
            player.playbackState != Player.STATE_ENDED &&
            player.playerError == null
        if (!canResume) {
            playSong(song, state.queue.ifEmpty { listOf(song) })
            return
        }
        ensurePlaybackService()
        player.play()
    }

    // 暂停播放（播放器未创建时为空操作）
    fun pause() {
        if (!playerReady) return
        player.pause()
    }

    // 定时关闭是否进行中（供播放页闹钟按钮高亮）
    val sleepTimerActive: StateFlow<Boolean> = sleepTimerManager.isActive

    // 定时关闭到期时刻；未启用为 null（供倒计时文案）
    val sleepTimerEndsAtEpochMs: StateFlow<Long?> = sleepTimerManager.endsAtEpochMs

    // 启动或覆盖定时关闭；到期后 pause()
    fun startSleepTimer(durationMs: Long) {
        sleepTimerManager.start(durationMs) { pause() }
    }

    // 取消定时关闭
    fun cancelSleepTimer() = sleepTimerManager.cancel()

    // 播放列表播完（下一首未预取或预取失败时）：单曲循环走 playSong 刷新地址后重播，其余模式切下一首
    @RequiresApi(Build.VERSION_CODES.O)
    private fun onPlaybackEnded() {
        when (queueManager.playMode.value) {
            PlayerPlayMode.Single -> {
                val state = _playbackState.value
                val song = state.currentSong ?: return
                playSong(song, state.queue.ifEmpty { listOf(song) })
            }
            PlayerPlayMode.Loop, PlayerPlayMode.Shuffle -> skipToNext()
        }
    }

    // 按当前播放模式切到下一首；已预取时直接切播放列表中的下一项，无缝衔接
    @RequiresApi(Build.VERSION_CODES.O)
    fun skipToNext() {
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return
        val prefetchedReady = prefetchedNextSongId != null &&
            player.currentMediaItemIndex + 1 < player.mediaItemCount &&
            queue.any { it.id == prefetchedNextSongId }
        if (prefetchedReady) {
            player.seekToNextMediaItem()
            return
        }
        playSong(queue[queueManager.resolveNextIndex(queue, state.queueIndex)], queue)
    }

    // 上一首：Shuffle 随机；Loop 环形回退；Single 在队首则回到开头，否则播上一曲
    @RequiresApi(Build.VERSION_CODES.O)
    fun skipToPrevious() {
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return
        // Single 模式在队首时仅 seekTo(0)，不切歌
        if (queueManager.playMode.value == PlayerPlayMode.Single && state.queueIndex <= 0) {
            player.seekTo(0)
            syncPositionAndLyric()
            return
        }
        val prevIndex = queueManager.resolvePreviousIndex(queue, state.queueIndex)
        playSong(queue[prevIndex], queue)
    }

    // 循环切换播放模式：委托给 QueueManager
    fun cyclePlayMode() = queueManager.cyclePlayMode()

    // 跳转到指定进度，并立即刷新进度/歌词展示
    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        syncPositionAndLyric()
    }

    // 从当前队列按索引播放；越界则忽略
    @RequiresApi(Build.VERSION_CODES.O)
    fun playQueueItemAt(index: Int) {
        val state = _playbackState.value
        if (index !in state.queue.indices) return
        playSong(state.queue[index], state.queue)
    }

    // 从播放队列移除指定下标；删当前曲则续播相邻项，清空则停播
    @RequiresApi(Build.VERSION_CODES.O)
    fun removeFromQueue(index: Int) {
        val state = _playbackState.value
        if (index !in state.queue.indices) return
        val removedSong = state.queue[index]
        val newQueue = state.queue.toMutableList().also { it.removeAt(index) }
        if (newQueue.isEmpty()) {
            clearQueue()
            return
        }
        // 删掉的若是已预取的下一首，同步移除播放列表中的预取项
        if (removedSong.id == prefetchedNextSongId) {
            removePrefetchedMediaItem()
        }
        when {
            index < state.queueIndex -> {
                _playbackState.update {
                    it.copy(queue = newQueue, queueIndex = state.queueIndex - 1)
                }
                saveSnapshot()
            }
            index > state.queueIndex -> {
                _playbackState.update { it.copy(queue = newQueue) }
                saveSnapshot()
            }
            else -> {
                val nextIndex = index.coerceAtMost(newQueue.lastIndex)
                playSong(newQueue[nextIndex], newQueue)
            }
        }
    }

    // 清空播放队列并停止当前播放，保留播放模式；一并取消定时关闭
    fun clearQueue() {
        playStatsRecorderManager.settleListenDuration()
        positionJob?.cancel()
        lyricManager.cancelLoading()
        urlJob?.cancel()
        prefetchJob?.cancel()
        prefetchedNextSongId = null
        prefetchedNextQualityLabel = null
        cancelSleepTimer()
        player.stop()
        player.clearMediaItems()
        _playbackPosition.value = PlaybackPosition()
        _playbackState.update { state ->
            PlaybackState(playMode = state.playMode)
        }
        snapshotManager.clear()
    }

    // 切换收藏：委托给 FavoriteManager（乐观更新 + 回滚）
    fun toggleFavorite() {
        val songId = _playbackState.value.currentSong?.id
            ?: _playbackState.value.previewSong?.id
            ?: return
        favoriteManager.toggleFavorite(songId)
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

    // 从 Player 读取进度/时长并写入高频 position flow；
    // 歌词行/下标仅在变化时更新 PlaybackState，低频订阅者不被进度带着重组
    private fun syncPositionAndLyric() {
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val durationMs = player.duration.coerceAtLeast(0L)
        val position = _playbackPosition.value
        if (position.positionMs != positionMs || position.durationMs != durationMs) {
            _playbackPosition.value = PlaybackPosition(positionMs, durationMs)
        }

        val state = _playbackState.value
        val display = lyricManager.resolveCurrentLyric(
            positionMs = positionMs,
            song = state.currentSong ?: state.previewSong
        )
        if (state.activeLyricIndex != display.index || state.currentLyricLine != display.line) {
            _playbackState.update {
                it.copy(activeLyricIndex = display.index, currentLyricLine = display.line)
            }
        }
        maybePrefetchNext(positionMs, durationMs)
    }

    // 播到最后一小段时预取下一首 URL 并追加进 Media3 播放列表，播完由 ExoPlayer 自动无缝衔接；
    // 单曲循环/单首队列无需预取，预取失败则退回 onPlaybackEnded 的全量切歌路径
    private fun maybePrefetchNext(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L || durationMs - positionMs > PREFETCH_WINDOW_MS) return
        if (prefetchedNextSongId != null || prefetchJob != null) return
        val state = _playbackState.value
        if (queueManager.playMode.value == PlayerPlayMode.Single || state.queue.size <= 1) return
        prefetchNext(state)
    }

    // 按当前播放模式选定下一首并异步取 URL；仅在仍播放原曲且未预取过时追加进播放列表
    private fun prefetchNext(state: PlaybackState) {
        val originSongId = state.currentSong?.id ?: return
        val nextSong = state.queue[queueManager.resolveNextIndex(state.queue, state.queueIndex)]
        prefetchJob = scope.launch {
            // 预取失败静默忽略：播完仍走 onPlaybackEnded → skipToNext 全量拉取
            val preparation = runCatching { preparePlaybackUseCase(nextSong) }.getOrNull()
                ?: return@launch

            if (_playbackState.value.currentSong?.id != originSongId) return@launch
            if (prefetchedNextSongId != null) return@launch

            val playUri = toPlayUri(preparation.source)
            player.addMediaItem(buildMediaItem(nextSong, playUri, preparation.artworkBytes))
            prefetchedNextSongId = nextSong.id
            prefetchedNextQualityLabel = preparation.source.qualityLabel
            prefetchJob = null
        }
    }

    // 播放列表切到已预取的下一首：结算上一首并重开计时、对齐队列下标、清理已播项，续上歌词/收藏/统计
    private fun onPrefetchedSongStarted(songId: Long) {
        val state = _playbackState.value
        val song = state.queue.firstOrNull { it.id == songId } ?: return
        playStatsRecorderManager.settleListenDuration()
        // 自动衔接期间 isPlaying 不变，onIsPlayingChanged 不会重开计时，需手动开段
        playStatsRecorderManager.markListeningStarted()
        val qualityLabel = prefetchedNextQualityLabel.orEmpty()
        prefetchedNextSongId = null
        prefetchedNextQualityLabel = null
        // 移除已播完的旧项，播放列表始终保持「当前曲 + 预取的下一首」
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex > 0) {
            player.removeMediaItems(0, currentIndex)
        }
        _playbackState.update {
            it.copy(
                currentSong = song,
                queueIndex = state.queue.indexOfFirst { item -> item.id == songId }.coerceAtLeast(0),
                isLoading = false,
                error = null,
                playUrl = player.currentMediaItem?.localConfiguration?.uri?.toString(),
                qualityLabel = qualityLabel,
                lyrics = emptyList(),
                currentLyricLine = lyricManager.fallbackLyric(song),
                activeLyricIndex = 0
            )
        }
        _playbackPosition.value = PlaybackPosition()
        playStatsRecorderManager.recordPlayStats(song)
        lyricManager.loadLyrics(song.id)
        favoriteManager.syncForSong(song.id)
    }

    // 从 Media3 播放列表移除预取的下一首（若存在），并清除预取标记
    private fun removePrefetchedMediaItem() {
        val nextIndex = player.currentMediaItemIndex + 1
        if (nextIndex < player.mediaItemCount) {
            player.removeMediaItem(nextIndex)
        }
        prefetchedNextSongId = null
        prefetchedNextQualityLabel = null
    }

    // 将 Song + 可播 URL 转为 Media3 MediaItem；Metadata 供通知栏/锁屏，mediaId 用歌曲 id
    // 优先内嵌已下载的封面字节（artworkData），系统无需再远程拉取，通知栏封面稳定显示；
    // 封面加载失败时回退到 artworkUri，由系统自行下载（可能失败）
    private fun buildMediaItem(song: Song, url: String, artworkBytes: ByteArray? = null): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(song.name)
            .setArtist(song.artists)
            .setAlbumTitle(song.album)
        if (artworkBytes != null) {
            metadataBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        } else {
            metadataBuilder.setArtworkUri(song.coverUrl?.takeIf { it.isNotBlank() }?.let(Uri::parse))
        }
        return MediaItem.Builder()
            .setUri(Uri.parse(url))
            .setMediaId(song.id.toString())
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    // 启动 MediaSession 服务（前台模式）
    // 服务 onCreate 中立即发布「准备中」通知，满足 5 秒前台限制；
    // 真正开播后 Media3 替换为媒体播放通知
    // 暂停后服务可能被系统回收，故 togglePlayPause 恢复播放前也会再调一次
    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensurePlaybackService() {
        val intent = Intent(context, MusicPlaybackService::class.java)
        context.startForegroundService(intent)
    }

    // PlaybackSource → Android 可播放 URI 字符串
    // 本地文件：SAF content URI 原样使用；私有目录绝对路径转 file://
    // 远端流媒体：URL 直接使用
    private fun toPlayUri(source: PlaybackSource): String = when (source) {
        is PlaybackSource.LocalFile -> {
            val path = source.path
            if (path.startsWith("content:", ignoreCase = true)) path
            else Uri.fromFile(File(path)).toString()
        }
        is PlaybackSource.RemoteStream -> source.url
    }

    // 将当前播放状态快照持久化到 Room，供进程被杀后恢复
    private fun saveSnapshot() {
        val state = _playbackState.value
        snapshotManager.save(state.currentSong, state.queue, state.queueIndex)
    }
}
