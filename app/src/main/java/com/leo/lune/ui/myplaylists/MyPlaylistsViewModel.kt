package com.leo.lune.ui.myplaylists

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.domain.model.UserPlaylist
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

// 「创建的」Tab id
private const val CreatedPlaylistsCategoryId = "created"
// 「收藏的」Tab id
private const val SubscribedPlaylistsCategoryId = "subscribed"

// 「我的歌单」分类 Tab
data class MyPlaylistsCategory(
    // created / subscribed
    val id: String,
    // Tab 展示名
    val name: String
)

// 「我的歌单」网格条目
data class MyPlaylistsItem(
    val id: Long,
    val title: String,
    // 副标题，通常为「N 首」
    val subtitle: String,
    val coverUrl: String = "",
    // 入口预览曲数，进详情用
    val trackCount: Int = 0
)

// 「我的歌单」页 UI 状态
data class MyPlaylistsUiState(
    // Tab 顺序：收藏的在前，创建的在后
    val categories: List<MyPlaylistsCategory> = listOf(
        MyPlaylistsCategory(SubscribedPlaylistsCategoryId, "收藏的"),
        MyPlaylistsCategory(CreatedPlaylistsCategoryId, "创建的")
    ),
    // 当前选中的分类 id；默认收藏的
    val selectedCategoryId: String = SubscribedPlaylistsCategoryId,
    // 当前 Tab 下的歌单网格数据
    val playlists: List<MyPlaylistsItem> = emptyList(),
    // 是否正在拉取用户歌单
    val isLoading: Boolean = false,
    // 加载失败时的错误信息；有列表数据时可不展示
    val error: String? = null,
    // 当前正在播放的歌单 id；无则为 null，驱动封面播放钮图标
    val playingPlaylistId: Long? = null
)

// 「我的歌单」页 ViewModel
// 订阅登录态拉取用户歌单；Tab（收藏的 / 创建的）本地过滤；封面播放对齐歌单广场行为
@HiltViewModel
class MyPlaylistsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val authRepository: AuthRepository,
    // 全局播放控制器，本页不直接持有 ExoPlayer
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPlaylistsUiState(isLoading = true))
    // 对外只读，MyPlaylistsScreen 通过 collect 订阅
    val uiState: StateFlow<MyPlaylistsUiState> = _uiState.asStateFlow()

    // 当前进行中的加载任务；重试 / 换账号时取消旧任务
    private var loadJob: Job? = null
    // 当前登录用户 id；重试时使用
    private var cachedUserId: Long? = null
    // 缓存全量用户歌单，切 Tab 只做本地过滤，不重复请求
    private var allPlaylists: List<UserPlaylist> = emptyList()
    // 歌单播放标记：用于判断当前播放是否来自本页某个歌单
    private val playlistMarkers = mutableMapOf<Long, MyPlaylistMarker>()

    init {
        // 持续观察登录态：登录 / 登出 / 会话恢复后刷新列表
        viewModelScope.launch {
            authRepository.observeLoginState().collect { loginState ->
                val userId = loginState.userId
                cachedUserId = userId
                if (userId == null) {
                    // 未登录：清空列表，不算错误
                    allPlaylists = emptyList()
                    _uiState.update {
                        it.copy(
                            playlists = emptyList(),
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    loadPlaylists(userId)
                }
            }
        }
        // 只取本页需要的播放字段，过滤无关更新，驱动封面播停图标
        viewModelScope.launch {
            playerController.playbackState
                .map { state ->
                    Triple(
                        state.isPlaying,
                        state.queue.firstOrNull()?.id,
                        state.currentSong?.id
                    )
                }
                .distinctUntilChanged()
                .collect { (isPlaying, queueFirstId, currentSongId) ->
                    _uiState.update {
                        it.copy(
                            playingPlaylistId = resolvePlayingPlaylistId(
                                isPlaying, queueFirstId, currentSongId
                            )
                        )
                    }
                }
        }
    }

    // 切换「收藏的 / 创建的」：本地过滤，不重新请求
    fun onCategorySelect(categoryId: String) {
        if (categoryId == _uiState.value.selectedCategoryId) return
        _uiState.update {
            it.copy(
                selectedCategoryId = categoryId,
                playlists = filterForCategory(allPlaylists, categoryId),
                error = null
            )
        }
    }

    // 加载失败后由 UI 触发重试
    fun onRetry() {
        val userId = cachedUserId ?: return
        loadPlaylists(userId)
    }

    // 封面播放钮：拉取歌单曲目并播放；若正在播该歌单则暂停
    @RequiresApi(Build.VERSION_CODES.O)
    fun onPlaylistPlayClick(playlistId: Long) {
        val playback = playerController.playbackState.value
        val isPlayingThis = _uiState.value.playingPlaylistId == playlistId &&
            playback.isPlaying
        if (isPlayingThis) {
            playerController.togglePlayPause()
            return
        }
        viewModelScope.launch {
            runCatching { musicRepository.getPlaylistSongs(playlistId, limit = null) }
                .onSuccess { songs ->
                    if (songs.isEmpty()) return@onSuccess
                    playlistMarkers[playlistId] = MyPlaylistMarker(
                        firstSongId = songs.first().id,
                        songIds = songs.map { it.id }.toSet()
                    )
                    playerController.playSong(songs.first(), songs)
                }
        }
    }

    // 拉取用户全部歌单，再按当前 Tab 过滤展示
    private fun loadPlaylists(userId: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                musicRepository.getUserPlaylists(userId)
            }.onSuccess { playlists ->
                allPlaylists = playlists
                val categoryId = _uiState.value.selectedCategoryId
                _uiState.update {
                    it.copy(
                        playlists = filterForCategory(playlists, categoryId),
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { throwable ->
                allPlaylists = emptyList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        playlists = emptyList(),
                        error = throwable.message ?: "加载失败，请稍后重试"
                    )
                }
            }
        }
    }

    // 返回当前正在播放的本页歌单 id；无则为 null
    private fun resolvePlayingPlaylistId(
        isPlaying: Boolean,
        queueFirstId: Long?,
        currentSongId: Long?
    ): Long? {
        if (!isPlaying || currentSongId == null || queueFirstId == null) return null
        return playlistMarkers.entries.firstOrNull { (_, marker) ->
            marker.firstSongId == queueFirstId && currentSongId in marker.songIds
        }?.key
    }

    companion object {
        // 收藏的：他人歌单；创建的：自建且排除「我喜欢的音乐」（首页已有入口）
        fun filterForCategory(
            playlists: List<UserPlaylist>,
            categoryId: String
        ): List<MyPlaylistsItem> {
            val filtered = when (categoryId) {
                SubscribedPlaylistsCategoryId ->
                    playlists.filter { !it.isCreatedByUser }
                else ->
                    playlists.filter { it.isCreatedByUser && !it.isLikedMusicPlaylist }
            }
            return filtered.map { it.toMyPlaylistsItem() }
        }
    }
}

// 本页歌单播放标记：首曲 id + 队列歌曲集合，用于匹配全局播放状态
private data class MyPlaylistMarker(
    val firstSongId: Long,
    val songIds: Set<Long>
)

// 领域歌单 → 本页网格 UI 模型
private fun UserPlaylist.toMyPlaylistsItem(): MyPlaylistsItem = MyPlaylistsItem(
    id = id,
    title = name,
    subtitle = if (trackCount > 0) "$trackCount 首" else "",
    coverUrl = coverUrl.orEmpty(),
    trackCount = trackCount
)
