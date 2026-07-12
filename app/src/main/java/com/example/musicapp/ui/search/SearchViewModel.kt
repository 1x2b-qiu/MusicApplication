package com.example.musicapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.model.LoginState
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.usecase.LogoutUseCase
import com.example.musicapp.domain.usecase.ObserveLoginStateUseCase
import com.example.musicapp.domain.usecase.SearchSongsUseCase
import com.example.musicapp.controller.player.MusicPlayerController
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
    // 搜索框输入内容
    val query: String = "",
    // 当前搜索结果列表
    val songs: List<Song> = emptyList(),
    // 是否正在请求搜索接口
    val isLoading: Boolean = false,
    // 搜索失败时的错误信息
    val error: String? = null,
    // 登录状态，供顶部栏展示昵称 / 登录入口
    val loginState: LoginState = LoginState()
)

// 搜索页 ViewModel
// 负责防抖搜索、同步登录状态，以及点击歌曲后驱动全局播放器
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchSongsUseCase: SearchSongsUseCase,
    private val observeLoginStateUseCase: ObserveLoginStateUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    // 对外只读，SearchScreen 通过 collect 订阅
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // 当前进行中的搜索任务；输入变化时会先取消上一次
    private var searchJob: Job? = null

    init {
        // 订阅登录状态变化，同步到 UI
        viewModelScope.launch {
            observeLoginStateUseCase().collect { loginState ->
                _uiState.update { it.copy(loginState = loginState) }
            }
        }
    }

    // 搜索框内容变化
    // 空关键词时清空结果；非空时延迟 400ms 防抖后再请求接口
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, error = null) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(songs = emptyList(), isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
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

    // 退出登录
    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }

    // 点击搜索结果歌曲
    // 立即播放该曲，并同步更新迷你播放栏预览
    fun onSongClick(song: Song) {
        playerController.playSong(song, emptyList())
        playerController.setPreviewSong(song)
    }
}
