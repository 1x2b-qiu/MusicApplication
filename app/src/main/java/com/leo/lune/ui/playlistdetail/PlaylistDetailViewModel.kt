package com.leo.lune.ui.playlistdetail

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.domain.model.PlaylistDetail
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.AuthRepository
import com.leo.lune.domain.repository.MusicRepository
import com.leo.lune.manager.FavoriteManager
import com.leo.lune.manager.FavoriteResult
import com.leo.lune.navigation.MusicRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 歌单详情页 UI 状态
data class PlaylistDetailUiState(
    // 歌单元数据（封面、标题、创建者等）；未加载成功前为 null
    val playlist: PlaylistDetail? = null,
    // 已加载歌曲，作播放队列；下滑续拉追加
    val songs: List<Song> = emptyList(),
    // 首屏加载中
    val isLoading: Boolean = false,
    // 分页续拉中
    val isLoadingMore: Boolean = false,
    // 是否还有下一页
    val hasMore: Boolean = false,
    // 加载失败时的错误信息
    val error: String? = null,
    // 是否正在播放，驱动主播放钮图标
    val isPlaying: Boolean = false,
    // 本页会话内是否已点过主播放钮；ViewModel 销毁后随状态重置
    val hasStartedPlayAll: Boolean = false,
    // 当前用户是否已收藏该歌单；驱动右上角红心
    val isSubscribed: Boolean = false,
    // 收藏请求进行中，避免连点重复提交
    val isSubscribeUpdating: Boolean = false
)

// 歌单详情页 ViewModel
// 按路由 playlistId 并行拉取元数据与首屏曲目；下滑分页续拉；播放委托全局播放器
@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val authRepository: AuthRepository,
    // 复用红心 Toast 通道，展示歌单收藏结果
    private val favoriteManager: FavoriteManager,
    // 全局播放控制器，本页不直接持有 ExoPlayer
    private val playerController: MusicPlayerController,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 导航参数（MusicRoute.PlaylistDetail）
    private val route = savedStateHandle.toRoute<MusicRoute.PlaylistDetail>()
    private val playlistId: Long = route.playlistId

    private val _uiState = MutableStateFlow(
        PlaylistDetailUiState(
            // 入口已有预览时先填 Hero / 背景，等详情接口覆盖
            playlist = previewFromRoute(route)
        )
    )
    // 对外只读，PlaylistDetailScreen 通过 collect 订阅
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    // 首屏加载任务，重试时可取消旧任务
    private var loadJob: Job? = null
    // 分页续拉任务；与首屏互斥，首屏重载时一并取消
    private var loadMoreJob: Job? = null
    // 当前进行中的收藏任务
    private var subscribeJob: Job? = null
    // 下一页 offset；null 表示没有更多或尚未完成首屏
    private var nextOffset: Int? = null

    init {
        loadInitial()
        // 只同步 isPlaying，驱动主播放钮播/停图标
        viewModelScope.launch {
            playerController.playbackState
                .map { it.isPlaying }
                .distinctUntilChanged()
                .collect { isPlaying ->
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                }
        }
    }

    // 点击歌曲：以当前已加载列表为队列开始播放
    @RequiresApi(Build.VERSION_CODES.O)
    fun onSongClick(song: Song) {
        val queue = _uiState.value.songs
        if (queue.isEmpty()) return
        playerController.playSong(song, queue)
    }

    // 主播放钮：本页首次点击从首曲连播已加载列表，之后切换播停（至 ViewModel 销毁）
    @RequiresApi(Build.VERSION_CODES.O)
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

    // 右上角红心：收藏 / 取消收藏歌单（乐观更新，失败回滚）
    fun onSubscribeClick() {
        val state = _uiState.value
        if (state.playlist == null || state.isSubscribeUpdating) return

        subscribeJob?.cancel()
        subscribeJob = viewModelScope.launch {
            val loginState = authRepository.observeLoginState().first()
            if (!loginState.isLoggedIn) {
                favoriteManager.emitResult(FavoriteResult.Failure("请先登录后收藏"))
                return@launch
            }

            val targetSubscribed = !state.isSubscribed
            _uiState.update {
                it.copy(
                    isSubscribed = targetSubscribed,
                    isSubscribeUpdating = true,
                    playlist = it.playlist?.copy(subscribed = targetSubscribed)
                )
            }

            runCatching {
                musicRepository.subscribePlaylist(playlistId, subscribe = targetSubscribed)
            }.onSuccess { result ->
                if (!result.success) {
                    revertSubscribe(targetSubscribed, "收藏操作失败")
                } else {
                    _uiState.update { it.copy(isSubscribeUpdating = false) }
                    favoriteManager.emitResult(
                        FavoriteResult.Success(
                            liked = targetSubscribed,
                            message = if (targetSubscribed) "已收藏歌单" else "已取消收藏"
                        )
                    )
                }
            }.onFailure { throwable ->
                revertSubscribe(targetSubscribed, throwable.message ?: "收藏操作失败")
            }
        }
    }

    // 加载失败后由 UI 触发重试（重新首屏加载）
    fun onRetry() {
        loadInitial()
    }

    // 列表接近底部时续拉下一页；无 offset / 正在加载 / 无更多则忽略
    fun onLoadMore() {
        val offset = nextOffset ?: return
        val state = _uiState.value
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
                    trackCount = _uiState.value.playlist?.trackCount
                )
                nextOffset = if (hasMore) offset + page.size else null
                _uiState.update { current ->
                    // 按 id 去重，避免分页边界偶发重复曲
                    val merged = (current.songs + page)
                        .distinctBy { it.id }
                    current.copy(
                        songs = merged,
                        hasMore = hasMore,
                        isLoadingMore = false
                    )
                }
            }.onFailure {
                // 续拉失败不打断已展示列表，仅结束 loadingMore
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    // 并行拉取歌单详情与首屏曲目；任一侧失败则整页错误
    private fun loadInitial() {
        loadJob?.cancel()
        loadMoreJob?.cancel()
        nextOffset = null
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
                coroutineScope {
                    val detailDeferred = async {
                        musicRepository.getPlaylistDetail(playlistId)
                    }
                    val songsDeferred = async {
                        musicRepository.getPlaylistSongs(
                            playlistId = playlistId,
                            limit = PageSize,
                            offset = 0
                        )
                    }
                    detailDeferred.await() to songsDeferred.await()
                }
            }.onSuccess { (detail, songs) ->
                val hasMore = resolveHasMore(
                    loadedCount = songs.size,
                    pageSize = songs.size,
                    trackCount = detail.trackCount
                )
                nextOffset = if (hasMore) songs.size else null
                _uiState.update {
                    // 入口已有封面时保留，避免详情回来重载闪一下
                    val keepCover = it.playlist?.coverUrl?.takeIf { url -> url.isNotBlank() }
                    it.copy(
                        playlist = if (keepCover != null) {
                            detail.copy(coverUrl = keepCover)
                        } else {
                            detail
                        },
                        songs = songs,
                        hasMore = hasMore,
                        isLoading = false,
                        error = null,
                        isSubscribed = detail.subscribed,
                        isSubscribeUpdating = false,
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

    // 收藏请求失败时回滚红心态
    private fun revertSubscribe(attemptedSubscribed: Boolean, message: String) {
        val rolledBack = !attemptedSubscribed
        _uiState.update {
            it.copy(
                isSubscribed = rolledBack,
                isSubscribeUpdating = false,
                playlist = it.playlist?.copy(subscribed = rolledBack)
            )
        }
        favoriteManager.emitResult(FavoriteResult.Failure(message))
    }

    companion object {
        // playlist/track/all 每页条数
        private const val PageSize = 30

        // 本页未满 → 无更多；否则对照 trackCount（>0 时）或默认还有下一页
        private fun resolveHasMore(
            loadedCount: Int,
            pageSize: Int,
            trackCount: Int?
        ): Boolean {
            if (pageSize < PageSize) return false
            val total = trackCount?.takeIf { it > 0 }
            return if (total != null) loadedCount < total else true
        }

        // 由路由预填的轻量歌单元数据；名称与封面都空则返回 null
        private fun previewFromRoute(route: MusicRoute.PlaylistDetail): PlaylistDetail? {
            if (route.playlistName.isBlank() && route.coverUrl.isBlank()) return null
            return PlaylistDetail(
                id = route.playlistId,
                name = route.playlistName,
                description = null,
                coverUrl = route.coverUrl.takeIf { it.isNotBlank() },
                trackCount = route.trackCount.coerceAtLeast(0),
                tags = emptyList(),
                creatorName = null,
                creatorAvatarUrl = null,
                subscribed = false
            )
        }
    }
}
