package com.leo.lune.ui.category



import androidx.lifecycle.ViewModel

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import javax.inject.Inject



// 每日推荐单曲

data class DailyRecommendSongItem(

    val id: Long,

    val title: String,

    val artist: String,

    val coverUrl: String

)



// 甄选歌单条目

data class FeaturedPlaylistItem(

    val id: Long,

    val title: String,

    val coverUrl: String

)



// 排行榜内单曲预览

data class ChartSongPreview(

    val rank: String,

    val title: String,

    val artist: String

)



// 排行榜卡片；gradientColors 为 ARGB Long，由 UI 转为 Color

data class ChartItem(

    val id: Long,

    val title: String,

    val gradientColors: List<Long>,

    val songs: List<ChartSongPreview>

)



// 音乐类型格子

data class GenreItem(

    val id: Long,

    val name: String,

    val coverUrl: String

)



// 分类页 UI 状态（目前为静态占位数据，不做真实请求）

data class CategoryUiState(

    val dailyRecommendSongs: List<DailyRecommendSongItem> = emptyList(),

    val featuredPlaylists: List<FeaturedPlaylistItem> = emptyList(),

    val charts: List<ChartItem> = emptyList(),

    val genres: List<GenreItem> = emptyList()

)



// 分类页 ViewModel：仅提供静态 UI 数据，点击事件预留空实现

@HiltViewModel

class CategoryViewModel @Inject constructor() : ViewModel() {



    private val _uiState = MutableStateFlow(sampleCategoryUiState())

    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()



    // 播放每日推荐单曲（占位）

    fun onDailyRecommendPlay(songId: Long) = Unit



    // 点击甄选歌单（占位）

    fun onPlaylistClick(playlistId: Long) = Unit



    // 点击「甄选歌单 · 全部」（占位）

    fun onFeaturedPlaylistsAllClick() = Unit



    // 点击排行榜（占位）

    fun onChartClick(chartId: Long) = Unit



    // 点击音乐类型（占位）

    fun onGenreClick(genreId: Long) = Unit

}



// 与设计稿对齐的静态占位数据

private fun sampleCategoryUiState(): CategoryUiState = CategoryUiState(

    dailyRecommendSongs = listOf(

        DailyRecommendSongItem(

            id = 1,

            title = "爱的箴言",

            artist = "彩云竟朝夕",

            coverUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=280&h=404&fit=crop&auto=format"

        ),

        DailyRecommendSongItem(

            id = 2,

            title = "沿海独白沿海独白沿海独白沿海独白",

            artist = "王唯一",

            coverUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=280&h=404&fit=crop&auto=format"

        ),

        DailyRecommendSongItem(

            id = 3,

            title = "晚风",

            artist = "你的晚风",

            coverUrl = "https://images.unsplash.com/photo-1502691876148-a84978e59af8?w=280&h=404&fit=crop&auto=format"

        ),

        DailyRecommendSongItem(

            id = 4,

            title = "说散就散",

            artist = "狗二",

            coverUrl = "https://images.unsplash.com/photo-1520170350707-b2da59970118?w=280&h=404&fit=crop&auto=format"

        ),

        DailyRecommendSongItem(

            id = 5,

            title = "雾中来信",

            artist = "白川",

            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=280&h=404&fit=crop&auto=format"

        )

    ),

    featuredPlaylists = listOf(

        FeaturedPlaylistItem(

            id = 1,

            title = "深夜留声机",

            coverUrl = "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=560&h=400&fit=crop&auto=format"

        ),

        FeaturedPlaylistItem(

            id = 2,

            title = "独处时刻",

            coverUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=560&h=400&fit=crop&auto=format"

        ),

        FeaturedPlaylistItem(

            id = 3,

            title = "城市漫游",

            coverUrl = "https://images.unsplash.com/photo-1519608487953-e999c86e7454?w=560&h=400&fit=crop&auto=format"

        )

    ),

    charts = listOf(

        ChartItem(

            id = 1,

            title = "热歌榜",

            gradientColors = listOf(0xFFFF7A59L, 0xFFD94A6DL, 0xFF392D61L),

            songs = listOf(

                ChartSongPreview("01", "爱的箴言", "彩云竟朝夕"),

                ChartSongPreview("02", "沿海独白", "王唯一"),

                ChartSongPreview("03", "晚风", "你的晚风")

            )

        ),

        ChartItem(

            id = 2,

            title = "新歌榜",

            gradientColors = listOf(0xFF4E8CFFL, 0xFF5268C7L, 0xFF2E285CL),

            songs = listOf(

                ChartSongPreview("01", "说散就散", "狗二"),

                ChartSongPreview("02", "山川", "雾野"),

                ChartSongPreview("03", "无眠海", "夏林")

            )

        ),

        ChartItem(

            id = 3,

            title = "飙升榜",

            gradientColors = listOf(0xFFE2B94FL, 0xFFBD7043L, 0xFF5A3043L),

            songs = listOf(

                ChartSongPreview("01", "落日航班", "陆离"),

                ChartSongPreview("02", "空城来信", "陈雨"),

                ChartSongPreview("03", "晴天之后", "白川")

            )

        )

    ),

    genres = listOf(

        GenreItem(

            id = 1,

            name = "流行",

            coverUrl = "https://images.unsplash.com/photo-1524368535928-5b5e00ddc76b?w=300&h=300&fit=crop&auto=format"

        ),

        GenreItem(

            id = 2,

            name = "摇滚",

            coverUrl = "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=300&h=300&fit=crop&auto=format"

        ),

        GenreItem(

            id = 3,

            name = "民谣",

            coverUrl = "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=300&h=300&fit=crop&auto=format"

        ),

        GenreItem(

            id = 4,

            name = "电子",

            coverUrl = "https://images.unsplash.com/photo-1571266028243-d220c9c3b21e?w=300&h=300&fit=crop&auto=format"

        ),

        GenreItem(

            id = 5,

            name = "说唱",

            coverUrl = "https://images.unsplash.com/photo-1521337706264-a414f153a5bd?w=300&h=300&fit=crop&auto=format"

        ),

        GenreItem(

            id = 6,

            name = "爵士",

            coverUrl = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=300&h=300&fit=crop&auto=format"

        ),

        GenreItem(

            id = 7,

            name = "古典",

            coverUrl = "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?w=300&h=300&fit=crop&auto=format"

        ),

        GenreItem(

            id = 8,

            name = "轻音乐",

            coverUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300&h=300&fit=crop&auto=format"

        )

    )

)

