package com.leo.lune.ui.liked

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

// 「我喜欢的」页 UI 状态
data class LikedUiState(
    // 已加载喜欢歌曲；下滑续拉追加（与首页轮播的前 N 首区分）
    val songs: List<Song> = emptyList(),
    // 当前展示列表：确认搜索后主动算好；无关键词时等于 songs
    val filteredSongs: List<Song> = emptyList(),
    // 歌单 trackCount；身份区曲数优先用它
    val trackCount: Int = 0,
    // 搜索框当前输入（草稿，输入时不筛选）
    val query: String = "",
    // 用户确认搜索后的关键词；空则展示已加载全量
    val activeKeyword: String = "",
    // 是否正在拉取喜欢歌单首屏
    val isLoading: Boolean = false,
    // 分页续拉中
    val isLoadingMore: Boolean = false,
    // 是否还有下一页
    val hasMore: Boolean = false,
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
// 负责分页拉取喜欢歌单、本地筛选、同步播放状态；播放操作委托给全局播放器
@HiltViewModel
class LikedViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val authRepository: AuthRepository,
    // 全局播放控制器，本页不直接持有 ExoPlayer
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(LikedUiState())
    // 对外只读，LikedScreen 通过 collect 订阅
    val uiState: StateFlow<LikedUiState> = _uiState.asStateFlow()

    // 首屏加载任务，重试时可取消旧任务
    private var loadJob: Job? = null
    // 分页续拉任务；与首屏互斥，首屏重载时一并取消
    private var loadMoreJob: Job? = null
    // 当前登录用户 id，随登录态持续更新
    private var cachedUserId: Long? = null
    // 喜欢歌单 id；首屏解析后供续拉使用
    private var likedPlaylistId: Long? = null
    // 下一页 offset；null 表示没有更多或尚未完成首屏
    private var nextOffset: Int? = null

    init {
        // 持续观察登录态：userId 变化（会话恢复 / 登录 / 登出）后重新拉取喜欢歌单
        viewModelScope.launch {
            authRepository.observeLoginState().collect { loginState ->
                cachedUserId = loginState.userId
                loadLikedSongs()
            }
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

    // 搜索框内容变化：只改草稿，不立刻筛选；清空时一并恢复已加载列表
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

    // 身份区主播放钮：本页首次点击从首曲连播已加载列表，之后切换播停（至 ViewModel 销毁）
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

    // 加载失败后由 UI 触发重试（重新首屏加载）
    fun onRetry() {
        loadLikedSongs()
    }

    // 列表接近底部时续拉下一页；搜索筛选中不续拉（只筛已加载）
    fun onLoadMore() {
        val playlistId = likedPlaylistId ?: return
        val offset = nextOffset ?: return
        val state = _uiState.value
        if (state.activeKeyword.isNotBlank()) return
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            runCatching {
                musicRepository.getPlaylistSongs(
                    playlistId = playlistId,
                    limit = PageSize,
                    offset = offset
                )
            }.onSuccess { page ->
                val hasMore = resolveHasMore(
                    loadedCount = offset + page.size,
                    pageSize = page.size,
                    trackCount = _uiState.value.trackCount
                )
                nextOffset = if (hasMore) offset + page.size else null
                _uiState.update { current ->
                    val merged = (current.songs + page).distinctBy { it.id }
                    current.copy(
                        songs = merged,
                        filteredSongs = filterSongs(merged, current.activeKeyword),
                        hasMore = hasMore,
                        isLoadingMore = false
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    // 拉取喜欢歌单首屏
    private fun loadLikedSongs() {
        val userId = cachedUserId ?: return
        loadJob?.cancel()
        loadMoreJob?.cancel()
        nextOffset = null
        likedPlaylistId = null
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    error = null,
                    hasMore = false
                )
            }
            runCatching {
                val likedPlaylist = musicRepository.getUserPlaylists(userId)
                    .firstOrNull { it.isLikedMusicPlaylist }
                    ?: error("Liked music playlist not found for user $userId")
                likedPlaylistId = likedPlaylist.id
                val songs = musicRepository.getPlaylistSongs(
                    playlistId = likedPlaylist.id,
                    limit = PageSize,
                    offset = 0
                )
                Triple(likedPlaylist.id, likedPlaylist.trackCount, songs)
            }.onSuccess { (_, trackCount, songs) ->
                val total = trackCount.takeIf { it > 0 } ?: songs.size
                val hasMore = resolveHasMore(
                    loadedCount = songs.size,
                    pageSize = songs.size,
                    trackCount = total
                )
                nextOffset = if (hasMore) songs.size else null
                _uiState.update { state ->
                    state.copy(
                        songs = songs,
                        filteredSongs = filterSongs(songs, state.activeKeyword),
                        trackCount = total,
                        hasMore = hasMore,
                        isLoading = false,
                        error = null,
                        // 列表刷新后允许重新「首次播放」
                        hasStartedPlayAll = false
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
        // playlist/track/all 每页条数（与歌单详情一致）
        private const val PageSize = 30

        // 本页未满 → 无更多；否则对照 trackCount（>0 时）或默认还有下一页
        private fun resolveHasMore(
            loadedCount: Int,
            pageSize: Int,
            trackCount: Int
        ): Boolean {
            if (pageSize < PageSize) return false
            val total = trackCount.takeIf { it > 0 }
            return if (total != null) loadedCount < total else true
        }

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
