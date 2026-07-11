package com.example.musicapp.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.usecase.GetSongUrlUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// 全局播放状态，供 MiniPlayerBar、PlayerScreen 等 UI 订阅
data class PlaybackState(
    // 当前正在播放的歌曲；为 null 表示尚未真正开始播
    val currentSong: Song? = null,
    // 迷你栏预览歌曲；未播放时用于展示封面和歌名
    val previewSong: Song? = defaultPreviewSong,
    // 是否正在播放，由 ExoPlayer 回调同步
    val isPlaying: Boolean = false,
    // 是否正在拉取播放地址
    val isLoading: Boolean = false,
    // 当前歌曲是否已收藏（仅本地状态切换）
    val isFavorite: Boolean = false,
    // 最近一次成功获取到的流媒体地址
    val playUrl: String? = null,
    // 播放或拉取地址时的错误信息
    val error: String? = null,
    // 当前播放队列
    val queue: List<Song> = emptyList(),
    // 当前歌曲在队列中的下标，供 skipToNext 使用
    val queueIndex: Int = 0
) {
    // 迷你栏展示用：优先显示正在播的歌，否则显示预览歌
    val displaySong: Song?
        get() = currentSong ?: previewSong
}

// App 启动后迷你栏的默认占位歌曲
private val defaultPreviewSong = Song(
    id = 0L,
    name = "Neon Drift",
    artists = "Kaito",
    album = "",
    coverUrl = null,
    durationMs = 222_000L
)

// 全局音乐播放控制器
// Hilt 单例，整个 App 共享同一个 ExoPlayer 实例
// 负责拉取播放地址、驱动 ExoPlayer、维护 PlaybackState
// 不依赖 ViewModel / NavGraph，由各处通过 Hilt 注入或 EntryPoint 获取
@Singleton
class MusicPlayerController @Inject constructor(
    @ApplicationContext context: Context,
    private val getSongUrlUseCase: GetSongUrlUseCase
) {
    // 底层播放器，真正负责解码和出声
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    // 控制器自己的协程作用域
    // SupervisorJob：某个播放任务异常不会拖垮整个 scope
    // Main.immediate：状态更新和 ExoPlayer 调用保持在主线程
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // 内部可变状态
    private val _playbackState = MutableStateFlow(PlaybackState())
    // 对外只读，UI 层 collect 这个 Flow
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    init {
        // 监听 ExoPlayer 播放状态变化，同步到 PlaybackState.isPlaying
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackState.update { it.copy(isPlaying = isPlaying) }
                }
            }
        )
    }

    // 设置迷你栏预览歌曲
    // 典型场景：首页加载出精选歌、搜索页点选歌曲但尚未播放
    // 若当前还没有队列，则把这首歌作为初始队列
    fun setPreviewSong(song: Song) {
        _playbackState.update { state ->
            state.copy(
                previewSong = song,
                queue = state.queue.ifEmpty { listOf(song) }
            )
        }
    }

    // 播放指定歌曲
    // song：要播放的歌曲
    // queue：播放队列；为空时退化为只播当前这一首
    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        // 没传队列时，用当前歌曲单首组成队列
        val resolvedQueue = queue.ifEmpty { listOf(song) }
        // 找到当前歌曲在队列中的位置，供下一首切换使用
        val queueIndex = resolvedQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        // 先同步 UI 状态：切歌、进入 loading、清空旧错误和旧地址
        _playbackState.update {
            it.copy(
                currentSong = song,
                queue = resolvedQueue,
                queueIndex = queueIndex,
                isLoading = true,
                error = null,
                playUrl = null
            )
        }

        // 异步向服务端请求真实可播放地址
        scope.launch {
            runCatching {
                getSongUrlUseCase(song.id)
            }.onSuccess { songUrl ->
                val url = songUrl.url
                if (url.isNullOrBlank()) {
                    // 接口成功但没有可用地址（如无版权）
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
                    // 交给 ExoPlayer 加载并开始播放
                    exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }
            }.onFailure { throwable ->
                // 网络或接口异常
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

    // 播放队列中的下一首；已是最后一首时不做任何事
    fun skipToNext() {
        val state = _playbackState.value
        val nextIndex = state.queueIndex + 1
        if (nextIndex < state.queue.size) {
            playSong(state.queue[nextIndex], state.queue)
        }
    }

    // 切换当前歌曲收藏状态（当前仅本地切换，未接后端）
    fun toggleFavorite() {
        _playbackState.update { it.copy(isFavorite = !it.isFavorite) }
    }
}
