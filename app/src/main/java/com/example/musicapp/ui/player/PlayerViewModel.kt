package com.example.musicapp.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.usecase.ObserveLoginStateUseCase
import com.example.musicapp.player.MusicPlayerController
import com.example.musicapp.player.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val songId: Long = 0L,
    val songName: String = "",
    val artistName: String = "",
    val coverUrl: String? = null,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val playUrl: String? = null,
    val error: String? = null,
    val isPlaying: Boolean = false
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: MusicPlayerController,
    observeLoginStateUseCase: ObserveLoginStateUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val navSongId: Long = savedStateHandle.get<Long>("songId") ?: 0L
    private val navSongName: String = savedStateHandle.get<String>("songName").orEmpty()
    private val navArtistName: String = savedStateHandle.get<String>("artistName").orEmpty()
    private val navCoverUrl: String? = savedStateHandle.get<String>("coverUrl")?.takeIf { it.isNotBlank() }

    val exoPlayer get() = playerController.exoPlayer

    val uiState: StateFlow<PlayerUiState> = combine(
        playerController.playbackState,
        observeLoginStateUseCase()
    ) { playback, loginState ->
        playback.toPlayerUiState(loginState.isLoggedIn)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState()
    )

    init {
        val current = playerController.playbackState.value.currentSong
        if (current == null && navSongId > 0L) {
            playerController.playSong(
                Song(
                    id = navSongId,
                    name = navSongName,
                    artists = navArtistName,
                    album = "",
                    coverUrl = navCoverUrl,
                    durationMs = 0L
                )
            )
        }
    }

    private fun PlaybackState.toPlayerUiState(isLoggedIn: Boolean): PlayerUiState {
        val song = currentSong
        return PlayerUiState(
            songId = song?.id ?: navSongId,
            songName = song?.name ?: navSongName,
            artistName = song?.artists ?: navArtistName,
            coverUrl = song?.coverUrl ?: navCoverUrl,
            isLoggedIn = isLoggedIn,
            isLoading = isLoading,
            playUrl = playUrl,
            error = error,
            isPlaying = isPlaying
        )
    }
}
