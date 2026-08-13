package com.leo.lune.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.DownloadedSong
import com.leo.lune.domain.model.LoginState
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.model.UserPlaylist
import com.leo.lune.domain.repository.AuthRepository
import com.leo.lune.domain.repository.DownloadRepository
import com.leo.lune.domain.repository.MusicRepository
import com.leo.lune.domain.repository.PlayHistoryRepository
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

// 首页「我的歌单」横滑条目（对齐曲库甄选歌单卡片字段）
data class HomeMyPlaylistItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val trackCount: Int,
    val coverUrl: String
)

// 首页 UI 状态
data class HomeUiState(
    // 「我喜欢的」歌曲列表
    val likedSongs: List<Song> = emptyList(),
    // 「我喜欢的」歌单曲目总数（来自 user playlist.trackCount）
    val likedTrackCount: Int = 0,
    // 最近播放歌曲列表（来自 Room 本地记录）
    val recentSongs: List<Song> = emptyList(),
    // 本地下载歌曲（同曲多音质分行，首页横滑截断）
    val localSongs: List<DownloadedSong> = emptyList(),
    // 本地下载行总数（含多音质分行，用于「全部」入口）
    val localTrackCount: Int = 0,
    // 「我的歌单」横滑预览：收藏在前、创建在后（已排除「我喜欢的音乐」）
    val myPlaylists: List<HomeMyPlaylistItem> = emptyList(),
    // 自己创建的歌单（含「我喜欢的音乐」、年度歌单等）
    val createdPlaylists: List<UserPlaylist> = emptyList(),
    // 收藏的他人歌单
    val subscribedPlaylists: List<UserPlaylist> = emptyList(),
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
    val hasPlaybackContent: Boolean = false,
    // 当前正在播放的「我的歌单」id；无则为 null，驱动封面播放钮
    val playingMyPlaylistId: Long? = null
)

// 首页 ViewModel
// 负责加载「我喜欢的」、用户歌单、订阅本地最近播放 / 本地下载与播放状态
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playHistoryRepository: PlayHistoryRepository,
    private val downloadRepository: DownloadRepository,
    private val authRepository: AuthRepository,
    // 全局播放控制器，首页不直接持有 ExoPlayer
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    // 对外只读的首页状态
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 当前进行中的「我喜欢的」加载任务，重试时可取消
    private var loadJob: Job? = null
    // 「我的歌单」播放标记：首曲 id + 队列集合，用于匹配全局播放状态
    private val myPlaylistMarkers = mutableMapOf<Long, HomeMyPlaylistMarker>()

    init {
        // 持续观察登录态：会话恢复 / 登录 / 登出后自动刷新「我喜欢的」
        viewModelScope.launch {
            authRepository.observeLoginState().collect { loginState ->
                _uiState.update { it.copy(loginState = loginState) }
                loadHomeContent()
            }
        }
        // 只取本页需要的播放字段，过滤无关更新（含顶栏歌词 / 我的歌单播停）
        viewModelScope.launch {
            playerController.playbackState
                .map { state ->
                    HomePlaybackSlice(
                        currentLyricLine = state.currentLyricLine,
                        isPlaying = state.isPlaying,
                        currentSongId = state.currentSong?.id,
                        hasPlaybackContent = state.displaySong != null,
                        queueFirstId = state.queue.firstOrNull()?.id
                    )
                }
                .distinctUntilChanged()
                .collect { playback ->
                    _uiState.update {
                        it.copy(
                            currentLyricLine = playback.currentLyricLine,
                            isPlaying = playback.isPlaying,
                            currentSongId = playback.currentSongId,
                            hasPlaybackContent = playback.hasPlaybackContent,
                            playingMyPlaylistId = resolvePlayingMyPlaylistId(
                                playback.isPlaying,
                                playback.queueFirstId,
                                playback.currentSongId
                            )
                        )
                    }
                }
        }
        // 订阅本地最近播放；播放器写入后首页会自动刷新
        viewModelScope.launch {
            playHistoryRepository.observeRecentPlays(limit = RECENT_PLAY_LIMIT).collect { recentSongs ->
                _uiState.update { it.copy(recentSongs = recentSongs) }
            }
        }
        // 订阅本地下载；同曲多音质各占一行，不去重
        viewModelScope.launch {
            downloadRepository.observeDownloadedSongs().collect { downloaded ->
                _uiState.update {
                    it.copy(
                        localSongs = downloaded.take(HOME_LOCAL_SONGS_LIMIT),
                        localTrackCount = downloaded.size
                    )
                }
            }
        }
    }

    // 加载失败后由 UI 触发重试
    fun onRetry() {
        loadHomeContent()
    }

    // 播放指定歌曲，并传入当前列表作为播放队列
    @RequiresApi(Build.VERSION_CODES.O)
    fun playSong(song: Song, queue: List<Song>) {
        playerController.playSong(song, queue)
    }

    // 播放本地下载行：用该行音质；队列与横滑列表一致（同曲多音质不去重）
    @RequiresApi(Build.VERSION_CODES.O)
    fun playLocalSong(song: DownloadedSong, queue: List<DownloadedSong>) {
        val startIndex = queue.indexOfFirst {
            it.songId == song.songId && it.bitrate == song.bitrate
        }.coerceAtLeast(0)
        playerController.playSong(
            song = song.toSong(),
            queue = queue.map { it.toSong() },
            localQuality = DownloadQuality.fromBitrate(song.bitrate),
            startQueueIndex = startIndex
        )
    }

    // 播放/暂停切换，委托给全局播放器
    @RequiresApi(Build.VERSION_CODES.O)
    fun togglePlayPause() {
        playerController.togglePlayPause()
    }

    // 「我的歌单」封面播放钮：拉取曲目并播放；若正在播该歌单则暂停
    @RequiresApi(Build.VERSION_CODES.O)
    fun onMyPlaylistPlayClick(playlistId: Long) {
        val playback = playerController.playbackState.value
        val isPlayingThis = _uiState.value.playingMyPlaylistId == playlistId &&
            playback.isPlaying
        if (isPlayingThis) {
            playerController.togglePlayPause()
            return
        }
        viewModelScope.launch {
            runCatching { musicRepository.getPlaylistSongs(playlistId, limit = null) }
                .onSuccess { songs ->
                    if (songs.isEmpty()) return@onSuccess
                    myPlaylistMarkers[playlistId] = HomeMyPlaylistMarker(
                        firstSongId = songs.first().id,
                        songIds = songs.map { it.id }.toSet()
                    )
                    playerController.playSong(songs.first(), songs)
                }
        }
    }

    // 加载「我喜欢的」与用户歌单；未登录时仅清空列表，不视为错误
    private fun loadHomeContent() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = _uiState.value.loginState.userId

            runCatching {
                if (userId == null) {
                    return@runCatching HomeLoadedContent()
                }
                val playlists = runCatching {
                    musicRepository.getUserPlaylists(userId)
                }.getOrElse { emptyList() }
                val likedPlaylist = playlists.firstOrNull { it.isLikedMusicPlaylist }
                val likedSongs = if (likedPlaylist != null) {
                    runCatching {
                        musicRepository.getPlaylistSongs(
                            likedPlaylist.id,
                            limit = HOME_LIKED_SONGS_LIMIT
                        )
                    }.getOrElse { emptyList() }
                } else {
                    emptyList()
                }
                val created = playlists.filter { it.isCreatedByUser }
                val subscribed = playlists.filter { !it.isCreatedByUser }
                HomeLoadedContent(
                    likedSongs = likedSongs,
                    likedTrackCount = likedPlaylist?.trackCount?.coerceAtLeast(0) ?: 0,
                    createdPlaylists = created,
                    subscribedPlaylists = subscribed,
                    // 横滑预览：收藏在前、创建在后（排除喜欢歌单）
                    myPlaylists = buildHomeMyPlaylists(subscribed, created)
                )
            }.onSuccess { content ->
                _uiState.update {
                    it.copy(
                        likedSongs = content.likedSongs,
                        likedTrackCount = content.likedTrackCount,
                        createdPlaylists = content.createdPlaylists,
                        subscribedPlaylists = content.subscribedPlaylists,
                        myPlaylists = content.myPlaylists,
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

    // 返回当前正在播放的「我的歌单」id；无则为 null
    private fun resolvePlayingMyPlaylistId(
        isPlaying: Boolean,
        queueFirstId: Long?,
        currentSongId: Long?
    ): Long? {
        if (!isPlaying || currentSongId == null || queueFirstId == null) return null
        return myPlaylistMarkers.entries.firstOrNull { (_, marker) ->
            marker.firstSongId == queueFirstId && currentSongId in marker.songIds
        }?.key
    }
}

// 首页一次加载得到的远端内容
private data class HomeLoadedContent(
    val likedSongs: List<Song> = emptyList(),
    val likedTrackCount: Int = 0,
    val createdPlaylists: List<UserPlaylist> = emptyList(),
    val subscribedPlaylists: List<UserPlaylist> = emptyList(),
    val myPlaylists: List<HomeMyPlaylistItem> = emptyList()
)

// 播放状态切片：仅首页关心的字段，便于 distinctUntilChanged
private data class HomePlaybackSlice(
    val currentLyricLine: String,
    val isPlaying: Boolean,
    val currentSongId: Long?,
    val hasPlaybackContent: Boolean,
    val queueFirstId: Long?
)

// 「我的歌单」播放标记
private data class HomeMyPlaylistMarker(
    val firstSongId: Long,
    val songIds: Set<Long>
)

// 组装首页横滑歌单：收藏 → 创建（去掉喜欢），再截断
private fun buildHomeMyPlaylists(
    subscribed: List<UserPlaylist>,
    created: List<UserPlaylist>
): List<HomeMyPlaylistItem> {
    return (subscribed + created.filter { !it.isLikedMusicPlaylist })
        .take(HOME_MY_PLAYLISTS_LIMIT)
        .map { it.toHomeMyPlaylistItem() }
}

private fun UserPlaylist.toHomeMyPlaylistItem(): HomeMyPlaylistItem = HomeMyPlaylistItem(
    id = id,
    title = name,
    subtitle = if (trackCount > 0) "$trackCount 首" else "",
    trackCount = trackCount.coerceAtLeast(0),
    coverUrl = coverUrl.orEmpty()
)

// 首页「最近播放」展示条数
private const val RECENT_PLAY_LIMIT = 20
// 首页「本地歌曲」横滑条数
private const val HOME_LOCAL_SONGS_LIMIT = 20
// 首页「我的歌单」横滑条数
private const val HOME_MY_PLAYLISTS_LIMIT = 20
// 首页「我喜欢的」轮播只拉取前 N 首，避免全量歌单拖慢首屏
private const val HOME_LIKED_SONGS_LIMIT = 20

// 将歌曲时长（毫秒）格式化为 mm:ss
fun formatSongDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
