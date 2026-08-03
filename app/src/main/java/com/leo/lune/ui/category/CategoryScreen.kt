package com.leo.lune.ui.category

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.leo.lune.R
import com.leo.lune.ui.home.HomeSectionHeader
import com.leo.lune.util.consumePointersUnlessResumed
import com.leo.lune.util.rememberCoverRequest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── 布局圆角 ─────────────────────────────────────────────────────────────────
// 卡片通用圆角
private val CardShape = RoundedCornerShape(16.dp)
// 甄选歌单封面圆角
private val PlaylistCoverShape = RoundedCornerShape(12.dp)
// 排行榜卡片圆角
private val ChartShape = RoundedCornerShape(16.dp)
// 风格分类格子圆角
private val GenreShape = RoundedCornerShape(16.dp)

// ─── 猜你喜欢分页 ─────────────────────────────────────────────────────────────
// 每页歌曲数
private const val GuessYouLikePageSize = 3
// 圆点 / 页数
private const val GuessYouLikePageCount = 5
// 总歌曲数 = 页数 × 每页首数
private const val GuessYouLikeSongCount = GuessYouLikePageSize * GuessYouLikePageCount
// 单页高度：3 行 (72dp) + 2 个间距 (6dp)
private val GuessYouLikePageHeight = 72.dp * GuessYouLikePageSize + 6.dp * (GuessYouLikePageSize - 1)

// 曲库页：每日推荐 / 甄选歌单 / 排行榜 / 风格分类
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme


    // 当前高亮的猜你喜欢曲目；null 表示无选中
    var playingSongId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .background(colorScheme.background)
            .consumePointersUnlessResumed(),
        // 底部为 MiniPlayer + TabBar 预留空间
        contentPadding = PaddingValues(bottom = 161.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            DailyRecommendSection(
                dailySongs = uiState.dailyRecommendSongs,
                guessYouLikeSongs = uiState.guessYouLikeSongs,
                playingSongId = playingSongId,
                isDailyRecommendPlaying = uiState.isDailyRecommendPlaying,
                onDailyRecommendPlayClick = viewModel::onDailyRecommendPlayClick,
                onGuessYouLikePlay = { id ->
                    playingSongId = id
                    viewModel.onGuessYouLikePlay(id)
                }
            )
        }

        item {
            FeaturedPlaylistsSection(
                playlists = uiState.featuredPlaylists,
                playingPlaylistId = uiState.playingFeaturedPlaylistId,
                onPlaylistClick = viewModel::onPlaylistClick,
                onViewAllClick = viewModel::onFeaturedPlaylistsAllClick
            )
        }

        item {
            ChartsSection(
                charts = uiState.charts,
                onChartClick = viewModel::onChartClick,
                onViewAllClick = viewModel::onChartsAllClick
            )
        }

        item {
            GenresSection(
                genres = uiState.genres,
                onGenreClick = viewModel::onGenreClick
            )
        }
    }
}

// 每日推荐 Banner + 猜你喜欢分页列表
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DailyRecommendSection(
    // 每日推荐（真实数据，供 Banner）
    dailySongs: List<DailyRecommendSongItem>,
    // 猜你喜欢（推荐新音乐）
    guessYouLikeSongs: List<DailyRecommendSongItem>,
    // 当前高亮播放的猜你喜欢歌曲 id，null 表示无选中
    playingSongId: Long?,
    // 每日推荐 Banner 是否正在播放
    isDailyRecommendPlaying: Boolean,
    // 播放 / 暂停每日推荐
    onDailyRecommendPlayClick: () -> Unit,
    // 点击猜你喜欢单曲播放
    onGuessYouLikePlay: (Long) -> Unit
) {
    // 猜你喜欢：15 首切成 5 页，每页 3 首
    val guessPages = remember(guessYouLikeSongs) {
        guessYouLikeSongs.take(GuessYouLikeSongCount).chunked(GuessYouLikePageSize)
    }
    val pagerState = rememberPagerState(pageCount = { guessPages.size.coerceAtLeast(1) })
    val pagerScope = rememberCoroutineScope()
    // Banner 日期文案
    val dateLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        // 有数据时才展示每日推荐 Banner
        if (dailySongs.isNotEmpty()) {
            DailyMixBanner(
                songs = dailySongs,
                dateLabel = dateLabel,
                isPlayingThis = isDailyRecommendPlaying,
                onPlayClick = onDailyRecommendPlayClick,
            )
        }
        // 猜你喜欢
        if (guessPages.isNotEmpty()) {
            HomeSectionHeader(
                title = "猜你喜欢",
                iconRes = R.drawable.ic_section_sparkles,
                iconTint = Color(0xFFFFB020)
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GuessYouLikePageHeight)
            ) { page ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    guessPages.getOrNull(page).orEmpty().forEach { song ->
                        DailySongRow(
                            song = song,
                            playing = playingSongId == song.id,
                            onPlay = { onGuessYouLikePlay(song.id) }
                        )
                    }
                }
            }
            // 圆点指示器
            GuessYouLikePageIndicator(
                pageCount = guessPages.size,
                currentPage = pagerState.currentPage,
                onDotClick = { index ->
                    pagerScope.launch { pagerState.animateScrollToPage(index) }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp)
            )
        }
    }
}

// 猜你喜欢分页圆点
@Composable
private fun GuessYouLikePageIndicator(
    pageCount: Int,
    currentPage: Int,
    onDotClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (pageCount <= 1) return
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .size(if (selected) 7.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) colorScheme.onBackground
                        else colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onDotClick(index) }
                    )
            )
        }
    }
}

// Daily Mix Banner
@Composable
private fun DailyMixBanner(
    // 每日推荐歌曲列表（取首曲封面作背景）
    songs: List<DailyRecommendSongItem>,
    // Banner 副标题日期文案
    dateLabel: String,
    // 是否正在播放每日推荐队列
    isPlayingThis: Boolean,
    // 播放 / 暂停
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    // 首曲封面 URL，空列表时为空串
    val firstCoverUrl = songs.firstOrNull()?.coverUrl.orEmpty()
    val playInteraction = remember { MutableInteractionSource() }
    val playScope = rememberCoroutineScope()
    // 点击时主动播缩小再回弹，避免短按看不到 isPressed 缩放
    val playScale = remember { Animatable(1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, colorScheme.surfaceDim, CardShape)
    ) {
        // 第一首封面铺满背景
        if (firstCoverUrl.isNotEmpty()) {
            AsyncImage(
                model = rememberCoverRequest(firstCoverUrl, 400.dp, 200.dp),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        }
        // 深色遮罩，保证白字可读
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )

        // 前景：标题区 + 播放钮 + 封面预览条
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧文案
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DAILY MIX",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "每日推荐",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "根据你的口味精选 · $dateLabel",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                // 播放按钮（样式与 HomeScreen FavoritesMainCard 一致）
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(playScale.value)
                        .shadow(12.dp, CircleShape, spotColor = colorScheme.primary)
                        .clip(CircleShape)
                        .background(Color(0xFFF4F2FB))
                        .clickable(
                            interactionSource = playInteraction,
                            indication = null,
                            onClick = {
                                playScope.launch {
                                    playScale.animateTo(0.9f, tween(60))
                                    playScale.animateTo(1f, tween(100))
                                }
                                onPlayClick()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            if (isPlayingThis) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (isPlayingThis) "暂停" else "播放全部",
                        colorFilter = ColorFilter.tint(Color(0xFF0E0E10)),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 封面条：预览前 6 首
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                songs.take(6).forEach { song ->
                    AsyncImage(
                        model = rememberCoverRequest(song.coverUrl, 36.dp),
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

// 猜你喜欢单曲行：布局/字号与 HomeRecentItem 一致，保留 HOT
@Composable
private fun DailySongRow(
    song: DailyRecommendSongItem,
    playing: Boolean,
    onPlay: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val coverShape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardSurface(CardShape)
            .clickable(onClick = onPlay)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            AsyncImage(
                model = rememberCoverRequest(song.coverUrl, 48.dp),
                contentDescription = song.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(coverShape),
                contentScale = ContentScale.Crop
            )
            // 播放中：封面叠半透明层 + 暂停图标
            if (playing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(coverShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_pause),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 21.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (song.hot) {
            Text(
                text = "HOT",
                color = Color(0xFFF87171),
                fontSize = 10.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33EF4444))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Text(
            text = song.duration,
            color = colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        )
    }
}

// 甄选歌单：横向滚动卡片列表
@Composable
private fun FeaturedPlaylistsSection(
    playlists: List<FeaturedPlaylistItem>,
    playingPlaylistId: Long?,
    onPlaylistClick: (Long) -> Unit,
    onViewAllClick: () -> Unit
) {
    if (playlists.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader(
            title = "甄选歌单",
            onViewAllClick = onViewAllClick
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(playlists, key = { it.id }) { playlist ->
                FeaturedPlaylistCard(
                    playlist = playlist,
                    isPlayingThis = playingPlaylistId == playlist.id,
                    onClick = { onPlaylistClick(playlist.id) }
                )
            }
        }
    }
}

// 甄选歌单卡片：封面播放钮 + 标题副标题
@Composable
private fun FeaturedPlaylistCard(
    playlist: FeaturedPlaylistItem,
    isPlayingThis: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val clickScope = rememberCoroutineScope()
    // 点击时主动播缩小再回弹，避免短按看不到 isPressed 缩放
    val scale = remember { Animatable(1f) }
    val playInteraction = remember { MutableInteractionSource() }
    val playScope = rememberCoroutineScope()
    val playScale = remember { Animatable(1f) }

    Column(
        modifier = Modifier
            .width(140.dp)
            .scale(scale.value)
            .cardSurface(CardShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    clickScope.launch {
                        scale.animateTo(0.95f, tween(60))
                        scale.animateTo(1f, tween(100))
                    }
                    onClick()
                }
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(124.dp)
                .clip(PlaylistCoverShape)
        ) {
            AsyncImage(
                model = rememberCoverRequest(playlist.coverUrl, 124.dp),
                contentDescription = playlist.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // 右下角播放钮（样式与 DailyMixBanner 一致，尺寸更小）
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(32.dp)
                    .scale(playScale.value)
                    .shadow(8.dp, CircleShape, spotColor = colorScheme.primary)
                    .clip(CircleShape)
                    .background(Color(0xFFF4F2FB))
                    .clickable(
                        interactionSource = playInteraction,
                        indication = null,
                        onClick = {
                            playScope.launch {
                                playScale.animateTo(0.9f, tween(60))
                                playScale.animateTo(1f, tween(100))
                            }
                            onClick()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        if (isPlayingThis) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                    contentDescription = if (isPlayingThis) "暂停" else "播放歌单",
                    colorFilter = ColorFilter.tint(Color(0xFF0E0E10)),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 2.dp)) {
            Text(
                text = playlist.title,
                color = colorScheme.onBackground,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = playlist.subtitle,
                color = colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// 排行榜：横向滚动，卡片含渐变顶栏与歌曲预览
@Composable
private fun ChartsSection(
    charts: List<ChartItem>,
    onChartClick: (Long) -> Unit,
    onViewAllClick: () -> Unit
) {
    if (charts.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader(title = "排行榜")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(charts, key = { it.id }) { chart ->
                ChartCard(
                    chart = chart,
                    onClick = { onChartClick(chart.id) }
                )
            }
        }
    }
}

// 排行榜卡片：彩色顶栏标题 + 前三名封面预览；glowColor 用于外发光阴影
@Composable
private fun ChartCard(
    chart: ChartItem,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val clickScope = rememberCoroutineScope()
    // 点击时主动播缩小再回弹，避免短按看不到 isPressed 缩放
    val scale = remember { Animatable(1f) }
    // headerGradient 存的是 ARGB Long，转为 Compose Color
    val headerColors = chart.headerGradient.map { Color(it.toInt()) }

    Column(
        modifier = Modifier
            .width(160.dp)
            .scale(scale.value)
            .shadow(
                elevation = 8.dp,
                shape = ChartShape,
                spotColor = Color(chart.glowColor.toInt()),
                ambientColor = Color(chart.glowColor.toInt())
            )
            .cardSurface(ChartShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    clickScope.launch {
                        scale.animateTo(0.95f, tween(60))
                        scale.animateTo(1f, tween(100))
                    }
                    onClick()
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(headerColors))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = chart.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chart.songs.forEachIndexed { index, song ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${index + 1}",
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.width(14.dp)
                    )
                    AsyncImage(
                        model = rememberCoverRequest(song.coverUrl, 28.dp),
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = song.title,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// 风格分类：横向滚动方形封面格子
@Composable
private fun GenresSection(
    genres: List<GenreItem>,
    onGenreClick: (Long) -> Unit
) {
    if (genres.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader(title = "风格分类")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(genres, key = { it.id }) { genre ->
                GenreCard(
                    genre = genre,
                    onClick = { onGenreClick(genre.id) }
                )
            }
        }
    }
}

// 风格分类格子：背景图 + 底部渐变遮罩 + 分类名
@Composable
private fun GenreCard(
    genre: GenreItem,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val clickScope = rememberCoroutineScope()
    // 点击时主动播缩小再回弹，避免短按看不到 isPressed 缩放
    val scale = remember { Animatable(1f) }
    val imageScale = remember { Animatable(1f) }

    Box(
        modifier = Modifier
            .size(90.dp)
            .scale(scale.value)
            .clip(GenreShape)
            .border(1.dp, MaterialTheme.colorScheme.surfaceDim, GenreShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    clickScope.launch {
                        launch {
                            scale.animateTo(0.93f, tween(60))
                            scale.animateTo(1f, tween(100))
                        }
                        launch {
                            imageScale.animateTo(1.1f, tween(120))
                            imageScale.animateTo(1f, tween(200))
                        }
                    }
                    onClick()
                }
            )
    ) {
        AsyncImage(
            model = rememberCoverRequest(genre.coverUrl, 90.dp),
            contentDescription = genre.name,
            modifier = Modifier
                .fillMaxSize()
                .scale(imageScale.value),
            contentScale = ContentScale.Crop
        )
        // 自下而上遮罩，保证白字可读
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )
        Text(
            text = genre.name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(10.dp)
        )
    }
}

// 卡片表面：与 HomeRecentItem 一致（surfaceVariant 底 + surfaceDim 描边）
@Composable
private fun Modifier.cardSurface(shape: RoundedCornerShape): Modifier {
    val colorScheme = MaterialTheme.colorScheme
    return this
        .clip(shape)
        .background(colorScheme.surfaceVariant)
        .border(1.dp, colorScheme.surfaceDim, shape)
}
