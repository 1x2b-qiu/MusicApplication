package com.leo.lune.ui.dailymix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.AuthRepository
import com.leo.lune.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 「每日推荐」页 UI 状态
data class DailyMixUiState(
    // 当日推荐全量歌曲
    val songs: List<Song> = emptyList(),
    // 当前展示列表：确认搜索后主动算好；无关键词时等于 songs
    val filteredSongs: List<Song> = emptyList(),
    // 搜索框当前输入（草稿，输入时不筛选）
    val query: String = "",
    // 用户确认搜索后的关键词；空则展示全量
    val activeKeyword: String = "",
    // 是否正在拉取每日推荐
    val isLoading: Boolean = false,
    // 加载失败时的错误信息
    val error: String? = null,
    // 是否正在播放，驱动身份区主播放钮图标
    val isPlaying: Boolean = false,
    // 当前播放歌曲 ID
    val currentSongId: Long? = null,
    // 本页会话内是否已点过主播放钮；ViewModel 销毁后随状态重置
    val hasStartedPlayAll: Boolean = false
)

// 「每日推荐」页 ViewModel
// 负责拉取每日推荐、本地筛选、同步播放状态；播放操作委托给全局播放器
@HiltViewModel
class DailyMixViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val authRepository: AuthRepository,
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyMixUiState())
    val uiState: StateFlow<DailyMixUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        // 登录态变化后重新拉取；未登录清空列表
        viewModelScope.launch {
            authRepository.observeLoginState().collect { loginState ->
                if (loginState.isLoggedIn) {
                    loadDailyMix()
                } else {
                    loadJob?.cancel()
                    _uiState.update {
                        it.copy(
                            songs = emptyList(),
                            filteredSongs = emptyList(),
                            isLoading = false,
                            error = "登录后查看每日推荐"
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            playerController.playbackState
                .map { state ->
                    DailyMixUiState(
                        isPlaying = state.isPlaying,
                        currentSongId = state.currentSong?.id
                    )
                }
                .distinctUntilChanged()
                .collect { playback ->
                    _uiState.update {
                        it.copy(
                            isPlaying = playback.isPlaying,
                            currentSongId = playback.currentSongId
                        )
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { state ->
            if (query.isBlank()) {
                state.copy(
                    query = "",
                    activeKeyword = "",
                    filteredSongs = state.songs
                )
            } else {
                state.copy(query = query)
            }
        }
    }

    fun confirmSearch() {
        val keyword = _uiState.value.query.trim()
        _uiState.update { state ->
            state.copy(
                activeKeyword = keyword,
                filteredSongs = filterSongs(state.songs, keyword)
            )
        }
    }

    fun onSongClick(song: Song) {
        val queue = _uiState.value.filteredSongs
        if (queue.isEmpty()) return
        playerController.playSong(song, queue)
    }

    fun onPlayAllClick() {
        val state = _uiState.value
        val queue = state.songs
        if (queue.isEmpty()) return
        if (state.hasStartedPlayAll) {
            playerController.togglePlayPause()
        } else {
            _uiState.update { it.copy(hasStartedPlayAll = true) }
            playerController.playSong(queue.first(), queue)
        }
    }

    fun onRetry() {
        loadDailyMix()
    }

    private fun loadDailyMix() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                musicRepository.getDailyRecommendSongs()
            }.onSuccess { songs ->
                _uiState.update { state ->
                    state.copy(
                        songs = songs,
                        filteredSongs = filterSongs(songs, state.activeKeyword),
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "加载失败，请稍后重试"
                    )
                }
            }
        }
    }

    companion object {
        fun filterSongs(songs: List<Song>, keyword: String): List<Song> {
            val key = keyword.trim()
            if (key.isEmpty()) return songs
            return songs.filter { song ->
                song.name.contains(key, ignoreCase = true) ||
                    song.artists.contains(key, ignoreCase = true) ||
                    song.album.contains(key, ignoreCase = true)
            }
        }
    }
}
