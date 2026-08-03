package com.leo.lune.ui.category

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.controller.PlaybackState
import com.leo.lune.domain.model.PersonalizedPlaylist
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
    val title: String,
    val artist: String,
    val coverUrl: String
)

// 排行榜卡片；headerGradient 为 ARGB Long，由 UI 转为 Color
data class ChartItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val headerGradient: List<Long>,
    val glowColor: Long,
    val songs: List<ChartSongPreview>
)

// 风格分类格子
data class GenreItem(
    val id: Long,
    val name: String,
    val coverUrl: String
)

// 曲库页 UI 状态：每日推荐 / 猜你喜欢 / 甄选歌单为真实数据，其余暂为占位
data class CategoryUiState(
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

// 曲库页 ViewModel：每日推荐 + 猜你喜欢 + 甄选歌单走网易云接口，其余仍用静态占位
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val authRepository: AuthRepository,
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(sampleCategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    // 每日推荐原始队列，供播放器用
    private var dailyRecommendQueue: List<Song> = emptyList()
    // 猜你喜欢原始队列，供播放器用
    private var guessYouLikeQueue: List<Song> = emptyList()
    // 甄选歌单播放标记：用于判断当前播放是否来自某个甄选歌单
    private val featuredPlaylistMarkers = mutableMapOf<Long, FeaturedPlaylistMarker>()

    init {
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

    // 甄选歌单：拉取歌单歌曲并立即播放（不进入详情）
    @RequiresApi(Build.VERSION_CODES.O)
    fun onPlaylistClick(playlistId: Long) {
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

    fun onFeaturedPlaylistsAllClick() = Unit

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

private fun u(id: String, w: Int = 400, h: Int = 400): String =
    "https://images.unsplash.com/$id?w=$w&h=$h&fit=crop&auto=format"

// 静态占位：排行榜 / 风格分类（甄选歌单改由接口填充）
private fun sampleCategoryUiState(): CategoryUiState = CategoryUiState(
    charts = listOf(
        ChartItem(
            id = 1,
            title = "热歌榜",
            subtitle = "实时热门",
            headerGradient = listOf(0xFFF97316L, 0xFFEF4444L),
            glowColor = 0x40EF4444L,
            songs = listOf(
                ChartSongPreview("Starfall", "Lunar Echo", u("photo-1618172842918-3eabce30c912", 80, 80)),
                ChartSongPreview("Afterimage", "KAIA", u("photo-1657627157213-c5f44dbd0724", 80, 80)),
                ChartSongPreview("Dreamcore", "Cello Wolf", u("photo-1784401930662-b9e573c8d79e", 80, 80))
            )
        ),
        ChartItem(
            id = 2,
            title = "新歌榜",
            subtitle = "每周更新",
            headerGradient = listOf(0xFF34D399L, 0xFF06B6D4L),
            glowColor = 0x4022D3EEL,
            songs = listOf(
                ChartSongPreview("Quantum Drift", "vektøR", u("photo-1619983081593-e2ba5b543168", 80, 80)),
                ChartSongPreview("Glass Ocean", "Mira Sol", u("photo-1752801375943-f0cb633f3422", 80, 80)),
                ChartSongPreview("Velvet Sky", "ANA", u("photo-1761104169769-1aefefbcb5f2", 80, 80))
            )
        ),
        ChartItem(
            id = 3,
            title = "网络热歌榜",
            subtitle = "全网爆款",
            headerGradient = listOf(0xFF60A5FAL, 0xFF8B5CF6L),
            glowColor = 0x408B5CF6L,
            songs = listOf(
                ChartSongPreview("Purple Haze", "Neon Drift", u("photo-1619983081593-e2ba5b543168", 80, 80)),
                ChartSongPreview("Circuit Bloom", "ByteFlower", u("photo-1539631934288-4f99f71032c6", 80, 80)),
                ChartSongPreview("Neon Gospel", "Phantom Eye", u("photo-1620219365320-a8c4e958ef0b", 80, 80))
            )
        ),
        ChartItem(
            id = 4,
            title = "听歌识曲榜",
            subtitle = "Shazam 热榜",
            headerGradient = listOf(0xFFEC4899L, 0xFFFB7185L),
            glowColor = 0x40EC4899L,
            songs = listOf(
                ChartSongPreview("Serotonin Rush", "SANA", u("photo-1768885514740-d64d25ac9a64", 80, 80)),
                ChartSongPreview("Dreamcore", "Cello Wolf", u("photo-1784401930662-b9e573c8d79e", 80, 80)),
                ChartSongPreview("Starfall", "Lunar Echo", u("photo-1618172842918-3eabce30c912", 80, 80))
            )
        )
    ),
    genres = listOf(
        GenreItem(1, "流行", u("photo-1514525253161-7a46d19cd819", 600, 400)),
        GenreItem(2, "摇滚", u("photo-1516924962500-2b4b3b99ea02", 600, 400)),
        GenreItem(3, "电子", u("photo-1470225620780-dba8ba36b745", 600, 400)),
        GenreItem(4, "嘻哈", u("photo-1638115311992-494f8d1d858b", 600, 400)),
        GenreItem(5, "R&B", u("photo-1655659775262-3a4a479532b0", 600, 400)),
        GenreItem(6, "国际", u("photo-1646765444015-5881f0fab3e8", 600, 400)),
        GenreItem(7, "爵士", u("photo-1774544809959-1991aa21c904", 600, 400)),
        GenreItem(8, "古典", u("photo-1763627516727-2ca3e324fa59", 600, 400)),
        GenreItem(9, "民谣", u("photo-1758272960116-93c2a2463e42", 600, 400)),
        GenreItem(10, "氛围", u("photo-1696488567389-e582d3ba3f19", 600, 400)),
        GenreItem(11, "说唱", u("photo-1771775735506-9705c4c186b8", 600, 400)),
        GenreItem(12, "轻音乐", u("photo-1565879629766-30adf38aac56", 600, 400))
    )
)
