package com.example.musicapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.controller.player.MusicPlayerController
import com.example.musicapp.domain.model.LoginState
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.usecase.GetLikedMusicPlaylistSongsUseCase
import com.example.musicapp.domain.usecase.ObserveLoginStateUseCase
import com.example.musicapp.domain.usecase.ObserveRecentPlayedSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
// 负责加载「我喜欢的」、订阅本地最近播放，同步登录与播放状态，播放操作委托给全局播放器
@HiltViewModel
class HomeViewModel @Inject constructor(
    // 拉取网易云「我喜欢的音乐」歌单
    private val getLikedMusicPlaylistSongsUseCase: GetLikedMusicPlaylistSongsUseCase,
    // 观察 Room 中的最近播放记录
    private val observeRecentPlayedSongsUseCase: ObserveRecentPlayedSongsUseCase,
    // 观察 DataStore 中的登录状态
    private val observeLoginStateUseCase: ObserveLoginStateUseCase,
    // 全局播放控制器，首页不直接持有 ExoPlayer
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    // 对外只读的首页状态
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 当前进行中的「我喜欢的」加载任务，切换账号时可取消
    private var loadJob: Job? = null

    init {
        // 登录状态变化时更新 UI；用户切换后重新加载「我喜欢的」
        viewModelScope.launch {
            observeLoginStateUseCase().collect { loginState ->
                val previousUserId = _uiState.value.loginState.userId
                _uiState.update { it.copy(loginState = loginState) }
                if (loginState.userId != previousUserId) {
                    // 错峰：先让 Room 本地数据（最近播放）加载展示，网络请求延后
                    delay(NETWORK_LOAD_DELAY_MS)
                    loadHomeContent()
                }
            }
        }
        // 订阅全局播放器状态，驱动顶栏歌词与播放指示
        viewModelScope.launch {
            playerController.playbackState.collect { playbackState ->
                _uiState.update {
                    it.copy(
                        currentLyricLine = playbackState.currentLyricLine,
                        isPlaying = playbackState.isPlaying,
                        currentSongId = playbackState.currentSong?.id,
                        hasPlaybackContent = playbackState.displaySong != null
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
// 网络请求延后毫秒数，让 Room 本地数据先加载展示
private const val NETWORK_LOAD_DELAY_MS = 300L

// 将歌曲时长（毫秒）格式化为 mm:ss
fun formatSongDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
