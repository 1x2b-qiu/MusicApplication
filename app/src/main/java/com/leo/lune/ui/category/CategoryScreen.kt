package com.leo.lune.ui.category


import android.os.Build

import androidx.annotation.RequiresApi

import androidx.compose.foundation.Image

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.interaction.collectIsPressedAsState

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.aspectRatio

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.LazyRow

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.ColorFilter

import androidx.compose.ui.graphics.luminance

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.core.graphics.drawable.toBitmap

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.palette.graphics.Palette

import coil.compose.AsyncImage

import com.leo.lune.R

import com.leo.lune.util.consumePointersUnlessResumed

import com.leo.lune.util.rememberCoverRequest

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext



private val DailyGlassOuterShape = RoundedCornerShape(12.dp)

// 封面主题色尚未算出时的底部衬底兜底色
private val DailyRecommendBackdropFallback = Color(0xFF1C1C1E)

private val PlaylistCoverShape = RoundedCornerShape(17.dp)
private val ChartCardShape = RoundedCornerShape(24.dp)

private val GenreCardShape = RoundedCornerShape(16.dp)


// 分类页：每日推荐、甄选歌单、排行榜、音乐类型

@RequiresApi(Build.VERSION_CODES.O)

@Composable

fun CategoryScreen(

    darkTheme: Boolean = true,

    onDailyRecommendAllClick: () -> Unit = {},

    viewModel: CategoryViewModel = hiltViewModel()

) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val colorScheme = MaterialTheme.colorScheme



    LazyColumn(

        modifier = Modifier

            .fillMaxSize()

            .background(colorScheme.background)

            .consumePointersUnlessResumed(),

        contentPadding = PaddingValues(bottom = 161.dp),

        verticalArrangement = Arrangement.spacedBy(32.dp)

    ) {
        item {

            DailyRecommendSection(

                songs = uiState.dailyRecommendSongs,

                onViewAllClick = onDailyRecommendAllClick,

                onPlayClick = viewModel::onDailyRecommendPlay

            )

        }



        item {

            FeaturedPlaylistsSection(

                playlists = uiState.featuredPlaylists,

                onPlaylistClick = viewModel::onPlaylistClick,

                onViewAllClick = viewModel::onFeaturedPlaylistsAllClick

            )

        }



        item {

            ChartsSection(

                charts = uiState.charts,

                darkTheme = darkTheme,

                onChartClick = viewModel::onChartClick

            )

        }



        item {

            GenresSection(

                genres = uiState.genres,

                darkTheme = darkTheme,

                onGenreClick = viewModel::onGenreClick

            )

        }

    }

}


// 区块标题行：左侧标题，右侧可选「全部」

@Composable

private fun CategorySectionHeader(

    title: String,

    actionLabel: String? = null,

    onActionClick: (() -> Unit)? = null

) {

    val colorScheme = MaterialTheme.colorScheme

    Row(

        modifier = Modifier

            .fillMaxWidth()

            .padding(vertical = 12.dp),

        horizontalArrangement = Arrangement.SpaceBetween,

        verticalAlignment = Alignment.CenterVertically

    ) {

        Row(

            horizontalArrangement = Arrangement.spacedBy(8.dp),

            verticalAlignment = Alignment.CenterVertically

        ) {

            Text(

                text = title,

                color = colorScheme.onBackground,

                fontSize = 17.sp,

                fontWeight = FontWeight.Medium,

                lineHeight = 25.5.sp,

                letterSpacing = (-0.34).sp

            )

        }

        if (actionLabel != null && onActionClick != null) {

            Row(

                verticalAlignment = Alignment.CenterVertically,

                modifier = Modifier.clickable(

                    interactionSource = remember { MutableInteractionSource() },

                    indication = null,

                    onClick = onActionClick

                ),

                horizontalArrangement = Arrangement.spacedBy(2.dp)

            ) {

                Text(

                    text = actionLabel,

                    color = colorScheme.onSurfaceVariant,

                    fontSize = 13.sp,

                    fontWeight = FontWeight.Medium,

                    lineHeight = 19.5.sp

                )

                Image(

                    painter = painterResource(R.drawable.ic_chevron_right),

                    contentDescription = null,

                    modifier = Modifier.size(16.dp)

                )

            }

        }

    }

}


// 今日推荐：横向列表；「全部」进入独立页

@Composable
private fun DailyRecommendSection(
    songs: List<DailyRecommendSongItem>,
    onViewAllClick: () -> Unit,
    onPlayClick: (Long) -> Unit
) {
    if (songs.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        CategorySectionHeader(
            title = "今日推荐",
            actionLabel = "全部",
            onActionClick = onViewAllClick
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                DailyRecommendCard(
                    song = song,
                    onPlayClick = { onPlayClick(song.id) },
                    modifier = Modifier.width(140.dp)
                )
            }
        }
    }
}

// 每日推荐单曲卡片：封面取主题色，底部渐变到纯色衬底保证白字可读
@Composable
private fun DailyRecommendCard(
    song: DailyRecommendSongItem,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var themeColor by remember(song.coverUrl) { mutableStateOf(DailyRecommendBackdropFallback) }
    var coverBitmap by remember(song.coverUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(coverBitmap) {
        val bitmap = coverBitmap ?: return@LaunchedEffect
        themeColor = withContext(Dispatchers.Default) {
            extractDailyRecommendThemeColor(bitmap)
        }
    }

    val playInteraction = remember { MutableInteractionSource() }
    val isPlayPressed by playInteraction.collectIsPressedAsState()
    val playScale = if (isPlayPressed) 0.92f else 1f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(DailyGlassOuterShape)
            .clickable(onClick = onPlayClick)
    ) {
        AsyncImage(
            model = rememberCoverRequest(song.coverUrl, 120.dp, 170.dp),
            contentDescription = song.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onSuccess = { state ->
                coverBitmap = runCatching {
                    state.result.drawable.toBitmap()
                }.getOrNull()
            }
        )

        // 底部全宽衬底：主题色由透明渐变到纯色
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to themeColor.copy(alpha = 0f),
                            0.35f to themeColor.copy(alpha = 0.55f),
                            0.7f to themeColor.copy(alpha = 0.9f),
                            1f to themeColor
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .height(60.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .scale(playScale)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(0.67.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable(
                        interactionSource = playInteraction,
                        indication = null,
                        onClick = onPlayClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_play),
                    contentDescription = "播放",
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 从封面提取适合作文字衬底的主题色；过亮时压暗保证白字可读
private fun extractDailyRecommendThemeColor(bitmap: android.graphics.Bitmap): Color {
    // Coil 常用 HARDWARE 位图，Palette 无法 getPixels，需先拷到软件位图
    val softwareBitmap = if (bitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
        bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
    } else {
        bitmap
    } ?: return DailyRecommendBackdropFallback

    return try {
        val palette = Palette.from(softwareBitmap).clearFilters().generate()
        val swatch = palette.darkMutedSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.dominantSwatch
        val color = if (swatch != null) Color(swatch.rgb) else DailyRecommendBackdropFallback
        ensureDarkDailyBackdrop(color)
    } finally {
        if (softwareBitmap !== bitmap) {
            softwareBitmap.recycle()
        }
    }
}

private fun ensureDarkDailyBackdrop(color: Color): Color {
    return if (color.luminance() > 0.45f) {
        Color(
            red = color.red * 0.35f,
            green = color.green * 0.35f,
            blue = color.blue * 0.35f,
            alpha = 1f
        )
    } else {
        color
    }
}


// 甄选歌单：横向滚动，封面 + 标题，无磨砂玻璃外框

@Composable

private fun FeaturedPlaylistsSection(

    playlists: List<FeaturedPlaylistItem>,

    onPlaylistClick: (Long) -> Unit,

    onViewAllClick: () -> Unit

) {

    if (playlists.isEmpty()) return



    Column(modifier = Modifier.fillMaxWidth()) {

        CategorySectionHeader(

            title = "甄选歌单",

            actionLabel = "全部",

            onActionClick = onViewAllClick

        )



        LazyRow(

            horizontalArrangement = Arrangement.spacedBy(12.dp),

            contentPadding = PaddingValues(end = 4.dp)

        ) {

            items(playlists, key = { it.id }) { playlist ->

                FeaturedPlaylistCard(

                    playlist = playlist,

                    onClick = { onPlaylistClick(playlist.id) }

                )

            }

        }

    }

}


@Composable

private fun FeaturedPlaylistCard(

    playlist: FeaturedPlaylistItem,

    onClick: () -> Unit

) {

    val colorScheme = MaterialTheme.colorScheme

    val interactionSource = remember { MutableInteractionSource() }

    val isPressed by interactionSource.collectIsPressedAsState()

    val scale = if (isPressed) 0.95f else 1f



    Column(

        modifier = Modifier

            .width(154.dp)

            .scale(scale)

            .clickable(

                interactionSource = interactionSource,

                indication = null,

                onClick = onClick

            )

    ) {

        AsyncImage(

            model = rememberCoverRequest(playlist.coverUrl, 154.dp),

            contentDescription = playlist.title,

            modifier = Modifier

                .fillMaxWidth()

                .aspectRatio(1.05f)

                .clip(PlaylistCoverShape),

            contentScale = ContentScale.Crop

        )

        Text(

            text = playlist.title,

            color = colorScheme.onBackground,

            fontSize = 14.sp,

            fontWeight = FontWeight.Medium,

            lineHeight = 21.sp,

            maxLines = 1,

            overflow = TextOverflow.Ellipsis,

            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)

        )

    }

}


// 排行榜：渐变卡片横向滚动

@Composable

private fun ChartsSection(

    charts: List<ChartItem>,

    darkTheme: Boolean,

    onChartClick: (Long) -> Unit

) {

    if (charts.isEmpty()) return



    Column(modifier = Modifier.fillMaxWidth()) {

        CategorySectionHeader(title = "排行榜")



        LazyRow(

            horizontalArrangement = Arrangement.spacedBy(12.dp),

            contentPadding = PaddingValues(end = 4.dp)

        ) {

            items(charts, key = { it.id }) { chart ->

                ChartCard(

                    chart = chart,

                    darkTheme = darkTheme,

                    onClick = { onChartClick(chart.id) }

                )

            }

        }

    }

}


@Composable

private fun ChartCard(

    chart: ChartItem,

    darkTheme: Boolean,

    onClick: () -> Unit

) {

    val interactionSource = remember { MutableInteractionSource() }

    val isPressed by interactionSource.collectIsPressedAsState()

    val scale = if (isPressed) 0.95f else 1f

    val gradientColors = chart.gradientColors.map { Color(it.toInt()) }

    val borderColor =
        if (darkTheme) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.35f)



    Column(

        modifier = Modifier

            .width(238.dp)

            .scale(scale)

            .clip(ChartCardShape)

            .background(

                Brush.linearGradient(colors = gradientColors),

                ChartCardShape

            )

            .border(0.67.dp, borderColor, ChartCardShape)

            .clickable(

                interactionSource = interactionSource,

                indication = null,

                onClick = onClick

            )

            .padding(16.dp)

    ) {

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .padding(bottom = 16.dp),

            horizontalArrangement = Arrangement.SpaceBetween,

            verticalAlignment = Alignment.CenterVertically

        ) {

            Text(

                text = chart.title,

                color = Color.White,

                fontSize = 16.sp,

                fontWeight = FontWeight.SemiBold,

                lineHeight = 24.sp

            )

            Box(

                modifier = Modifier

                    .clip(RoundedCornerShape(50))

                    .background(Color.Black.copy(alpha = 0.15f))

                    .padding(horizontal = 8.dp, vertical = 4.dp)

            ) {

                Text(

                    text = "今日",

                    color = Color.White.copy(alpha = 0.8f),

                    fontSize = 10.sp,

                    fontWeight = FontWeight.Medium,

                    lineHeight = 14.sp

                )

            }

        }



        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            chart.songs.forEach { song ->

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.spacedBy(8.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Text(

                        text = song.rank,

                        color = Color.White.copy(alpha = 0.7f),

                        fontSize = 12.sp,

                        fontWeight = FontWeight.SemiBold,

                        lineHeight = 18.sp,

                        modifier = Modifier.width(20.dp)

                    )

                    Column(modifier = Modifier.weight(1f)) {

                        Text(

                            text = song.title,

                            color = Color.White,

                            fontSize = 12.sp,

                            fontWeight = FontWeight.Medium,

                            lineHeight = 18.sp,

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis

                        )

                        Text(

                            text = song.artist,

                            color = Color.White.copy(alpha = 0.6f),

                            fontSize = 10.sp,

                            lineHeight = 14.sp,

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis,

                            modifier = Modifier.padding(top = 2.dp)

                        )

                    }

                }

            }

        }

    }

}


// 音乐类型：四列网格

@Composable

private fun GenresSection(

    genres: List<GenreItem>,

    darkTheme: Boolean,

    onGenreClick: (Long) -> Unit

) {

    if (genres.isEmpty()) return


    val borderColor =
        if (darkTheme) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.35f)



    Column(modifier = Modifier.fillMaxWidth()) {

        CategorySectionHeader(title = "音乐类型")



        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            genres.chunked(4).forEach { rowGenres ->

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.spacedBy(10.dp)

                ) {

                    rowGenres.forEach { genre ->

                        GenreCard(

                            genre = genre,

                            borderColor = borderColor,

                            onClick = { onGenreClick(genre.id) },

                            modifier = Modifier.weight(1f)

                        )

                    }

                    repeat(4 - rowGenres.size) {

                        Spacer(modifier = Modifier.weight(1f))

                    }

                }

            }

        }

    }

}


@Composable

private fun GenreCard(

    genre: GenreItem,

    borderColor: Color,

    onClick: () -> Unit,

    modifier: Modifier = Modifier

) {

    val interactionSource = remember { MutableInteractionSource() }

    val isPressed by interactionSource.collectIsPressedAsState()

    val scale = if (isPressed) 0.95f else 1f



    Box(

        modifier = modifier

            .aspectRatio(1f)

            .scale(scale)

            .clip(GenreCardShape)

            .border(0.67.dp, borderColor, GenreCardShape)

            .clickable(

                interactionSource = interactionSource,

                indication = null,

                onClick = onClick

            )

    ) {

        AsyncImage(

            model = rememberCoverRequest(genre.coverUrl, 80.dp),

            contentDescription = genre.name,

            modifier = Modifier.fillMaxSize(),

            contentScale = ContentScale.Crop

        )

        Box(

            modifier = Modifier

                .fillMaxSize()

                .background(

                    Brush.verticalGradient(

                        colors = listOf(

                            Color.Transparent,

                            Color.Black.copy(alpha = 0.72f)

                        ),

                        startY = 0f,

                        endY = Float.POSITIVE_INFINITY

                    )

                )

        )

        Text(

            text = genre.name,

            color = Color.White,

            fontSize = 14.sp,

            fontWeight = FontWeight.Medium,

            lineHeight = 21.sp,

            modifier = Modifier

                .align(Alignment.BottomStart)

                .padding(start = 10.dp, bottom = 10.dp)

        )

    }

}

