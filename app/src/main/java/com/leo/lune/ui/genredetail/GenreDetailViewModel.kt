package com.leo.lune.ui.genredetail

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.MusicRepository
import com.leo.lune.navigation.MusicRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 曲风详情页 UI 状态
data class GenreDetailUiState(
    // 路由中的曲风 tagId（style/list / style/song）
    val styleId: Long = 0L,
    // 曲风名：优先路由传入，详情接口返回后覆盖
    val title: String = "",
    // 曲风简介（style/detail.desc）
    val description: String? = null,
    // 曲风封面；优先入口传入，详情失败时回落首曲封面
    val coverUrl: String? = null,
    // 接口数量文案（如「999999+」）；无则 UI 回落已加载首数
    val songCountLabel: String? = null,
    // 已加载单曲，作播放队列
    val songs: List<Song> = emptyList(),
    // 首屏加载中
    val isLoading: Boolean = false,
    // 分页续拉中
    val isLoadingMore: Boolean = false,
    // 是否还有下一页
    val hasMore: Boolean = false,
    val error: String? = null,
    // 是否正在播放，驱动主播放钮图标
    val isPlaying: Boolean = false,
    // 本页会话内是否已点过主播放钮；ViewModel 销毁后随状态重置
    val hasStartedPlayAll: Boolean = false
)

// 曲风详情页 ViewModel
// 按路由 styleId 并行拉取曲风元数据与首屏单曲；下滑分页续拉；播放委托全局播放器
@HiltViewModel
class GenreDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    // 全局播放控制器，本页不直接持有 ExoPlayer
    private val playerController: MusicPlayerController,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 导航参数（MusicRoute.GenreDetail）
    private val route = savedStateHandle.toRoute<MusicRoute.GenreDetail>()
    // 曲风 tagId，用于 style/detail、style/song
    private val styleId: Long = route.styleId

    private val _uiState = MutableStateFlow(
        GenreDetailUiState(
            styleId = styleId,
            // 进页即可展示曲库格子上的名称与封面
            title = route.styleName,
            coverUrl = route.coverUrl.takeIf { it.isNotBlank() }
        )
    )
    // 对外只读，GenreDetailScreen 通过 collect 订阅
    val uiState: StateFlow<GenreDetailUiState> = _uiState.asStateFlow()

    // 首屏加载任务，重试时可取消旧任务
    private var loadJob: Job? = null
    // 分页续拉任务；与首屏互斥，首屏重载时一并取消
    private var loadMoreJob: Job? = null
    // 下一页 cursor；null 表示没有更多或尚未完成首屏
    private var nextCursor: Long? = null

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

    // 主播放钮：本页首次点击从首曲连播，之后切换播停（至 ViewModel 销毁）
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

    // 加载失败后由 UI 触发重试（重新首屏加载）
    fun onRetry() {
        loadInitial()
    }

    // 列表接近底部时续拉下一页；无 cursor / 正在加载 / 无更多则忽略
    fun onLoadMore() {
        val cursor = nextCursor ?: return
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            runCatching {
                musicRepository.getMusicStyleSongs(
                    styleId = styleId,
                    cursor = cursor,
                    size = PageSize
                )
            }.onSuccess { page ->
                nextCursor = page.nextCursor
                _uiState.update { current ->
                    // 按 id 去重，避免分页边界偶发重复曲
                    val merged = (current.songs + page.songs)
                        .distinctBy { it.id }
                    current.copy(
                        songs = merged,
                        hasMore = page.hasMore,
                        isLoadingMore = false
                    )
                }
            }.onFailure {
                // 续拉失败不打断已展示列表，仅结束 loadingMore
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    // 并行拉取曲风详情与首屏单曲；详情失败仍可展示歌曲列表
    private fun loadInitial() {
        loadJob?.cancel()
        loadMoreJob?.cancel()
        nextCursor = null
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
                    // 详情非关键路径：失败返回 null，标题回落路由 styleName
                    val detailDeferred = async {
                        runCatching { musicRepository.getMusicStyleDetail(styleId) }.getOrNull()
                    }
                    val songsDeferred = async {
                        musicRepository.getMusicStyleSongs(
                            styleId = styleId,
                            cursor = 0,
                            size = PageSize
                        )
                    }
                    detailDeferred.await() to songsDeferred.await()
                }
            }.onSuccess { (detail, page) ->
                nextCursor = page.nextCursor
                _uiState.update { current ->
                    // 入口已有封面时保留，避免详情回来重载闪一下
                    val keepCover = current.coverUrl?.takeIf { it.isNotBlank() }
                    current.copy(
                        title = detail?.name?.takeIf { it.isNotBlank() }
                            ?: current.title,
                        description = detail?.description,
                        coverUrl = keepCover
                            ?: detail?.coverUrl
                            ?: page.songs.firstOrNull()?.coverUrl,
                        songCountLabel = detail?.songCountLabel,
                        songs = page.songs,
                        hasMore = page.hasMore,
                        isLoading = false,
                        error = null,
                        // 列表刷新后允许重新「首次播放」
                        hasStartedPlayAll = false
                    )
                }
            }.onFailure { throwable ->
                // 仅当首屏歌曲请求失败时进入整页错误态
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
        // style/song 每页条数
        private const val PageSize = 30
    }
}
