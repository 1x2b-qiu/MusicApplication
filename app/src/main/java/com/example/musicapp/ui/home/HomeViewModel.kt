package com.example.musicapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.controller.MusicPlayerController
import com.example.musicapp.domain.model.LoginState
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.usecase.GetLikedMusicPlaylistSongsUseCase
import com.example.musicapp.domain.usecase.ObserveLoginStateUseCase
import com.example.musicapp.domain.usecase.ObserveRecentPlayedSongsUseCase
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

// 首页 UI 状态
data class HomeUiState(
    // 「我喜欢的」歌曲列表
    val likedSongs: List<Song> = emptyList(),
    // 最近播放歌曲列表（来自 Room 本地记录）
    val recentSongs: List<Song> = emptyList(),
    // 是否正在加载「我喜欢的」等内容
    val isLoading: Boolean = false,
    // 加载失败时的错误信息
    val error: String? = null,
    // 登录状态，用于控制头像点击跳转登录
    val loginState: LoginState = LoginState(),
    // 顶栏展示的当前歌词行，由全局播放器同步
    val currentLyricLine: String = "听点音乐吧",
    // 是否正在播放，用于头像光晕和「正在播放」标签
    val isPlaying: Boolean = false,
    // 当前正在播放的歌曲 ID，用于「我喜欢的」播放按钮状态
    val currentSongId: Long? = null,
    // 迷你栏/顶栏是否有可展示的播放内容
    val hasPlaybackContent: Boolean = false
)

// 首页 ViewModel
// 负责加载「我喜欢的」、订阅本地最近播放与播放状态；登录信息进页时读一次
@HiltViewModel
class HomeViewModel @Inject constructor(
    // 拉取网易云「我喜欢的音乐」歌单
    private val getLikedMusicPlaylistSongsUseCase: GetLikedMusicPlaylistSongsUseCase,
    // 观察 Room 中的最近播放记录
    private val observeRecentPlayedSongsUseCase: ObserveRecentPlayedSongsUseCase,
    // 进页时读取一次当前登录状态
    private val observeLoginStateUseCase: ObserveLoginStateUseCase,
    // 全局播放控制器，首页不直接持有 ExoPlayer
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    // 对外只读的首页状态
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 当前进行中的「我喜欢的」加载任务，重试时可取消
    private var loadJob: Job? = null

    init {
        // 进页时只取一次登录态，再加载「我喜欢的」
        viewModelScope.launch {
            val loginState = observeLoginStateUseCase().first()
            _uiState.update { it.copy(loginState = loginState) }
            loadHomeContent()
        }
        // 只取本页需要的播放字段，过滤无关更新（含顶栏歌词）
        viewModelScope.launch {
            playerController.playbackState
                .map { state ->
                    HomeUiState(
                        currentLyricLine = state.currentLyricLine,
                        isPlaying = state.isPlaying,
                        currentSongId = state.currentSong?.id,
                        hasPlaybackContent = state.displaySong != null
                    )
                }
                .distinctUntilChanged()
                .collect { playback ->
                    _uiState.update {
                        it.copy(
                            currentLyricLine = playback.currentLyricLine,
                            isPlaying = playback.isPlaying,
                            currentSongId = playback.currentSongId,
                            hasPlaybackContent = playback.hasPlaybackContent
                        )
                    }
                }
        }
        // 订阅本地最近播放；播放器写入后首页会自动刷新
        viewModelScope.launch {
            observeRecentPlayedSongsUseCase(limit = RECENT_PLAY_LIMIT).collect { recentSongs ->
                _uiState.update { it.copy(recentSongs = recentSongs) }
            }
        }
    }

    // 加载失败后由 UI 触发重试
    fun onRetry() {
        loadHomeContent()
    }

    // 播放指定歌曲，并传入当前列表作为播放队列
    fun playSong(song: Song, queue: List<Song>) {
        playerController.playSong(song, queue)
    }

    // 播放/暂停切换，委托给全局播放器
    fun togglePlayPause() {
        playerController.togglePlayPause()
    }

    // 加载「我喜欢的」歌单；未登录时仅清空列表，不视为错误
    private fun loadHomeContent() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = _uiState.value.loginState.userId

            runCatching {
                val liked = if (userId != null) {
                    runCatching {
                        getLikedMusicPlaylistSongsUseCase(userId, limit = HOME_LIKED_SONGS_LIMIT)
                    }.getOrElse { emptyList() }
                } else {
                    emptyList()
                }
                liked
            }.onSuccess { liked ->
                // 有收藏歌曲时，设置迷你栏预览为第一首
                liked.firstOrNull()?.let(playerController::setPreviewSong)
                _uiState.update {
                    it.copy(
                        likedSongs = liked,
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "加载失败，请确认 API 服务已启动"
                    )
                }
            }
        }
    }
}

// 首页「最近播放」展示条数
private const val RECENT_PLAY_LIMIT = 20
// 首页「我喜欢的」轮播只拉取前 N 首，避免全量歌单拖慢首屏
private const val HOME_LIKED_SONGS_LIMIT = 30

// 将歌曲时长（毫秒）格式化为 mm:ss
fun formatSongDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
