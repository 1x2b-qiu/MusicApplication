package com.example.musicapp.ui.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.controller.player.MusicPlayerController
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.usecase.ObservePlayStatsUseCase
import com.example.musicapp.domain.usecase.ObserveRecentPlayedSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 「最近播放」页 UI 状态
data class RecentUiState(
    // 本地最近播放全量列表（受 Room 保留上限约束）
    val songs: List<Song> = emptyList(),
    // 当前展示列表：确认搜索后主动算好；无关键词时等于 songs
    val filteredSongs: List<Song> = emptyList(),
    // 搜索框当前输入（草稿，输入时不筛选）
    val query: String = "",
    // 用户确认搜索后的关键词；空则展示全量
    val activeKeyword: String = "",
    // 是否等待首批本地数据
    val isLoading: Boolean = true,
    // 是否正在播放，驱动身份区主播放钮图标
    val isPlaying: Boolean = false,
    // 当前播放歌曲 ID，用于判断是否仍在最近播放列表内连播
    val currentSongId: Long? = null,
    // 本周已播放次数（不去重，来自 play_stats）
    val weekPlayedCount: Int = 0,
    // 累计实际听歌小时（由总毫秒换算）
    val totalPlayHours: Int = 0,
    // 累计实际听歌分钟（不足一小时的部分）
    val totalPlayMinutes: Int = 0
)

// 「最近播放」页 ViewModel
// 订阅本地最近播放与播放统计、本地筛选、同步播放状态；播放操作委托给全局播放器
@HiltViewModel
class RecentViewModel @Inject constructor(
    // 观察 Room 中的最近播放列表
    private val observeRecentPlayedSongsUseCase: ObserveRecentPlayedSongsUseCase,
    // 观察本周次数与累计听歌时长
    private val observePlayStatsUseCase: ObservePlayStatsUseCase,
    // 全局播放控制器，本页不直接持有 ExoPlayer
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecentUiState())
    // 对外只读，RecentScreen 通过 collect 订阅
    val uiState: StateFlow<RecentUiState> = _uiState.asStateFlow()

    init {
        // 订阅最近播放列表；播放器写入后本页会自动刷新
        viewModelScope.launch {
            // 与本地保留上限一致，作为本页「全量」
            observeRecentPlayedSongsUseCase(limit = RECENT_FULL_LIMIT).collect { songs ->
                _uiState.update { state ->
                    state.copy(
                        songs = songs,
                        // 列表刷新后按当前关键词重新筛一遍
                        filteredSongs = filterSongs(songs, state.activeKeyword),
                        isLoading = false
                    )
                }
            }
        }
        // 订阅播放统计，驱动身份区「本周已播放 / 累计播放」文案
        viewModelScope.launch {
            observePlayStatsUseCase().collect { stats ->
                // 总毫秒 → 整分钟，再拆成小时与剩余分钟
                val totalMinutes = (stats.totalListenDurationMs / 60_000L).toInt()
                _uiState.update {
                    it.copy(
                        weekPlayedCount = stats.weekPlayCount,
                        totalPlayHours = totalMinutes / 60,
                        totalPlayMinutes = totalMinutes % 60
                    )
                }
            }
        }
        // 订阅全局播放器状态，驱动主播放钮与当前曲高亮逻辑
        viewModelScope.launch {
            playerController.playbackState.collect { playbackState ->
                _uiState.update {
                    it.copy(
                        isPlaying = playbackState.isPlaying,
                        currentSongId = playbackState.currentSong?.id
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

    // 点击歌曲：以当前筛选结果为队列开始播放
    fun onSongClick(song: Song) {
        val queue = _uiState.value.filteredSongs
        if (queue.isEmpty()) return
        playerController.playSong(song, queue)
    }

    // 身份区主播放钮：当前曲在最近播放列表中则切换播停，否则从列表首曲连播全量
    fun onPlayAllClick() {
        val state = _uiState.value
        val queue = state.songs
        if (queue.isEmpty()) return
        val currentId = state.currentSongId
        val isPlayingRecent = currentId != null && queue.any { it.id == currentId }
        if (isPlayingRecent) {
            playerController.togglePlayPause()
        } else {
            playerController.playSong(queue.first(), queue)
        }
    }

    companion object {
        // 按关键词本地过滤最近播放列表（不区分大小写）；空关键词返回全量
        fun filterSongs(songs: List<Song>, keyword: String): List<Song> {
            val key = keyword.trim()
            if (key.isEmpty()) return songs
            return songs.filter { song ->
                song.name.contains(key, ignoreCase = true) ||
                    song.artists.contains(key, ignoreCase = true) ||
                    song.album.contains(key, ignoreCase = true)
            }
        }
    }
}

// 与 PlayHistoryRepositoryImpl 本地保留上限一致
private const val RECENT_FULL_LIMIT = 50
