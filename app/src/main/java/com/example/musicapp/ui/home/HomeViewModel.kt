package com.example.musicapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.model.LoginState
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.usecase.ObserveLoginStateUseCase
import com.example.musicapp.domain.usecase.SearchSongsUseCase
import com.example.musicapp.player.MusicPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeCategory(
    val id: String,
    val label: String,
    val keyword: String
)

data class HomeUiState(
    val greetingPrefix: String = "晚上好",
    val userName: String = "Alex",
    val categories: List<HomeCategory> = defaultCategories,
    val selectedCategoryId: String = "focus",
    val featuredSong: Song? = null,
    val recommendedSongs: List<Song> = emptyList(),
    val recentSongs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasNotification: Boolean = true,
    val loginState: LoginState = LoginState()
)

private val defaultCategories = listOf(
    HomeCategory("focus", "专注", "轻音乐"),
    HomeCategory("night", "深夜", "深夜"),
    HomeCategory("rhythm", "律动", "电子"),
    HomeCategory("relax", "放松", "放松"),
    HomeCategory("commute", "通勤", "流行"),
    HomeCategory("fitness", "健身", "运动"),
    HomeCategory("retro", "复古", "复古")
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val searchSongsUseCase: SearchSongsUseCase,
    private val observeLoginStateUseCase: ObserveLoginStateUseCase,
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        _uiState.update { it.copy(greetingPrefix = resolveGreetingPrefix()) }
        viewModelScope.launch {
            observeLoginStateUseCase().collect { loginState ->
                _uiState.update {
                    it.copy(
                        loginState = loginState,
                        userName = loginState.nickname ?: "Alex"
                    )
                }
            }
        }
        loadHomeContent()
    }

    fun onCategorySelected(categoryId: String) {
        if (categoryId == _uiState.value.selectedCategoryId) return
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        loadHomeContent()
    }

    fun onNotificationClick() {
        _uiState.update { it.copy(hasNotification = !it.hasNotification) }
    }

    fun onRetry() {
        loadHomeContent()
    }

    fun playSong(song: Song, queue: List<Song>) {
        playerController.playSong(song, queue)
    }

    private fun loadHomeContent() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val category = _uiState.value.categories
                .firstOrNull { it.id == _uiState.value.selectedCategoryId }
                ?: defaultCategories.first()

            runCatching {
                val recommended = searchSongsUseCase(category.keyword, limit = 8)
                val recent = searchSongsUseCase("热门", limit = 4)
                val featured = recommended.firstOrNull()
                Triple(featured, recommended, recent)
            }.onSuccess { (featured, recommended, recent) ->
                featured?.let(playerController::setPreviewSong)
                _uiState.update {
                    it.copy(
                        featuredSong = featured,
                        recommendedSongs = recommended,
                        recentSongs = recent,
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "加载失败，请确认 API 服务已启动"
                    )
                }
            }
        }
    }

    private fun resolveGreetingPrefix(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "早上好"
            in 12..17 -> "下午好"
            else -> "晚上好"
        }
    }
}

fun formatSongDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
