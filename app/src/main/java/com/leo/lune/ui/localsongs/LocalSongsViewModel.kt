package com.leo.lune.ui.localsongs

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.LocalLibraryItem
import com.leo.lune.domain.usecase.local.ObserveLocalLibraryUseCase
import com.leo.lune.navigation.MusicRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 「本地歌曲」页 UI 状态
data class LocalSongsUiState(
    // 本地曲库全量（下载 + 导入）
    val songs: List<LocalLibraryItem> = emptyList(),
    // 当前展示列表：确认搜索后主动算好；无关键词时等于 songs
    val filteredSongs: List<LocalLibraryItem> = emptyList(),
    // 入口 / 首曲封面；优先入口传入且不因列表刷新覆盖空白闪烁
    val coverUrl: String? = null,
    // 列表行总数（含多音质分行）；身份区优先用它
    val trackCount: Int = 0,
    // 搜索框当前输入（草稿，输入时不筛选）
    val query: String = "",
    // 用户确认搜索后的关键词；空则展示全量
    val activeKeyword: String = "",
    // 是否等待首批本地数据
    val isLoading: Boolean = true,
    // 是否正在播放，驱动身份区主播放钮图标
    val isPlaying: Boolean = false,
    // 当前播放歌曲 ID
    val currentSongId: Long? = null,
    // 本页会话内是否已点过主播放钮；ViewModel 销毁后随状态重置
    val hasStartedPlayAll: Boolean = false
)

// 「本地歌曲」页 ViewModel
// 订阅本地曲库、本地筛选、同步播放状态；播放操作委托给全局播放器
@HiltViewModel
class LocalSongsViewModel @Inject constructor(
    private val observeLocalLibrary: ObserveLocalLibraryUseCase,
    // 全局播放控制器，本页不直接持有 ExoPlayer
    private val playerController: MusicPlayerController,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 导航参数（MusicRoute.LocalSongs）
    private val route = savedStateHandle.toRoute<MusicRoute.LocalSongs>()

    private val _uiState = MutableStateFlow(
        LocalSongsUiState(
            // 进页即可展示首页带入的封面与曲数
            coverUrl = route.coverUrl.takeIf { it.isNotBlank() },
            trackCount = route.trackCount.coerceAtLeast(0)
        )
    )
    // 对外只读，LocalSongsScreen 通过 collect 订阅
    val uiState: StateFlow<LocalSongsUiState> = _uiState.asStateFlow()

    init {
        // 订阅本地曲库；下载/导入/删除后本页会自动刷新
        viewModelScope.launch {
            observeLocalLibrary().collect { songs ->
                _uiState.update { state ->
                    // 入口已有封面时保留，避免列表回来重载闪一下
                    val keepCover = state.coverUrl?.takeIf { it.isNotBlank() }
                    state.copy(
                        songs = songs,
                        // 列表刷新后按当前关键词重新筛一遍
                        filteredSongs = filterSongs(songs, state.activeKeyword),
                        coverUrl = keepCover ?: songs.firstOrNull()?.song?.coverUrl,
                        trackCount = songs.size,
                        isLoading = false,
                        // 列表被清空后重置「首次播放」；有歌时保留会话内状态
                        hasStartedPlayAll = if (songs.isEmpty()) false else state.hasStartedPlayAll
                    )
                }
            }
        }
        // 只取本页需要的播放字段，过滤无关更新
        viewModelScope.launch {
            playerController.playbackState
                .map { state ->
                    LocalSongsUiState(
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

    // 搜索框内容变化：只改草稿，不立刻筛选；清空时一并恢复全量列表
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

    // 点击某行：下载行按该行音质开播；导入行播文件 URI
    @RequiresApi(Build.VERSION_CODES.O)
    fun onSongClick(song: LocalLibraryItem) {
        val rows = _uiState.value.filteredSongs
        if (rows.isEmpty()) return
        val startIndex = rows.indexOfFirst { it.rowKey == song.rowKey }.coerceAtLeast(0)
        playerController.playSong(
            song = song.song,
            queue = rows.map { it.song },
            localQuality = song.bitrate?.let { DownloadQuality.fromBitrate(it) },
            startQueueIndex = startIndex
        )
    }

    // 身份区主播放钮：本页首次点击从首行连播，之后切换播停（至 ViewModel 销毁）
    @RequiresApi(Build.VERSION_CODES.O)
    fun onPlayAllClick() {
        val state = _uiState.value
        val rows = state.songs
        if (rows.isEmpty()) return
        if (state.hasStartedPlayAll) {
            playerController.togglePlayPause()
        } else {
            val first = rows.first()
            _uiState.update { it.copy(hasStartedPlayAll = true) }
            playerController.playSong(
                song = first.song,
                queue = rows.map { it.song },
                localQuality = first.bitrate?.let { DownloadQuality.fromBitrate(it) },
                startQueueIndex = 0
            )
        }
    }

    companion object {
        // 按关键词本地过滤（不区分大小写）；空关键词返回全量
        fun filterSongs(
            songs: List<LocalLibraryItem>,
            keyword: String
        ): List<LocalLibraryItem> {
            val key = keyword.trim()
            if (key.isEmpty()) return songs
            return songs.filter { item ->
                val song = item.song
                song.name.contains(key, ignoreCase = true) ||
                    song.artists.contains(key, ignoreCase = true) ||
                    song.album.contains(key, ignoreCase = true)
            }
        }
    }
}
