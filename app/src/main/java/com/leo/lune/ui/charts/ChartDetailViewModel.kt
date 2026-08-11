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
    // 榜内全部歌曲，作播放队列
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // 是否正在播放，驱动主播放钮图标
    val isPlaying: Boolean = false,
    // 本页会话内是否已点过主播放钮；ViewModel 销毁后随状态重置
    val hasStartedPlayAll: Boolean = false
)

// 排行榜详情页 ViewModel
// 按路由 chartId 并行拉取歌单元数据与曲目；标题优先用 FixedCharts；播放委托全局播放器
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

    // 当前进行中的加载任务，重试时可取消旧任务
    private var loadJob: Job? = null

    init {
        loadChart()
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

    // 点击歌曲：以当前榜单列表为队列开始播放
    @RequiresApi(Build.VERSION_CODES.O)
    fun onSongClick(song: Song) {
        val queue = _uiState.value.songs
        if (queue.isEmpty()) return
        playerController.playSong(song, queue)
    }

    // 主播放钮：本页首次点击从首曲连播全量，之后切换播停（至 ViewModel 销毁）
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

    // 加载失败后由 UI 触发重试
    fun onRetry() {
        loadChart()
    }

    // 并行拉取榜单元数据与全部曲目；任一侧失败则整页错误
    private fun loadChart() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                coroutineScope {
                    val detailDeferred = async {
                        musicRepository.getPlaylistDetail(chartId)
                    }
                    val songsDeferred = async {
                        // limit = null：不截断，返回榜内全部歌曲
                        musicRepository.getPlaylistSongs(chartId, limit = null)
                    }
                    detailDeferred.await() to songsDeferred.await()
                }
            }.onSuccess { (detail, songs) ->
                _uiState.update {
                    it.copy(
                        title = chartSpec?.title?.takeIf { name -> name.isNotBlank() }
                            ?: detail.name,
                        subtitle = chartSpec?.subtitle.orEmpty(),
                        coverUrl = detail.coverUrl
                            ?: songs.firstOrNull()?.coverUrl,
                        trackCount = if (detail.trackCount > 0) {
                            detail.trackCount
                        } else {
                            songs.size
                        },
                        creatorName = detail.creatorName,
                        creatorAvatarUrl = detail.creatorAvatarUrl,
                        description = detail.description,
                        tags = detail.tags,
                        songs = songs,
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
}
