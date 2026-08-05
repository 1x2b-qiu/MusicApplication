package com.leo.lune.ui.search

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.domain.model.SearchSuggestion
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.MusicRepository
import com.leo.lune.domain.repository.SearchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    // 热搜关键词
    val hotSearches: List<String> = emptyList(),
    // 当前输入对应的联想建议
    val suggestions: List<SearchSuggestion> = emptyList(),
    // 是否正在请求联想（debounce / 网络中）
    val isSuggestLoading: Boolean = false,
    // 是否正在请求搜索接口
    val isLoading: Boolean = false,
    // 搜索失败时的错误信息
    val error: String? = null
)

// 搜索页 ViewModel
// 负责手动触发搜索、热搜/联想、最近搜索持久化，以及点击歌曲后驱动全局播放器
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    // 对外只读，SearchScreen 通过 collect 订阅
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // 当前进行中的搜索任务；输入变化时会先取消上一次
    private var searchJob: Job? = null
    // 搜索联想 debounce 任务
    private var suggestJob: Job? = null

    init {
        // 订阅本地最近搜索词
        viewModelScope.launch {
            searchHistoryRepository.observeRecentSearches().collect { recentSearches ->
                _uiState.update { it.copy(recentSearches = recentSearches) }
            }
        }
        loadHotSearches()
    }

    // 进入页面拉取热搜；失败时静默为空，不影响最近搜索
    private fun loadHotSearches() {
        viewModelScope.launch {
            runCatching {
                musicRepository.getHotSearchTerms()
            }.onSuccess { terms ->
                _uiState.update { it.copy(hotSearches = terms) }
            }
        }
    }

    // 搜索框内容变化：更新输入，并 debounce 请求联想
    fun onQueryChange(query: String) {
        searchJob?.cancel()
        suggestJob?.cancel()
        _uiState.update { state ->
            if (query.isBlank()) {
                state.copy(
                    query = "",
                    activeKeyword = "",
                    songs = emptyList(),
                    suggestions = emptyList(),
                    isSuggestLoading = false,
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
                    suggestions = if (keywordChanged) emptyList() else state.suggestions,
                    isSuggestLoading = keywordChanged,
                    isLoading = false
                )
            }
        }

        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        // 已展示结果区时不拉联想
        if (trimmed == _uiState.value.activeKeyword) return

        suggestJob = viewModelScope.launch {
            delay(SuggestDebounceMs)
            runCatching {
                musicRepository.getSearchSuggestions(trimmed)
            }.onSuccess { suggestions ->
                if (shouldApplySuggestions(trimmed)) {
                    _uiState.update {
                        it.copy(suggestions = suggestions, isSuggestLoading = false)
                    }
                }
            }.onFailure {
                if (shouldApplySuggestions(trimmed)) {
                    _uiState.update {
                        it.copy(suggestions = emptyList(), isSuggestLoading = false)
                    }
                }
            }
        }
    }

    // 用户点击搜索按钮或键盘确认
    fun confirmSearch() {
        searchWithKeyword(_uiState.value.query)
    }

    // 点击最近搜索 Chip
    fun onRecentSearchClick(term: String) {
        searchWithKeyword(term)
    }

    // 点击热搜 Chip
    fun onHotSearchClick(term: String) {
        searchWithKeyword(term)
    }

    // 点击联想建议
    fun onSuggestionClick(suggestion: SearchSuggestion) {
        searchWithKeyword(suggestion.text)
    }

    // 写入最近搜索并立即触发搜索
    private fun searchWithKeyword(raw: String) {
        val term = raw.trim()
        if (term.isEmpty()) return

        suggestJob?.cancel()
        searchJob?.cancel()

        viewModelScope.launch {
            searchHistoryRepository.addRecentSearch(term)
        }

        _uiState.update {
            it.copy(
                query = term,
                activeKeyword = term,
                suggestions = emptyList(),
                isSuggestLoading = false,
                error = null
            )
        }
        searchJob = viewModelScope.launch {
            performSearch(term)
        }
    }

    // 删除单条最近搜索
    fun removeRecentSearch(term: String) {
        viewModelScope.launch {
            searchHistoryRepository.removeRecentSearch(term)
        }
    }

    // 清空全部最近搜索
    fun clearRecentSearches() {
        viewModelScope.launch {
            searchHistoryRepository.clearRecentSearches()
        }
    }

    // 点击搜索结果歌曲：以当前列表为队列播放，并同步迷你播放栏预览
    @RequiresApi(Build.VERSION_CODES.O)
    fun onSongClick(song: Song) {
        val queue = _uiState.value.songs
        playerController.playSong(song, queue)
        playerController.setPreviewSong(song)
    }

    private fun shouldApplySuggestions(requestQuery: String): Boolean {
        val state = _uiState.value
        return state.query.trim() == requestQuery && state.activeKeyword.isBlank()
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        runCatching {
            musicRepository.searchSongs(query)
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

    private companion object {
        const val SuggestDebounceMs = 180L
    }
}
