package com.leo.lune.ui.radio

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.MusicRepository
import com.leo.lune.manager.FavoriteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 波段条目（UI）
data class RadioStationItem(
    val id: String,
    val title: String,
    val subtitle: String
)

// 电台页 UI 状态：与日推完全独立的队列
data class RadioUiState(
    val stations: List<RadioStationItem> = emptyList(),
    val selectedStationId: String = "",
    // 当前选中波段是否正在由播放器播放
    val isPlayingSelectedStation: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    // 主区展示曲（未开播时为波段预览下标对应曲）
    val songId: Long = 0L,
    val songTitle: String = "",
    val songArtist: String = "",
    val coverUrl: String = "",
    val isFavorite: Boolean = false,
    val stationTitle: String = "",
    val stationSubtitle: String = ""
)

// 电台页 ViewModel：多波段独立队列，进页不自动播
@HiltViewModel
class RadioViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playerController: MusicPlayerController,
    private val favoriteManager: FavoriteManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    // stationId → 完整歌曲队列
    private val stationQueues = mutableMapOf<String, List<Song>>()
    // 各波段未开播时的预览下标
    private val previewIndexByStation = mutableMapOf<String, Int>()
    // 当前电台播放标记（用于识别是否为本页队列）
    private var radioMarker: RadioMarker? = null

//    init {
//        _uiState.value = RadioUiState(
//            stations = FixedStations.map { it.toItem() },
//            selectedStationId = FixedStations.first().id,
//            stationTitle = FixedStations.first().title,
//            stationSubtitle = FixedStations.first().subtitle,
//            isLoading = true
//        )
//        loadAllStations()
//        viewModelScope.launch {
//            playerController.playbackState
//                .map { state ->
//                    PlaybackSnapshot(
//                        isPlaying = state.isPlaying,
//                        queueFirstId = state.queue.firstOrNull()?.id,
//                        currentSong = state.currentSong
//                    )
//                }
//                .distinctUntilChanged()
//                .collect { syncPlayback(it) }
//        }
//        viewModelScope.launch {
//            favoriteManager.results.collect {
//                val songId = _uiState.value.songId
//                if (songId != 0L) {
//                    _uiState.update {
//                        it.copy(isFavorite = favoriteManager.isFavoriteSong(songId))
//                    }
//                }
//            }
//        }
//    }

    // 选择波段：只换台，不自动播放
    fun onStationSelect(stationId: String) {
        if (stationId == _uiState.value.selectedStationId) return
        val spec = FixedStations.firstOrNull { it.id == stationId } ?: return
        previewIndexByStation.putIfAbsent(stationId, 0)
        _uiState.update {
            it.copy(
                selectedStationId = stationId,
                stationTitle = spec.title,
                stationSubtitle = spec.subtitle,
                error = null
            )
        }
        publishDisplayForSelected()
    }

    // 播放 / 暂停当前选中波段
    @RequiresApi(Build.VERSION_CODES.O)
    fun onPlayPauseClick() {
        val stationId = _uiState.value.selectedStationId
        val queue = stationQueues[stationId].orEmpty()
        if (queue.isEmpty()) return

        if (isActiveStation(stationId)) {
            playerController.togglePlayPause()
            return
        }

        val index = previewIndexByStation[stationId]
            ?.coerceIn(0, queue.lastIndex)
            ?: 0
        val song = queue[index]
        radioMarker = RadioMarker(
            stationId = stationId,
            firstSongId = queue.first().id,
            songIds = queue.map { it.id }.toSet()
        )
        playerController.playSong(song, queue)
    }

    // 上一首：播放中走播放器；未开播则只移动预览
    @RequiresApi(Build.VERSION_CODES.O)
    fun onSkipPrevious() {
        val stationId = _uiState.value.selectedStationId
        val queue = stationQueues[stationId].orEmpty()
        if (queue.isEmpty()) return

        if (isActiveStation(stationId)) {
            playerController.skipToPrevious()
            return
        }
        val current = previewIndexByStation[stationId] ?: 0
        val nextIndex = if (current <= 0) queue.lastIndex else current - 1
        previewIndexByStation[stationId] = nextIndex
        publishDisplayForSelected()
    }

    // 下一首：播放中走播放器；未开播则只移动预览
    @RequiresApi(Build.VERSION_CODES.O)
    fun onSkipNext() {
        val stationId = _uiState.value.selectedStationId
        val queue = stationQueues[stationId].orEmpty()
        if (queue.isEmpty()) return

        if (isActiveStation(stationId)) {
            playerController.skipToNext()
            return
        }
        val current = previewIndexByStation[stationId] ?: 0
        val nextIndex = (current + 1) % queue.size
        previewIndexByStation[stationId] = nextIndex
        publishDisplayForSelected()
    }

    // 红心当前展示曲
    fun onFavoriteClick() {
        val songId = _uiState.value.songId.takeIf { it != 0L } ?: return
        favoriteManager.toggleFavorite(songId)
        _uiState.update { it.copy(isFavorite = favoriteManager.isFavoriteSong(songId)) }
    }

    // 失败后重试拉取
    fun onRetry() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadAllStations()
    }

    private fun loadAllStations() {
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    FixedStations.map { spec ->
                        async {
                            val songs = when (val source = spec.source) {
                                RadioSongSource.Personalized ->
                                    musicRepository.getPersonalizedNewsongs(limit = StationQueueLimit)
                                is RadioSongSource.Playlist ->
                                    musicRepository.getPlaylistSongs(
                                        playlistId = source.playlistId,
                                        limit = StationQueueLimit
                                    )
                            }
                            spec.id to songs
                        }
                    }.awaitAll()
                }
            }.onSuccess { pairs ->
                stationQueues.clear()
                previewIndexByStation.clear()
                pairs.forEach { (id, songs) ->
                    stationQueues[id] = songs
                    previewIndexByStation[id] = 0
                }
                val anyLoaded = stationQueues.values.any { it.isNotEmpty() }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = if (anyLoaded) null else "电波暂时中断，请稍后重试"
                    )
                }
                publishDisplayForSelected()
            }.onFailure {
                stationQueues.clear()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "电波暂时中断，请稍后重试",
                        songId = 0L,
                        songTitle = "",
                        songArtist = "",
                        coverUrl = "",
                        isFavorite = false
                    )
                }
            }
        }
    }

    private fun syncPlayback(snapshot: PlaybackSnapshot) {
        val marker = radioMarker
        val activeStationId = when {
            marker == null -> null
            snapshot.queueFirstId != marker.firstSongId -> null
            snapshot.currentSong?.id !in marker.songIds -> null
            else -> marker.stationId
        }

        val selectedId = _uiState.value.selectedStationId
        val activeSelected = activeStationId == selectedId
        val playingSelected = activeSelected && snapshot.isPlaying

        if (activeSelected) {
            val current = snapshot.currentSong
            if (current != null) {
                val queue = stationQueues[selectedId].orEmpty()
                val index = queue.indexOfFirst { it.id == current.id }
                if (index >= 0) previewIndexByStation[selectedId] = index
                _uiState.update {
                    it.copy(
                        isPlayingSelectedStation = playingSelected,
                        songId = current.id,
                        songTitle = current.name,
                        songArtist = current.artists,
                        coverUrl = current.coverUrl.orEmpty(),
                        isFavorite = favoriteManager.isFavoriteSong(current.id)
                    )
                }
                return
            }
        }

        _uiState.update { it.copy(isPlayingSelectedStation = false) }
        if (!activeSelected) {
            publishDisplayForSelected()
        }
    }

    private fun publishDisplayForSelected() {
        val state = _uiState.value
        val stationId = state.selectedStationId
        val queue = stationQueues[stationId].orEmpty()
        if (queue.isEmpty()) {
            if (!state.isLoading) {
                _uiState.update {
                    it.copy(
                        songId = 0L,
                        songTitle = "",
                        songArtist = "",
                        coverUrl = "",
                        isFavorite = false,
                        isPlayingSelectedStation = false
                    )
                }
            }
            return
        }
        // 若该波段队列仍挂在播放器上，展示跟播放器走（由 syncPlayback 负责）
        if (isActiveStation(stationId)) return

        val index = (previewIndexByStation[stationId] ?: 0).coerceIn(0, queue.lastIndex)
        previewIndexByStation[stationId] = index
        val song = queue[index]
        _uiState.update {
            it.copy(
                songId = song.id,
                songTitle = song.name,
                songArtist = song.artists,
                coverUrl = song.coverUrl.orEmpty(),
                isFavorite = favoriteManager.isFavoriteSong(song.id),
                isPlayingSelectedStation = false
            )
        }
    }

    // 播放器队列是否仍是该波段（含暂停中）
    private fun isActiveStation(stationId: String): Boolean {
        val marker = radioMarker ?: return false
        if (marker.stationId != stationId) return false
        val state = playerController.playbackState.value
        val queueFirstId = state.queue.firstOrNull()?.id ?: return false
        val currentId = state.currentSong?.id ?: state.previewSong?.id ?: return false
        return queueFirstId == marker.firstSongId && currentId in marker.songIds
    }

    private data class RadioMarker(
        val stationId: String,
        val firstSongId: Long,
        val songIds: Set<Long>
    )

    private data class PlaybackSnapshot(
        val isPlaying: Boolean,
        val queueFirstId: Long?,
        val currentSong: Song?
    )
}

// 波段曲源：推荐新音乐或固定歌单
private sealed interface RadioSongSource {
    data object Personalized : RadioSongSource
    data class Playlist(val playlistId: Long) : RadioSongSource
}

private data class RadioStationSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val source: RadioSongSource
)

private fun RadioStationSpec.toItem() = RadioStationItem(
    id = id,
    title = title,
    subtitle = subtitle
)

// 每台预取曲目上限
private const val StationQueueLimit = 30

// 首版五个波段；封面与气质由队列首曲自然带出
private val FixedStations = listOf(
    RadioStationSpec(
        id = "night",
        title = "夜间电波",
        subtitle = "偏安静的夜色",
        source = RadioSongSource.Personalized
    ),
    RadioStationSpec(
        id = "morning",
        title = "晨间频率",
        subtitle = "轻快但不吵",
        source = RadioSongSource.Playlist(3779629)
    ),
    RadioStationSpec(
        id = "commute",
        title = "通勤轨道",
        subtitle = "节奏稳、好跟着走",
        source = RadioSongSource.Playlist(3778678)
    ),
    RadioStationSpec(
        id = "focus",
        title = "深潜专注",
        subtitle = "少打扰的持续声场",
        source = RadioSongSource.Playlist(6723173524)
    ),
    RadioStationSpec(
        id = "afterglow",
        title = "散场余温",
        subtitle = "慢一点的收尾",
        source = RadioSongSource.Playlist(6688069460)
    )
)
