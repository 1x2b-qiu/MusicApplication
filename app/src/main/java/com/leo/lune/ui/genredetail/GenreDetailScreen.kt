package com.leo.lune.ui.genredetail

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
private val GenreCoverSize = 108.dp

/**
 * 曲风详情页：布局对齐歌单详情（模糊顶背景 + 封面信息区 + 曲目列表），支持分页续拉。
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GenreDetailScreen(
    onBack: () -> Unit,
    viewModel: GenreDetailViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    // 底部留白：迷你播放栏 66dp + 导航层间距 12dp
    val miniPlayerBottomInset = 78.dp

    val coverUrl = uiState.coverUrl ?: uiState.songs.firstOrNull()?.coverUrl

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
        GenreDetailBackdrop(coverUrl = coverUrl)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            GenreDetailTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = miniPlayerBottomInset + 20.dp)
            ) {
                GenreDetailHero(
                    title = uiState.title,
                    coverUrl = coverUrl,
                    description = uiState.description,
                    stats = when {
                        !uiState.songCountLabel.isNullOrBlank() -> "${uiState.songCountLabel} 首"
                        uiState.songs.isNotEmpty() -> "${uiState.songs.size} 首"
                        else -> ""
                    },
                    canPlay = uiState.songs.isNotEmpty(),
                    isPlayingAll = uiState.hasStartedPlayAll && uiState.isPlaying,
                    onPlayAllClick = viewModel::onPlayAllClick
                )

                if (statusMessage != null) {
                    GenreDetailStatusText(
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
                            .padding(top = 16.dp)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 8.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.songs,
                            key = { _, song -> song.id }
                        ) { index, song ->
                            GenreDetailTrackRow(
                                index = index,
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
}

@Composable
private fun GenreDetailBackdrop(coverUrl: String?) {
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
            // 无封面时用淡色光斑，避免顶区一片死黑
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

// 顶栏对齐歌单详情：左返回、居中标题；右侧占位保持对称（无收藏）
@Composable
private fun GenreDetailTopBar(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GenreDetailHeaderIconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "风格详情",
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

@Composable
private fun GenreDetailHero(
    title: String,
    coverUrl: String?,
    description: String?,
    stats: String,
    canPlay: Boolean,
    isPlayingAll: Boolean,
    onPlayAllClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val playInteraction = remember { MutableInteractionSource() }
    val playScope = rememberCoroutineScope()
    // 点击时主动播缩小再回弹，对齐 ChartDetailInfo
    val playScale = remember { Animatable(1f) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(GenreCoverSize)
                    .shadow(18.dp, CoverShape, spotColor = Color.Black.copy(alpha = 0.45f))
                    .clip(CoverShape)
                    .background(colorScheme.surfaceVariant)
                    .border(1.dp, colorScheme.outlineVariant, CoverShape)
            ) {
                AsyncImage(
                    model = rememberCoverRequest(coverUrl, GenreCoverSize),
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

                // 始终占位，避免 stats 晚到时标题在居中块内上移
                Text(
                    text = stats.ifBlank { " " },
                    color = if (stats.isBlank()) {
                        Color.Transparent
                    } else {
                        colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    },
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 与歌单详情 / Liked 一致的大播放钮
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
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

// 列表单行：序号 + 封面 + 歌名/歌手专辑 + 时长（视觉对齐歌单详情）
@Composable
private fun GenreDetailTrackRow(
    index: Int,
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
            text = (index + 1).toString(),
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
private fun GenreDetailStatusText(
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
private fun GenreDetailHeaderIconButton(
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
