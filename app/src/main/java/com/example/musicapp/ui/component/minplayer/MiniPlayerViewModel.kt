package com.example.musicapp.ui.component.minplayer

import androidx.lifecycle.ViewModel
import com.example.musicapp.controller.player.MusicPlayerController
import com.example.musicapp.controller.player.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MiniPlayerViewModel @Inject constructor(
    private val playerController: MusicPlayerController
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerController.playbackState

    fun toggleFavorite() = playerController.toggleFavorite()

    fun togglePlayPause() = playerController.togglePlayPause()

    fun skipToNext() = playerController.skipToNext()
}
