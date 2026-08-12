package com.leo.lune.ui.charts

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

// 排行榜详情页 UI 状态
data class ChartDetailUiState(
    // 路由中的榜单 ID（网易云官方榜对应的歌单 ID）
    val chartId: Long = 0L,
    // 榜名：优先 FixedCharts，否则用接口返回的歌单名
    val title: String = "",
    // FixedCharts 副标题，如「实时热门」；信息区无副标题时可回落 tags
    val subtitle: String = "",
    val coverUrl: String? = null,
    val trackCount: Int = 0,
    // 创建者昵称 / 头像（来自 playlist/detail）
    val creatorName: String? = null,
    val creatorAvatarUrl: String? = null,
    // 榜单介绍（歌单 description）
    val description: String? = null,
    val tags: List<String> = emptyList(),
    // 已加载歌曲，作播放队列；下滑续拉追加
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

// 排行榜详情页 ViewModel
// 按路由 chartId 并行拉取歌单元数据与首屏曲目；下滑分页续拉；播放委托全局播放器
@HiltViewModel
class ChartDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    // 全局播放控制器，本页不直接持有 ExoPlayer
    private val playerController: MusicPlayerController,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 导航参数中的榜单 ID（MusicRoute.ChartDetail）
    private val chartId: Long =
        savedStateHandle.toRoute<MusicRoute.ChartDetail>().chartId

    // 本地固定榜配置；不在列表内时为 null，标题回落接口 name
    private val chartSpec: ChartSpec? = FixedCharts.firstOrNull { it.id == chartId }

    private val _uiState = MutableStateFlow(
        ChartDetailUiState(
            chartId = chartId,
            title = chartSpec?.title.orEmpty(),
            subtitle = chartSpec?.subtitle.orEmpty()
        )
    )
    // 对外只读，ChartDetailScreen 通过 collect 订阅
    val uiState: StateFlow<ChartDetailUiState> = _uiState.asStateFlow()

    // 首屏加载任务，重试时可取消旧任务
    private var loadJob: Job? = null
    // 分页续拉任务；与首屏互斥，首屏重载时一并取消
    private var loadMoreJob: Job? = null
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
                    playlistId = chartId,
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

    // 并行拉取榜单元数据与首屏曲目；任一侧失败则整页错误
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
                        musicRepository.getPlaylistDetail(chartId)
                    }
                    val songsDeferred = async {
                        musicRepository.getPlaylistSongs(
                            playlistId = chartId,
                            limit = PageSize,
                            offset = 0
                        )
                    }
                    detailDeferred.await() to songsDeferred.await()
                }
            }.onSuccess { (detail, songs) ->
                val trackCount = if (detail.trackCount > 0) {
                    detail.trackCount
                } else {
                    songs.size
                }
                val hasMore = resolveHasMore(
                    loadedCount = songs.size,
                    pageSize = songs.size,
                    trackCount = trackCount
                )
                nextOffset = if (hasMore) songs.size else null
                _uiState.update {
                    it.copy(
                        title = chartSpec?.title?.takeIf { name -> name.isNotBlank() }
                            ?: detail.name,
                        subtitle = chartSpec?.subtitle.orEmpty(),
                        coverUrl = detail.coverUrl
                            ?: songs.firstOrNull()?.coverUrl,
                        trackCount = trackCount,
                        creatorName = detail.creatorName,
                        creatorAvatarUrl = detail.creatorAvatarUrl,
                        description = detail.description,
                        tags = detail.tags,
                        songs = songs,
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
    }
}
