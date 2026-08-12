package com.leo.lune.ui.home

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.leo.lune.R
import com.leo.lune.domain.model.Song
import com.leo.lune.ui.component.lyricsheader.HomeLyricsHeaderContentHeight
import com.leo.lune.util.consumePointersUnlessResumed
import com.leo.lune.util.rememberCoverRequest
import kotlinx.coroutines.delay

// 「我喜欢的」缩略图行参数
private const val FavoritesThumbTransitionMs = 300
// 「我喜欢的」自动轮播总开关；关掉后保留下方轮播逻辑，仅不启动
private const val FavoritesAutoCarouselEnabled = false
private const val FavoritesAutoCarouselIntervalMs = 4_000L
private const val FavoritesAutoCarouselResumeDelayMs = 5_000L
private val ThumbShape = RoundedCornerShape(16.dp)
private val ThumbOuterSize = 66.dp

// 首页：可滚动内容区（顶栏由 MusicNavHost 统一挂载）
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    onLikedClick: (coverUrl: String, trackCount: Int) -> Unit,
    onRecentClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(top = HomeLyricsHeaderContentHeight)
            .padding(horizontal = 16.dp)
            .consumePointersUnlessResumed()
    ) {
        when {
            uiState.error != null && uiState.recentSongs.isEmpty() -> {
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 161.dp)
                ) {
                    if (uiState.likedSongs.isNotEmpty()) {
                        item {
                            HomeFavoritesSection(
                                songs = uiState.likedSongs,
                                isPlaying = uiState.isPlaying,
                                currentSongId = uiState.currentSongId,
                                onPlaySong = viewModel::playSong,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onViewAllClick = {
                                    onLikedClick(
                                        uiState.likedSongs.firstOrNull()?.coverUrl.orEmpty(),
                                        uiState.likedTrackCount
                                    )
                                }
                            )
                        }
                    }
                    if (uiState.recentSongs.isNotEmpty()) {
                        item {
                            HomeRecentThumbSection(
                                songs = uiState.recentSongs,
                                isPlaying = uiState.isPlaying,
                                currentSongId = uiState.currentSongId,
                                onPlaySong = viewModel::playSong,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onViewAllClick = onRecentClick
                            )
                        }
                    }
                }
            }
        }
    }
}

// 区块标题行：左侧可选图标 + 标题，右侧可选操作文案（默认「全部」）
@Composable
fun HomeSectionHeader(
    title: String,
    // 传 null 时不显示图标；彩色 PNG 请保持 iconTint 默认 Unspecified
    @DrawableRes iconRes: Int? = null,
    iconTint: Color = Color.Unspecified,
    // 右侧操作文案；仅在 onViewAllClick 非空时显示
    actionLabel: String = "全部",
    // 传 null 时不显示右侧操作
    onViewAllClick: (() -> Unit)? = null
) {
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
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                color = colorScheme.onBackground,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 25.5.sp,
                letterSpacing = (-0.34).sp
            )
        }
        if (onViewAllClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onViewAllClick
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

// 「最近播放」：与喜欢同款缩略图横滑（点选中，点中心钮播放）
@Composable
private fun HomeRecentThumbSection(
    songs: List<Song>,
    isPlaying: Boolean,
    currentSongId: Long?,
    onPlaySong: (Song, List<Song>) -> Unit,
    onTogglePlayPause: () -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return

    // 默认选中第一首（与 HomeFavoritesSection 对齐）
    var selectedIndex by remember(songs) { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val visibleIndices by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.map { it.index }.toSet()
        }
    }
    val safeIndex = selectedIndex.coerceIn(0, songs.lastIndex)

    // 新记录插入队首后，横滑列表滚回最前
    LaunchedEffect(songs.firstOrNull()?.id) {
        listState.animateScrollToItem(0)
    }

    Column(modifier = modifier) {
        HomeSectionHeader(
            title = "最近播放",
            iconRes = R.drawable.ic_recent_play,
            iconTint = colorScheme.onBackground,
            onViewAllClick = onViewAllClick
        )
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                FavoritesThumbnailItem(
                    song = song,
                    isSelected = index == safeIndex,
                    isPlayingThis = isPlaying && currentSongId == song.id,
                    isLazyAnimated = index in visibleIndices,
                    onSelectClick = { selectedIndex = index },
                    onPlayClick = {
                        if (isPlaying && currentSongId == song.id) onTogglePlayPause()
                        else onPlaySong(song, songs)
                    }
                )
            }
        }
    }
}

// 「我喜欢的」：仅缩略图横滑；选中后封面中心出现小播放钮，点播放钮才播
@Composable
private fun HomeFavoritesSection(
    songs: List<Song>,
    isPlaying: Boolean,
    currentSongId: Long?,
    onPlaySong: (Song, List<Song>) -> Unit,
    onTogglePlayPause: () -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return

    // 默认选中第一首
    var selectedIndex by remember(songs) { mutableIntStateOf(0) }
    var isAutoCarouselEnabled by remember {
        mutableStateOf(FavoritesAutoCarouselEnabled)
    }
    var idleResumeEpoch by remember { mutableIntStateOf(0) }
    val thumbnailListState = rememberLazyListState()
    val visibleIndices by remember {
        derivedStateOf {
            thumbnailListState.layoutInfo.visibleItemsInfo.map { it.index }.toSet()
        }
    }
    val safeIndex = selectedIndex.coerceIn(0, songs.lastIndex)

    val onUserInteraction: () -> Unit = {
        if (FavoritesAutoCarouselEnabled) {
            isAutoCarouselEnabled = false
            idleResumeEpoch++
        }
    }
    val onUserInteractionState = rememberUpdatedState(onUserInteraction)
    val thumbnailScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    onUserInteractionState.value()
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(idleResumeEpoch, songs) {
        if (!FavoritesAutoCarouselEnabled) return@LaunchedEffect
        if (idleResumeEpoch == 0) return@LaunchedEffect
        delay(FavoritesAutoCarouselResumeDelayMs)
        if (songs.size > 1) {
            isAutoCarouselEnabled = true
        }
    }

    LaunchedEffect(songs, isAutoCarouselEnabled) {
        if (!FavoritesAutoCarouselEnabled) return@LaunchedEffect
        if (!isAutoCarouselEnabled || songs.size <= 1) return@LaunchedEffect
        while (true) {
            delay(FavoritesAutoCarouselIntervalMs)
            val nextIndex = (selectedIndex + 1) % songs.size
            selectedIndex = nextIndex
            thumbnailListState.animateScrollToItem(nextIndex)
        }
    }

    Column(modifier = modifier) {
        HomeSectionHeader(
            title = "我喜欢的",
            iconRes = R.drawable.ic_heart2,
            iconTint = Color(0xFFFF4D6A),
            onViewAllClick = onViewAllClick
        )

        LazyRow(
            state = thumbnailListState,
            modifier = Modifier.nestedScroll(thumbnailScrollConnection),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                val isSelected = index == safeIndex
                FavoritesThumbnailItem(
                    song = song,
                    isSelected = isSelected,
                    isPlayingThis = isPlaying && currentSongId == song.id,
                    isLazyAnimated = index in visibleIndices,
                    onSelectClick = {
                        selectedIndex = index
                        onUserInteraction()
                    },
                    onPlayClick = {
                        onUserInteraction()
                        if (isPlaying && currentSongId == song.id) onTogglePlayPause()
                        else onPlaySong(song, songs)
                    }
                )
            }
        }
    }
}

// 缩略图：点封面选中；仅选中项中心显示小播放钮，点钮才播放/暂停
@Composable
private fun FavoritesThumbnailItem(
    song: Song,
    isSelected: Boolean,
    isPlayingThis: Boolean,
    isLazyAnimated: Boolean,
    onSelectClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val thumbTransition = if (isLazyAnimated) {
        tween<Float>(FavoritesThumbTransitionMs, easing = FastOutSlowInEasing)
    } else {
        snap()
    }

    val thumbSize by animateDpAsState(
        targetValue = if (isSelected) 62.dp else 54.dp,
        animationSpec = if (isLazyAnimated) {
            tween(FavoritesThumbTransitionMs, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "thumb_size"
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = thumbTransition,
        label = "thumb_ring_alpha"
    )
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0f else 0.4f,
        animationSpec = thumbTransition,
        label = "thumb_overlay_alpha"
    )
    val innerBorderColor = lerp(
        Color.White.copy(alpha = 0.12f),
        colorScheme.primary.copy(alpha = 0.7f),
        ringAlpha
    )

    Box(
        modifier = Modifier.size(ThumbOuterSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(thumbSize)
                .clip(ThumbShape)
                .border(width = 0.67.dp, color = innerBorderColor, shape = ThumbShape)
                .clickable(onClick = onSelectClick),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = rememberCoverRequest(song.coverUrl, 62.dp),
                contentDescription = song.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (overlayAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = overlayAlpha))
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF4F2FB))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPlayClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            if (isPlayingThis) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (isPlayingThis) "暂停" else "播放",
                        colorFilter = ColorFilter.tint(Color(0xFF0E0E10)),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

