package com.example.musicapp.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.controller.MusicPlayerController
import com.example.musicapp.domain.model.DownloadedSong
import com.example.musicapp.domain.usecase.download.ObserveDownloadedSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// 本地下载列表页 UI 状态
data class DownloadsUiState(
    val downloadedSongs: List<DownloadedSong> = emptyList()
)

// 本地下载列表：观察已下载记录，点击后走全局播放器（优先本地文件）
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    observeDownloadedSongsUseCase: ObserveDownloadedSongsUseCase,
    private val playerController: MusicPlayerController
) : ViewModel() {

    val uiState: StateFlow<DownloadsUiState> = observeDownloadedSongsUseCase()
        .map { DownloadsUiState(downloadedSongs = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DownloadsUiState()
        )

    // 以全部已下载为队列播放指定项
    fun playSong(song: DownloadedSong) {
        val queue = uiState.value.downloadedSongs.map { it.toSong() }
        playerController.playSong(song.toSong(), queue)
    }
}
