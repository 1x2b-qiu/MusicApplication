package com.leo.lune.ui.liked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.usecase.music.GetLikedMusicPlaylistSongsUseCase
import com.leo.lune.domain.usecase.auth.ObserveLoginStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 「我喜欢的」页 UI 状态
data class LikedUiState(
    // 全量喜欢歌曲（不截断，与首页轮播的前 N 首区分）
    val songs: List<Song> = emptyList(),
    // 当前展示列表：确认搜索后主动算好；无关键词时等于 songs
    val filteredSongs: List<Song> = emptyList(),
    // 搜索框当前输入（草稿，输入时不筛选）
    val query: String = "",
    // 用户确认搜索后的关键词；空则展示全量
    val activeKeyword: String = "",
    // 是否正在拉取喜欢歌单
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

// 「我喜欢的」页 ViewModel
// 负责拉取全量喜欢歌单、本地筛选、同步播放状态；播放操作委托给全局播放器
@HiltViewModel
class LikedViewModel @Inject constructor(
    // 拉取网易云「我喜欢的音乐」歌单全部歌曲
    private val getLikedMusicPlaylistSongsUseCase: GetLikedMusicPlaylistSongsUseCase,
    // 进页时读取一次当前 userId
    private val observeLoginStateUseCase: ObserveLoginStateUseCase,
    // 全局播放控制器，本页不直接持有 ExoPlayer
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(LikedUiState())
    // 对外只读，LikedScreen 通过 collect 订阅
    val uiState: StateFlow<LikedUiState> = _uiState.asStateFlow()

    // 当前进行中的歌单加载任务，重试时可取消旧任务
    private var loadJob: Job? = null
    // 进页时读到的 userId，供失败重试使用
    private var cachedUserId: Long? = null

    init {
        // 进页时只取一次 userId，再拉取全量喜欢歌曲（本页进入时已登录）
        viewModelScope.launch {
            cachedUserId = observeLoginStateUseCase().first().userId
            loadLikedSongs()
        }
        // 只取本页需要的播放字段，过滤无关更新
        viewModelScope.launch {
            playerController.playbackState
                .map { state ->
                    LikedUiState(
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

    // 搜索框内容变化：只改草稿，不立刻筛选；清空时一并恢复全量列表
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

    // 手动确认搜索：按当前输入主动算出展示列表
    fun confirmSearch() {
        val keyword = _uiState.value.query.trim()
        _uiState.update { state ->
            state.copy(
                activeKeyword = keyword,
                filteredSongs = filterSongs(state.songs, keyword)
            )
        }
    }

    // 点击歌曲：以当前筛选结果为队列开始播放
    fun onSongClick(song: Song) {
        val queue = _uiState.value.filteredSongs
        if (queue.isEmpty()) return
        playerController.playSong(song, queue)
    }

    // 身份区主播放钮：本页首次点击从首曲连播全量，之后切换播停（至 ViewModel 销毁）
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

    // 加载失败后由 UI 触发重试
    fun onRetry() {
        loadLikedSongs()
    }

    // 拉取全量喜欢歌单
    private fun loadLikedSongs() {
        val userId = cachedUserId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                // limit = null：不截断，返回歌单内全部歌曲
                getLikedMusicPlaylistSongsUseCase(userId, limit = null)
            }.onSuccess { songs ->
                _uiState.update { state ->
                    state.copy(
                        songs = songs,
                        // 列表刷新后按当前关键词重新筛一遍
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
        // 按关键词本地过滤喜欢列表（不区分大小写）；空关键词返回全量
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
