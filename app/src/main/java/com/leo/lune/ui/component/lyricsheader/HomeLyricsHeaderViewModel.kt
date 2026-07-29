package com.leo.lune.ui.component.lyricsheader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// 顶栏 UI 状态：歌词、播放态、头像
data class HomeLyricsHeaderUi(
    val currentLyricLine: String = "听点音乐吧",
    val isPlaying: Boolean = false,
    val hasPlaybackContent: Boolean = false,
    val avatarUrl: String? = null
)

@HiltViewModel
class HomeLyricsHeaderViewModel @Inject constructor(
    authRepository: AuthRepository,
    playerController: MusicPlayerController
) : ViewModel() {

    val uiState: StateFlow<HomeLyricsHeaderUi> = combine(
        authRepository.observeLoginState().map { it.avatarUrl }.distinctUntilChanged(),
        playerController.playbackState
            .map { state ->
                Triple(
                    state.currentLyricLine,
                    state.isPlaying,
                    state.displaySong != null
                )
            }
            .distinctUntilChanged()
    ) { avatarUrl, playback ->
        HomeLyricsHeaderUi(
            currentLyricLine = playback.first,
            isPlaying = playback.second,
            hasPlaybackContent = playback.third,
            avatarUrl = avatarUrl
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeLyricsHeaderUi()
    )
}
