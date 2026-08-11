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
import com.leo.lune.domain.repository.MusicRepository
import com.leo.lune.navigation.MusicRoute
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

// 歌单详情页 UI 状态
data class PlaylistDetailUiState(
    // 歌单元数据（封面、标题、创建者等）；未加载成功前为 null
    val playlist: PlaylistDetail? = null,
    // 歌单内全部歌曲
    val songs: List<Song> = emptyList(),
    // 是否正在拉取详情 / 曲目
    val isLoading: Boolean = false,
    // 加载失败时的错误信息
    val error: String? = null,
    // 是否正在播放，驱动主播放钮图标
    val isPlaying: Boolean = false,
    // 本页会话内是否已点过主播放钮；ViewModel 销毁后随状态重置
    val hasStartedPlayAll: Boolean = false
)

// 歌单详情页 ViewModel
// 负责按路由 playlistId 并行拉取元数据与曲目、同步播放状态；播放操作委托给全局播放器
@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    // 全局播放控制器，本页不直接持有 ExoPlayer
    private val playerController: MusicPlayerController,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 导航参数中的歌单 ID（MusicRoute.PlaylistDetail）
    private val playlistId: Long =
        savedStateHandle.toRoute<MusicRoute.PlaylistDetail>().playlistId

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    // 对外只读，PlaylistDetailScreen 通过 collect 订阅
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    // 当前进行中的加载任务，重试时可取消旧任务
    private var loadJob: Job? = null

    init {
        loadPlaylist()
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

    // 点击歌曲：以当前歌单列表为队列开始播放
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
        loadPlaylist()
    }

    // 并行拉取歌单详情与全部曲目；任一侧失败则整页错误
    private fun loadPlaylist() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                coroutineScope {
                    val detailDeferred = async {
                        musicRepository.getPlaylistDetail(playlistId)
                    }
                    val songsDeferred = async {
                        // limit = null：不截断，返回歌单内全部歌曲
                        musicRepository.getPlaylistSongs(playlistId, limit = null)
                    }
                    detailDeferred.await() to songsDeferred.await()
                }
            }.onSuccess { (detail, songs) ->
                _uiState.update {
                    it.copy(
                        playlist = detail,
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
