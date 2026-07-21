package com.example.musicapp.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.controller.MusicPlayerController
import com.example.musicapp.controller.PlaybackPosition
import com.example.musicapp.controller.PlayerPlayMode
import com.example.musicapp.domain.model.DownloadQuality
import com.example.musicapp.domain.model.LyricLine
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.usecase.download.DownloadSongUseCase
import com.example.musicapp.domain.usecase.download.ObserveSongDownloadedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 全屏播放页 UI 状态（播放数据来自全局 MusicPlayerController，下载态由本页维护）
data class PlayerUiState(
    val songId: Long = 0L,
    val songName: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val coverUrl: String? = null,
    // 是否正在解析播放地址
    val isLoading: Boolean = false,
    val playUrl: String? = null,
    val error: String? = null,
    val isPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    // 播放队列与当前下标
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = 0,
    val lyrics: List<LyricLine> = emptyList(),
    // 当前高亮歌词行下标（低频，仅行切换时更新）
    val activeLyricIndex: Int = 0,
    val playMode: PlayerPlayMode = PlayerPlayMode.Shuffle,
    // 歌曲总时长（毫秒），来自 Song 元数据；播放器实际时长见 positionState
    val durationMs: Long = 0L,
    // 本地下载：是否已落盘 / 是否进行中 / 字节进度 / 失败文案
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadError: String? = null
)

// 本页下载过程的瞬时状态；用 targetSongId 避免切歌后旧任务污染新曲 UI
private data class DownloadUi(
    val targetSongId: Long = 0L,
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null
)

// 全屏播放页 ViewModel
// 播控只委托 MusicPlayerController；下载由本页 UseCase 驱动并合并进 uiState
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: MusicPlayerController,
    private val downloadSongUseCase: DownloadSongUseCase,
    private val observeSongDownloadedUseCase: ObserveSongDownloadedUseCase
) : ViewModel() {

    // 下载进行中 / 失败信息（不进 Controller，仅本页使用）
    private val downloadUi = MutableStateFlow(DownloadUi())

    // 高频播放进度（约 200ms）；不进 uiState，由进度条等局部控件就近订阅
    val positionState: StateFlow<PlaybackPosition> = playerController.playbackPosition

    // 合并三路：全局播放态 + 当前曲是否已下载 + 本页下载瞬时态
    val uiState: StateFlow<PlayerUiState> = combine(
        playerController.playbackState,
        // 切歌后切换观察目标，避免一直订阅上一首的下载标记
        playerController.playbackState
            .map { it.displaySong?.id ?: 0L }
            .distinctUntilChanged()
            .flatMapLatest { songId ->
                if (songId == 0L) flowOf(false)
                else observeSongDownloadedUseCase(songId)
            },
        downloadUi
    ) { state, isDownloaded, download ->
        val song = state.displaySong
        val downloadingCurrent = download.isDownloading && download.targetSongId == song?.id
        PlayerUiState(
            songId = song?.id ?: 0L,
            songName = song?.name.orEmpty(),
            artistName = song?.artists.orEmpty(),
            albumName = song?.album.orEmpty(),
            coverUrl = song?.coverUrl,
            isLoading = state.isLoading,
            playUrl = state.playUrl,
            error = state.error,
            isPlaying = state.isPlaying,
            isFavorite = state.isFavorite,
            queue = state.queue,
            queueIndex = state.queueIndex,
            lyrics = state.lyrics,
            activeLyricIndex = state.activeLyricIndex,
            playMode = state.playMode,
            durationMs = song?.durationMs ?: 0L,
            isDownloaded = isDownloaded,
            // 仅当下载任务针对当前展示曲时才展示进行中 / 错误 / 进度
            isDownloading = downloadingCurrent,
            downloadProgress = if (downloadingCurrent) download.progress else 0f,
            downloadError = download.error?.takeIf { download.targetSongId == song?.id }
        )
    }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlayerUiState()
        )

    fun togglePlayPause() = playerController.togglePlayPause()

    fun toggleFavorite() = playerController.toggleFavorite()

    fun skipToNext() = playerController.skipToNext()

    fun skipToPrevious() = playerController.skipToPrevious()

    // 进度条拖动结束后定位
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    // 点击歌词跳转到对应时间
    fun seekToLyric(index: Int) {
        val lyric = uiState.value.lyrics.getOrNull(index) ?: return
        playerController.seekTo(lyric.timeMs)
    }

    // 播放队列中指定项
    fun playQueueItemAt(index: Int) = playerController.playQueueItemAt(index)

    // 从播放队列移除指定下标
    fun removeFromQueue(index: Int) = playerController.removeFromQueue(index)

    // 清空播放队列
    fun clearQueue() = playerController.clearQueue()

    // 切换播放模式，逻辑在 Controller
    fun cyclePlayMode() = playerController.cyclePlayMode()

    // 下载当前展示曲到应用私有目录；已下载或下载中则忽略重复点击
    fun downloadCurrentSong(quality: DownloadQuality = DownloadQuality.Default) {
        val song = playerController.playbackState.value.displaySong ?: return
        if (uiState.value.isDownloaded || uiState.value.isDownloading) return

        downloadUi.update {
            DownloadUi(targetSongId = song.id, isDownloading = true, progress = 0f, error = null)
        }
        viewModelScope.launch {
            runCatching {
                downloadSongUseCase(song, quality) { bytesRead, totalBytes ->
                    if (totalBytes <= 0L) return@downloadSongUseCase
                    val fraction = (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f)
                    downloadUi.update { current ->
                        if (current.targetSongId != song.id || !current.isDownloading) current
                        else current.copy(progress = fraction)
                    }
                }
            }.onSuccess {
                downloadUi.update {
                    DownloadUi(targetSongId = song.id, isDownloading = false, progress = 1f, error = null)
                }
            }.onFailure { error ->
                downloadUi.update {
                    DownloadUi(
                        targetSongId = song.id,
                        isDownloading = false,
                        progress = 0f,
                        error = error.message ?: "下载失败"
                    )
                }
            }
        }
    }
}
