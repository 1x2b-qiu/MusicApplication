package com.leo.lune.ui.category

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.AuthRepository
import com.leo.lune.domain.repository.MusicRepository
import com.leo.lune.ui.home.formatSongDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

// 曲库页 UI 状态：每日推荐 / 猜你喜欢为真实数据，其余暂为占位
data class CategoryUiState(
    // 每日推荐（Banner）
    val dailyRecommendSongs: List<DailyRecommendSongItem> = emptyList(),
    // 猜你喜欢（推荐新音乐）
    val guessYouLikeSongs: List<DailyRecommendSongItem> = emptyList(),
    val featuredPlaylists: List<FeaturedPlaylistItem> = emptyList(),
    val charts: List<ChartItem> = emptyList(),
    val genres: List<GenreItem> = emptyList()
)

// 曲库页 ViewModel：每日推荐 + 猜你喜欢走网易云接口，其余仍用静态占位
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

    init {
        viewModelScope.launch {
            authRepository.observeLoginState().collect { loginState ->
                if (loginState.isLoggedIn) {
                    loadDailyRecommend()
                    loadGuessYouLike()
                } else {
                    dailyRecommendQueue = emptyList()
                    guessYouLikeQueue = emptyList()
                    _uiState.update {
                        it.copy(
                            dailyRecommendSongs = emptyList(),
                            guessYouLikeSongs = emptyList()
                        )
                    }
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
                        it.copy(dailyRecommendSongs = songs.map { song -> song.toDailyRecommendItem() })
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

    // 猜你喜欢：播放指定歌曲（队列为当前推荐列表）
    @RequiresApi(Build.VERSION_CODES.O)
    fun onGuessYouLikePlay(songId: Long) {
        val queue = guessYouLikeQueue
        val song = queue.firstOrNull { it.id == songId } ?: return
        playerController.playSong(song, queue)
    }

    // 每日推荐：播放全部
    @RequiresApi(Build.VERSION_CODES.O)
    fun onDailyRecommendPlayAll() {
        val queue = dailyRecommendQueue
        if (queue.isEmpty()) return
        playerController.playSong(queue.first(), queue)
    }

    fun onPlaylistClick(playlistId: Long) = Unit

    fun onFeaturedPlaylistsAllClick() = Unit

    fun onChartClick(chartId: Long) = Unit

    fun onChartsAllClick() = Unit

    fun onGenreClick(genreId: Long) = Unit
}

private fun Song.toDailyRecommendItem(): DailyRecommendSongItem = DailyRecommendSongItem(
    id = id,
    title = name,
    artist = artists,
    coverUrl = coverUrl.orEmpty(),
    duration = formatSongDuration(durationMs)
)

private fun u(id: String, w: Int = 400, h: Int = 400): String =
    "https://images.unsplash.com/$id?w=$w&h=$h&fit=crop&auto=format"

// 静态占位：甄选歌单 / 排行榜 / 风格分类（不含每日推荐与猜你喜欢）
private fun sampleCategoryUiState(): CategoryUiState = CategoryUiState(
    featuredPlaylists = listOf(
        FeaturedPlaylistItem(1, "深夜孤独症", "凌晨2点", 42, u("photo-1761104169769-1aefefbcb5f2", 300, 300)),
        FeaturedPlaylistItem(2, "赛博朋克", "未来感", 88, u("photo-1784401930662-b9e573c8d79e", 300, 300)),
        FeaturedPlaylistItem(3, "清晨咖啡馆", "轻音乐", 36, u("photo-1618172842918-3eabce30c912", 300, 300)),
        FeaturedPlaylistItem(4, "电子夜游记", "EDM合集", 64, u("photo-1768885514740-d64d25ac9a64", 300, 300)),
        FeaturedPlaylistItem(5, "失眠专用", "白噪音", 29, u("photo-1752801375943-f0cb633f3422", 300, 300)),
        FeaturedPlaylistItem(6, "燃爆健身房", "高强度", 57, u("photo-1619983081593-e2ba5b543168", 300, 300)),
        FeaturedPlaylistItem(7, "雨天发呆", "Lo-Fi", 33, u("photo-1657627157213-c5f44dbd0724", 300, 300)),
        FeaturedPlaylistItem(8, "午夜情书", "R&B精选", 51, u("photo-1539631934288-4f99f71032c6", 300, 300)),
        FeaturedPlaylistItem(9, "阳光早安", "元气满满", 24, u("photo-1620219365320-a8c4e958ef0b", 300, 300))
    ),
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
