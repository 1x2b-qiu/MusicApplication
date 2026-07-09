package com.example.musicapp.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.usecase.GetSongUrlUseCase
import com.example.musicapp.domain.usecase.ObserveLoginStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val error: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getSongUrlUseCase: GetSongUrlUseCase,
    private val observeLoginStateUseCase: ObserveLoginStateUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val songId: Long = savedStateHandle.get<Long>("songId") ?: 0L
    private val songName: String = savedStateHandle.get<String>("songName").orEmpty()
    private val artistName: String = savedStateHandle.get<String>("artistName").orEmpty()
    private val coverUrl: String? = savedStateHandle.get<String>("coverUrl")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            songId = songId,
            songName = songName,
            artistName = artistName,
            coverUrl = coverUrl
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeLoginStateUseCase().collect { loginState ->
                val wasLoggedIn = _uiState.value.isLoggedIn
                _uiState.update { it.copy(isLoggedIn = loginState.isLoggedIn) }
                if (loginState.isLoggedIn && !wasLoggedIn && songId > 0L) {
                    loadSongUrl()
                }
            }
        }
        if (songId > 0L) {
            loadSongUrl()
        }
    }

    private fun loadSongUrl() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, playUrl = null) }
            runCatching {
                getSongUrlUseCase(songId)
            }.onSuccess { songUrl ->
                if (songUrl.url.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = if (it.isLoggedIn) {
                                "该歌曲暂无播放权限"
                            } else {
                                "该歌曲仅可试听约 30 秒，请登录后播放完整版"
                            }
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, playUrl = songUrl.url, error = null)
                    }
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "获取播放地址失败"
                    )
                }
            }
        }
    }
}
