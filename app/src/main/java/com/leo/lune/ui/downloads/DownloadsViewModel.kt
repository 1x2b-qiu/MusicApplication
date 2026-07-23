package com.leo.lune.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.controller.ActiveDownloadTask
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.controller.SongDownloadManager
import com.leo.lune.domain.model.DownloadedSong
import com.leo.lune.domain.usecase.download.DeleteDownloadUseCase
import com.leo.lune.domain.usecase.download.ObserveDownloadedSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// 本地下载列表页 UI 状态
data class DownloadsUiState(
    val activeTasks: List<ActiveDownloadTask> = emptyList(),
    val downloadedSongs: List<DownloadedSong> = emptyList(),
    // 已下载合计体积（字节）
    val totalSizeBytes: Long = 0L
)

// 本地下载列表：观察进行中任务与已下载记录；支持播放、暂停/继续、取消、删除
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    observeDownloadedSongsUseCase: ObserveDownloadedSongsUseCase,
    private val deleteDownloadUseCase: DeleteDownloadUseCase,
    private val downloadManager: SongDownloadManager,
    private val playerController: MusicPlayerController
) : ViewModel() {

    val uiState: StateFlow<DownloadsUiState> = combine(
        downloadManager.tasks,
        observeDownloadedSongsUseCase()
    ) { tasks, songs ->
        DownloadsUiState(
            activeTasks = tasks.filter { it.error == null },
            downloadedSongs = songs,
            totalSizeBytes = songs.sumOf { it.fileSizeBytes }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DownloadsUiState()
    )

    // 以全部已下载为队列播放指定项
    fun playSong(song: DownloadedSong) {
        val queue = uiState.value.downloadedSongs.map { it.toSong() }
        playerController.playSong(song.toSong(), queue)
    }

    fun cancelDownload(songId: Long) {
        downloadManager.cancel(songId)
    }

    fun togglePauseDownload(songId: Long) {
        downloadManager.togglePause(songId)
    }

    fun deleteDownload(songId: Long) {
        viewModelScope.launch {
            deleteDownloadUseCase(songId)
        }
    }
}
