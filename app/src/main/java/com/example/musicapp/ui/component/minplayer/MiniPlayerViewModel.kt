package com.example.musicapp.ui.component.minplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.controller.player.MusicPlayerController
import com.example.musicapp.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// 迷你栏 UI 状态：只保留展示所需字段
data class MiniPlayerUi(
    val song: Song? = null,
    val isPlaying: Boolean = false,
    val isFavorite: Boolean = false
)

@HiltViewModel
class MiniPlayerViewModel @Inject constructor(
    private val playerController: MusicPlayerController
) : ViewModel() {

    // 只映射迷你栏需要的字段，字段不变则不向 UI 推送
    val uiState: StateFlow<MiniPlayerUi> = playerController.playbackState
        .map { state ->
            MiniPlayerUi(
                song = state.displaySong,
                isPlaying = state.isPlaying,
                isFavorite = state.isFavorite
            )
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MiniPlayerUi()
        )

    fun toggleFavorite() = playerController.toggleFavorite()

    fun togglePlayPause() = playerController.togglePlayPause()

    fun skipToNext() = playerController.skipToNext()
}
