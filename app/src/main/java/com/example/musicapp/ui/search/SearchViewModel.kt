package com.example.musicapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.controller.player.MusicPlayerController
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.usecase.AddRecentSearchUseCase
import com.example.musicapp.domain.usecase.ClearRecentSearchesUseCase
import com.example.musicapp.domain.usecase.ObserveRecentSearchesUseCase
import com.example.musicapp.domain.usecase.RemoveRecentSearchUseCase
import com.example.musicapp.domain.usecase.SearchSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 搜索页 UI 状态
data class SearchUiState(
    // 搜索框当前输入（草稿）
    val query: String = "",
    // 用户主动确认后的搜索关键词；非空时展示结果区
    val activeKeyword: String = "",
    // 当前搜索结果列表
    val songs: List<Song> = emptyList(),
    // 本地最近搜索词
    val recentSearches: List<String> = emptyList(),
    // 是否正在请求搜索接口
    val isLoading: Boolean = false,
    // 搜索失败时的错误信息
    val error: String? = null
)

// 搜索页 ViewModel
// 负责手动触发搜索、最近搜索持久化，以及点击歌曲后驱动全局播放器
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchSongsUseCase: SearchSongsUseCase,
    private val observeRecentSearchesUseCase: ObserveRecentSearchesUseCase,
    private val addRecentSearchUseCase: AddRecentSearchUseCase,
    private val removeRecentSearchUseCase: RemoveRecentSearchUseCase,
    private val clearRecentSearchesUseCase: ClearRecentSearchesUseCase,
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    // 对外只读，SearchScreen 通过 collect 订阅
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // 当前进行中的搜索任务；输入变化时会先取消上一次
    private var searchJob: Job? = null

    init {
        // 订阅本地最近搜索词
        viewModelScope.launch {
            observeRecentSearchesUseCase().collect { recentSearches ->
                _uiState.update { it.copy(recentSearches = recentSearches) }
            }
        }
    }

    // 搜索框内容变化：仅更新输入，不自动请求接口
    fun onQueryChange(query: String) {
        searchJob?.cancel()
        _uiState.update { state ->
            if (query.isBlank()) {
                state.copy(
                    query = "",
                    activeKeyword = "",
                    songs = emptyList(),
                    isLoading = false,
                    error = null
                )
            } else {
                val trimmed = query.trim()
                val keywordChanged = trimmed != state.activeKeyword
                state.copy(
                    query = query,
                    error = null,
                    activeKeyword = if (keywordChanged) "" else state.activeKeyword,
                    songs = if (keywordChanged) emptyList() else state.songs,
                    isLoading = false
                )
            }
        }
    }

    // 用户点击搜索按钮或键盘确认时调用
    // 将当前关键词写入最近搜索，并立即触发一次搜索
    fun confirmSearch() {
        val term = _uiState.value.query.trim()
        if (term.isEmpty()) return

        viewModelScope.launch {
            addRecentSearchUseCase(term)
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(activeKeyword = term) }
            performSearch(term)
        }
    }

    // 点击最近搜索 Chip：填入关键词并立即搜索
    fun onRecentSearchClick(term: String) {
        searchJob?.cancel()
        _uiState.update { it.copy(query = term, activeKeyword = term, error = null) }
        searchJob = viewModelScope.launch {
            performSearch(term)
        }
    }

    // 删除单条最近搜索
    fun removeRecentSearch(term: String) {
        viewModelScope.launch {
            removeRecentSearchUseCase(term)
        }
    }

    // 清空全部最近搜索
    fun clearRecentSearches() {
        viewModelScope.launch {
            clearRecentSearchesUseCase()
        }
    }

    // 点击搜索结果歌曲：以当前列表为队列播放，并同步迷你播放栏预览
    fun onSongClick(song: Song) {
        val queue = _uiState.value.songs
        playerController.playSong(song, queue)
        playerController.setPreviewSong(song)
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        runCatching {
            searchSongsUseCase(query)
        }.onSuccess { songs ->
            _uiState.update { it.copy(songs = songs, isLoading = false) }
        }.onFailure { throwable ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = throwable.message ?: "搜索失败，请确认 API 服务已启动"
                )
            }
        }
    }
}
