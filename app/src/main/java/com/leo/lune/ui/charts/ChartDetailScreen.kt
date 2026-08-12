package com.leo.lune.ui.charts

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.leo.lune.R
import com.leo.lune.domain.model.Song
import com.leo.lune.ui.home.formatSongDuration
import com.leo.lune.util.consumePointersUnlessResumed
import com.leo.lune.util.rememberCoverRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
private val CoverShape = RoundedCornerShape(20.dp)
private val ChartCoverSize = 108.dp
private val PodiumCoverShape = RoundedCornerShape(16.dp)

/**
 * 排行榜详情：顶栏 + 歌单详情式信息区固定；前三领奖台与列表一并滚动。
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ChartDetailScreen(
    onBack: () -> Unit,
    viewModel: ChartDetailViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val miniPlayerBottomInset = 78.dp

    val topSongs = uiState.songs.take(3)
    val restSongs = if (uiState.songs.size > 3) uiState.songs.drop(3) else emptyList()
    val backdropCover = uiState.coverUrl ?: topSongs.firstOrNull()?.coverUrl

    // 接近底部时续拉
    LaunchedEffect(listState, uiState.hasMore, uiState.isLoadingMore, uiState.songs.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = info.totalItemsCount
            total > 0 && lastVisible >= total - 4
        }
            .distinctUntilChanged()
            .collect { nearEnd ->
                if (nearEnd) viewModel.onLoadMore()
            }
    }

    val statusMessage = when {
        uiState.isLoading && uiState.songs.isEmpty() -> "加载中…"
        uiState.error != null && uiState.songs.isEmpty() -> uiState.error
        !uiState.isLoading && uiState.songs.isEmpty() -> "暂无歌曲"
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .consumePointersUnlessResumed()
    ) {
        ChartDetailBackdrop(coverUrl = backdropCover)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = miniPlayerBottomInset)
        ) {
            ChartDetailTopBar(onBack = onBack)

            if (statusMessage != null) {
                ChartDetailStatusText(
                    text = statusMessage,
                    actionLabel = "重试".takeIf {
                        uiState.error != null && uiState.songs.isEmpty()
                    },
                    onAction = viewModel::onRetry.takeIf {
                        uiState.error != null && uiState.songs.isEmpty()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                ChartDetailInfo(
                    title = uiState.title,
                    subtitle = uiState.subtitle,
                    coverUrl = uiState.coverUrl,
                    trackCount = uiState.trackCount,
                    creatorName = uiState.creatorName,
                    creatorAvatarUrl = uiState.creatorAvatarUrl,
                    description = uiState.description,
                    tags = uiState.tags,
                    canPlay = uiState.songs.isNotEmpty(),
                    isPlayingAll = uiState.hasStartedPlayAll && uiState.isPlaying,
                    onPlayAllClick = viewModel::onPlayAllClick,
                    modifier = Modifier.padding(top = 8.dp)
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    if (topSongs.isNotEmpty()) {
                        item(key = "podium") {
                            ChartPodium(
                                songs = topSongs,
                                onSongClick = viewModel::onSongClick,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                    itemsIndexed(
                        items = restSongs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        ChartDetailTrackRow(
                            rank = index + 4,
                            song = song,
                            onClick = { viewModel.onSongClick(song) }
                        )
                    }
                    if (uiState.isLoadingMore) {
                        item(key = "loading_more") {
                            Text(
                                text = "加载更多…",
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartDetailBackdrop(coverUrl: String?) {
    val colorScheme = MaterialTheme.colorScheme
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = rememberCoverRequest(coverUrl, screenWidth),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.35f
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp)
                    .size(176.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.background.copy(alpha = 0.2f),
                            colorScheme.background.copy(alpha = 0.72f),
                            colorScheme.background
                        )
                    )
                )
        )
    }
}

@Composable
private fun ChartDetailTopBar(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChartDetailHeaderIconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "榜单详情",
            modifier = Modifier.weight(1f),
            color = colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.3).sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(36.dp))
    }
}

// 信息区对齐 PlaylistDetailHero：封面 + 创建者 + 标签/曲数 + 介绍 + 圆形播放钮
@Composable
private fun ChartDetailInfo(
    title: String,
    subtitle: String,
    coverUrl: String?,
    trackCount: Int,
    creatorName: String?,
    creatorAvatarUrl: String?,
    description: String?,
    tags: List<String>,
    canPlay: Boolean,
    isPlayingAll: Boolean,
    onPlayAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val ownerName = creatorName.orEmpty()
    val mood = subtitle.takeIf { it.isNotBlank() }
        ?: tags.takeIf { it.isNotEmpty() }?.joinToString(" · ").orEmpty()
    val stats = if (trackCount > 0) "$trackCount 首" else ""
    val playInteraction = remember { MutableInteractionSource() }
    val playScope = rememberCoroutineScope()
    // 点击时主动播缩小再回弹，对齐 FavoritesMainCard
    val playScale = remember { Animatable(1f) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(ChartCoverSize)
                    .shadow(18.dp, CoverShape, spotColor = Color.Black.copy(alpha = 0.45f))
                    .clip(CoverShape)
                    .background(colorScheme.surfaceVariant)
                    .border(1.dp, colorScheme.outlineVariant, CoverShape)
            ) {
                AsyncImage(
                    model = rememberCoverRequest(coverUrl, ChartCoverSize),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifBlank { " " },
                    color = colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.6).sp,
                    lineHeight = 24.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (ownerName.isNotEmpty() || creatorAvatarUrl != null) {
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncImage(
                            model = rememberCoverRequest(creatorAvatarUrl, 22.dp),
                            contentDescription = null,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(colorScheme.surfaceVariant)
                                .border(0.67.dp, colorScheme.outlineVariant, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = ownerName,
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (mood.isNotEmpty() || stats.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (mood.isNotEmpty()) {
                            Text(
                                text = mood,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        if (mood.isNotEmpty() && stats.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                            )
                        }
                        if (stats.isNotEmpty()) {
                            Text(
                                text = stats,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(47.dp)
                    .scale(playScale.value)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFF4F2FB))
                    .clickable(
                        enabled = canPlay,
                        interactionSource = playInteraction,
                        indication = null,
                        onClick = {
                            playScope.launch {
                                playScale.animateTo(0.9f, tween(60))
                                playScale.animateTo(1f, tween(100))
                            }
                            onPlayAllClick()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        if (isPlayingAll) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                    contentDescription = if (isPlayingAll) "暂停" else "播放全部",
                    colorFilter = ColorFilter.tint(Color(0xFF0E0E10)),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                color = colorScheme.onBackground,
                fontSize = 13.sp,
                lineHeight = 22.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun ChartPodium(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val first = songs.getOrNull(0)
    val second = songs.getOrNull(1)
    val third = songs.getOrNull(2)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (second != null) {
                PodiumSlot(
                    song = second,
                    rank = 2,
                    coverSize = 88.dp,
                    onClick = { onSongClick(second) }
                )
            }
        }
        Box(
            modifier = Modifier.weight(1.15f),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (first != null) {
                PodiumSlot(
                    song = first,
                    rank = 1,
                    coverSize = 124.dp,
                    highlight = true,
                    onClick = { onSongClick(first) }
                )
            }
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (third != null) {
                PodiumSlot(
                    song = third,
                    rank = 3,
                    coverSize = 88.dp,
                    onClick = { onSongClick(third) }
                )
            }
        }
    }
}

@Composable
private fun PodiumSlot(
    song: Song,
    rank: Int,
    coverSize: Dp,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(0.94f, onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(coverSize)
                .then(
                    if (highlight) {
                        Modifier.shadow(
                            20.dp,
                            PodiumCoverShape,
                            spotColor = Color.Black.copy(alpha = 0.5f)
                        )
                    } else {
                        Modifier
                    }
                )
                .clip(PodiumCoverShape)
                .background(colorScheme.surfaceVariant)
                .border(
                    width = if (highlight) 1.5.dp else 1.dp,
                    color = if (highlight) {
                        Color.White.copy(alpha = 0.35f)
                    } else {
                        colorScheme.surfaceDim
                    },
                    shape = PodiumCoverShape
                )
        ) {
            AsyncImage(
                model = rememberCoverRequest(song.coverUrl, coverSize),
                contentDescription = song.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Text(
                text = rank.toString(),
                color = Color.White.copy(alpha = if (highlight) 0.36f else 0.3f),
                fontSize = if (highlight) 72.sp else 48.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = if (highlight) 72.sp else 48.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Text(
            text = song.name,
            color = colorScheme.onBackground,
            fontSize = if (highlight) 14.sp else 12.sp,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )
        Text(
            text = song.artists,
            color = colorScheme.onSurfaceVariant,
            fontSize = if (highlight) 11.sp else 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp)
        )
    }
}

@Composable
private fun ChartDetailTrackRow(
    rank: Int,
    song: Song,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            color = colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Start,
            modifier = Modifier.width(20.dp)
        )
        AsyncImage(
            model = rememberCoverRequest(song.coverUrl, 43.dp),
            contentDescription = song.name,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(43.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 9.dp)
        ) {
            Text(
                text = song.name,
                color = colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(song.artists)
                    if (song.album.isNotBlank()) {
                        append(" · ")
                        append(song.album)
                    }
                },
                color = colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        Text(
            text = formatSongDuration(song.durationMs),
            color = colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 9.dp)
        )
    }
}

@Composable
private fun ChartDetailStatusText(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                color = colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clickable(onClick = onAction)
            )
        }
    }
}

@Composable
private fun ChartDetailHeaderIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colorScheme.surfaceVariant)
            .border(0.67.dp, colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun Modifier.pressScaleClickable(
    pressedScale: Float,
    onClick: () -> Unit
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    return this
        .scale(scale.value)
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = {
                scope.launch {
                    scale.animateTo(pressedScale, tween(60))
                    scale.animateTo(1f, tween(100))
                }
                onClick()
            }
        )
}
