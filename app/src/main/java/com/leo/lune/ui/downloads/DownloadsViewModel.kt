package com.leo.lune.ui.downloads

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.manager.ActiveDownloadTask
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.manager.SongDownloadManager
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.DownloadedSong
import com.leo.lune.domain.repository.DownloadRepository
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
// 同曲多音质各占一行
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val downloadManager: SongDownloadManager,
    private val playerController: MusicPlayerController
) : ViewModel() {

    val uiState: StateFlow<DownloadsUiState> = combine(
        downloadManager.tasks,
        downloadRepository.observeDownloadedSongs()
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

    // 播放指定已下载行：用该行音质；队列按 songId 去重，切到其它曲仍走最高音质
    @RequiresApi(Build.VERSION_CODES.O)
    fun playSong(song: DownloadedSong) {
        val queue = uiState.value.downloadedSongs
            .distinctBy { it.songId }
            .map { it.toSong() }
        playerController.playSong(
            song = song.toSong(),
            queue = queue,
            localQuality = DownloadQuality.fromBitrate(song.bitrate)
        )
    }

    fun cancelDownload(songId: Long, quality: DownloadQuality) {
        downloadManager.cancel(songId, quality)
    }

    fun togglePauseDownload(songId: Long, quality: DownloadQuality) {
        downloadManager.togglePause(songId, quality)
    }

    fun deleteDownload(song: DownloadedSong) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(
                song.songId,
                DownloadQuality.fromBitrate(song.bitrate)
            )
        }
    }
}
