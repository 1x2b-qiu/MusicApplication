package com.leo.lune.ui.library

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.controller.PlaybackState
import com.leo.lune.domain.model.PersonalizedPlaylist
import com.leo.lune.domain.model.PlaylistGenre
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.AuthRepository
import com.leo.lune.domain.repository.MusicRepository
import com.leo.lune.ui.home.formatSongDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 每日推荐 / 猜你喜欢单曲（UI 展示用）
data class DailyRecommendSongItem(
    val id: Long,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val duration: String,
    val hot: Boolean = false
)

// 甄选歌单条目
data class FeaturedPlaylistItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val trackCount: Int,
    val coverUrl: String
)

// 排行榜内单曲预览
data class ChartSongPreview(
    val id: Long,
    val title: String,
    val artist: String,
    val coverUrl: String
)

// 排行榜卡片；封面由 songs 首曲提供
data class ChartItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val songs: List<ChartSongPreview>
)

// 风格分类格子
data class GenreItem(
    val id: Long,
    val name: String,
    val coverUrl: String
)

// 曲库页 UI 状态：曲库各区块均为真实数据
data class LibraryUiState(
    // 每日推荐（Banner）
    val dailyRecommendSongs: List<DailyRecommendSongItem> = emptyList(),
    // 猜你喜欢（推荐新音乐）
    val guessYouLikeSongs: List<DailyRecommendSongItem> = emptyList(),
    // 甄选歌单（推荐歌单）
    val featuredPlaylists: List<FeaturedPlaylistItem> = emptyList(),
    // 每日推荐 Banner 播放按钮是否处于「播放中」态
    val isDailyRecommendPlaying: Boolean = false,
    // 当前正在播放的甄选歌单 id，null 表示无
    val playingFeaturedPlaylistId: Long? = null,
    val charts: List<ChartItem> = emptyList(),
    val genres: List<GenreItem> = emptyList()
)

// 曲库页 ViewModel：曲库各区块走网易云接口
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val authRepository: AuthRepository,
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(sampleLibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    // 每日推荐原始队列，供播放器用
    private var dailyRecommendQueue: List<Song> = emptyList()
    // 猜你喜欢原始队列，供播放器用
    private var guessYouLikeQueue: List<Song> = emptyList()
    // 甄选歌单播放标记：用于判断当前播放是否来自某个甄选歌单
    private val featuredPlaylistMarkers = mutableMapOf<Long, FeaturedPlaylistMarker>()
    // 排行榜预览歌曲队列：chartId → 前三首
    private val chartQueues = mutableMapOf<Long, List<Song>>()

    init {
        // 排行榜 / 风格分类无需登录，进页即拉取
        loadCharts()
        loadGenres()
        viewModelScope.launch {
            authRepository.observeLoginState().collect { loginState ->
                if (loginState.isLoggedIn) {
                    loadDailyRecommend()
                    loadGuessYouLike()
                    loadFeaturedPlaylists()
                } else {
                    dailyRecommendQueue = emptyList()
                    guessYouLikeQueue = emptyList()
                    featuredPlaylistMarkers.clear()
                    _uiState.update {
                        it.copy(
                            dailyRecommendSongs = emptyList(),
                            guessYouLikeSongs = emptyList(),
                            featuredPlaylists = emptyList(),
                            isDailyRecommendPlaying = false,
                            playingFeaturedPlaylistId = null
                        )
                    }
                }
            }
        }
        // 同步每日推荐与甄选歌单播放按钮状态
        viewModelScope.launch {
            playerController.playbackState
                .map { state ->
                    Pair(
                        isPlayingDailyRecommend(state),
                        resolvePlayingFeaturedPlaylistId(state)
                    )
                }
                .distinctUntilChanged()
                .collect { (isDailyPlaying, playingFeaturedId) ->
                    _uiState.update {
                        it.copy(
                            isDailyRecommendPlaying = isDailyPlaying,
                            playingFeaturedPlaylistId = playingFeaturedId
                        )
                    }
                }
        }
    }

    // 拉取每日推荐并映射到 Banner UI
    private fun loadDailyRecommend() {
        viewModelScope.launch {
            runCatching { musicRepository.getDailyRecommendSongs() }
                .onSuccess { songs ->
                    dailyRecommendQueue = songs
                    _uiState.update {
                        it.copy(
                            dailyRecommendSongs = songs.map { song -> song.toDailyRecommendItem() },
                            isDailyRecommendPlaying = isPlayingDailyRecommend(
                                playerController.playbackState.value
                            )
                        )
                    }
                }
                .onFailure {
                    dailyRecommendQueue = emptyList()
                    _uiState.update { it.copy(dailyRecommendSongs = emptyList()) }
                }
        }
    }

    // 拉取推荐新音乐并映射到猜你喜欢 UI（固定最多 15 首）
    private fun loadGuessYouLike() {
        viewModelScope.launch {
            runCatching { musicRepository.getPersonalizedNewsongs(limit = 15) }
                .onSuccess { songs ->
                    guessYouLikeQueue = songs
                    _uiState.update {
                        it.copy(guessYouLikeSongs = songs.map { song -> song.toDailyRecommendItem() })
                    }
                }
                .onFailure {
                    guessYouLikeQueue = emptyList()
                    _uiState.update { it.copy(guessYouLikeSongs = emptyList()) }
                }
        }
    }

    // 拉取推荐歌单并映射到甄选歌单 UI
    private fun loadFeaturedPlaylists() {
        viewModelScope.launch {
            runCatching { musicRepository.getPersonalizedPlaylists(limit = 10) }
                .onSuccess { playlists ->
                    _uiState.update {
                        it.copy(featuredPlaylists = playlists.map { playlist -> playlist.toFeaturedItem() })
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(featuredPlaylists = emptyList()) }
                }
        }
    }

    // 拉取固定四个榜单前 3 首预览（复用 playlist/track/all）
    private fun loadCharts() {
        viewModelScope.launch {
            val nextQueues = mutableMapOf<Long, List<Song>>()
            val charts = FixedCharts.map { spec ->
                val songs = runCatching {
                    musicRepository.getPlaylistSongs(spec.id, limit = 3)
                }.getOrElse { emptyList() }
                nextQueues[spec.id] = songs
                ChartItem(
                    id = spec.id,
                    title = spec.title,
                    subtitle = spec.subtitle,
                    songs = songs.map { song ->
                        ChartSongPreview(
                            id = song.id,
                            title = song.name,
                            artist = song.artists,
                            coverUrl = song.coverUrl.orEmpty()
                        )
                    }
                )
            }
            chartQueues.clear()
            chartQueues.putAll(nextQueues)
            _uiState.update { it.copy(charts = charts) }
        }
    }

    // 拉取热门风格分类（含封面）
    private fun loadGenres() {
        viewModelScope.launch {
            runCatching { musicRepository.getHotPlaylistGenres() }
                .onSuccess { genres ->
                    _uiState.update {
                        it.copy(genres = genres.map { genre -> genre.toGenreItem() })
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(genres = emptyList()) }
                }
        }
    }

    // 猜你喜欢：播放指定歌曲（队列为当前推荐列表）
    @RequiresApi(Build.VERSION_CODES.O)
    fun onGuessYouLikePlay(songId: Long) {
        val queue = guessYouLikeQueue
        val song = queue.firstOrNull { it.id == songId } ?: return
        playerController.playSong(song, queue)
    }

    // 每日推荐：播放全部 / 播放中则暂停
    @RequiresApi(Build.VERSION_CODES.O)
    fun onDailyRecommendPlayClick() {
        val queue = dailyRecommendQueue
        if (queue.isEmpty()) return
        if (_uiState.value.isDailyRecommendPlaying) {
            playerController.togglePlayPause()
        } else {
            playerController.playSong(queue.first(), queue)
        }
    }

    // 甄选歌单：仅播放按钮触发；拉取歌曲并播放 / 播放中则暂停
    @RequiresApi(Build.VERSION_CODES.O)
    fun onPlaylistPlayClick(playlistId: Long) {
        val playback = playerController.playbackState.value
        val isPlayingThis = _uiState.value.playingFeaturedPlaylistId == playlistId &&
            playback.isPlaying
        if (isPlayingThis) {
            playerController.togglePlayPause()
            return
        }

        viewModelScope.launch {
            runCatching { musicRepository.getPlaylistSongs(playlistId) }
                .onSuccess { songs ->
                    if (songs.isEmpty()) return@onSuccess
                    featuredPlaylistMarkers[playlistId] = FeaturedPlaylistMarker(
                        firstSongId = songs.first().id,
                        songIds = songs.map { it.id }.toSet()
                    )
                    playerController.playSong(songs.first(), songs)
                }
        }
    }

    // 甄选歌单：进入详情（暂留空）
    fun onPlaylistClick(playlistId: Long) = Unit

    // 排行榜：点击前三首中的某一首，以该榜预览列表为队列播放
    @RequiresApi(Build.VERSION_CODES.O)
    fun onChartSongClick(chartId: Long, songId: Long) {
        val queue = chartQueues[chartId].orEmpty()
        val song = queue.firstOrNull { it.id == songId } ?: return
        playerController.playSong(song, queue)
    }

    // 排行榜：进入详情（暂留空）
    fun onChartClick(chartId: Long) = Unit

    fun onChartsAllClick() = Unit

    fun onGenreClick(genreId: Long) = Unit

    // 当前是否正在播放每日推荐队列
    private fun isPlayingDailyRecommend(state: PlaybackState): Boolean {
        val queue = dailyRecommendQueue
        if (queue.isEmpty() || !state.isPlaying) return false
        val dailyFirstId = queue.first().id
        val currentSongId = state.currentSong?.id ?: return false
        return state.queue.firstOrNull()?.id == dailyFirstId &&
            queue.any { it.id == currentSongId }
    }

    // 返回当前正在播放的甄选歌单 id；无则为 null
    private fun resolvePlayingFeaturedPlaylistId(state: PlaybackState): Long? {
        if (!state.isPlaying) return null
        val currentSongId = state.currentSong?.id ?: return null
        val queueFirstId = state.queue.firstOrNull()?.id ?: return null
        return featuredPlaylistMarkers.entries.firstOrNull { (_, marker) ->
            marker.firstSongId == queueFirstId && currentSongId in marker.songIds
        }?.key
    }
}

private data class FeaturedPlaylistMarker(
    val firstSongId: Long,
    val songIds: Set<Long>
)

private fun Song.toDailyRecommendItem(): DailyRecommendSongItem = DailyRecommendSongItem(
    id = id,
    title = name,
    artist = artists,
    coverUrl = coverUrl.orEmpty(),
    duration = formatSongDuration(durationMs)
)

private fun PersonalizedPlaylist.toFeaturedItem(): FeaturedPlaylistItem =
    FeaturedPlaylistItem(
        id = id,
        title = name,
        subtitle = copywriter ?: "$trackCount 首",
        trackCount = trackCount,
        coverUrl = coverUrl.orEmpty()
    )

private fun PlaylistGenre.toGenreItem(): GenreItem = GenreItem(
    id = id,
    name = name,
    coverUrl = coverUrl.orEmpty()
)

// 固定四个榜单：id 来自网易云官方榜
private data class ChartSpec(
    val id: Long,
    val title: String,
    val subtitle: String
)

private val FixedCharts = listOf(
    ChartSpec(3778678, "热歌榜", "实时热门"),
    ChartSpec(3779629, "新歌榜", "每周更新"),
    ChartSpec(6723173524, "网络热歌榜", "全网爆款"),
    ChartSpec(6688069460, "听歌识曲榜", "听歌识曲热榜")
)

// 初始空态：各区块由接口填充
private fun sampleLibraryUiState(): LibraryUiState = LibraryUiState()
