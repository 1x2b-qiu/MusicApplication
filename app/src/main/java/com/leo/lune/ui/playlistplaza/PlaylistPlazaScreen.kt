package com.leo.lune.ui.playlistplaza

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
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
import com.leo.lune.util.consumePointersUnlessResumed
import com.leo.lune.util.rememberCoverRequest
import kotlinx.coroutines.launch

private val CoverShape = RoundedCornerShape(14.dp)

// 歌单广场：Downloads 式顶栏 + 文字分类 Tab + 双列歌单网格
@Composable
fun PlaylistPlazaScreen(
    onBack: () -> Unit,
    viewModel: PlaylistPlazaViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 底部留白：迷你播放栏 66dp + 导航层间距 12dp
    val miniPlayerBottomInset = 78.dp
    val statusMessage = when {
        uiState.isLoading && uiState.playlists.isEmpty() -> "加载中…"
        uiState.error != null && uiState.playlists.isEmpty() -> uiState.error
        !uiState.isLoading && uiState.playlists.isEmpty() -> "暂无歌单"
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .consumePointersUnlessResumed()
            .padding(bottom = miniPlayerBottomInset)
    ) {
        PlaylistPlazaTopBar(onBack = onBack)

        CategoryTabRow(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            onCategorySelect = viewModel::onCategorySelect,
            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)
        )

        if (statusMessage != null) {
            PlaylistPlazaStatusText(
                text = statusMessage,
                actionLabel = "重试".takeIf { uiState.error != null },
                onAction = viewModel::onRetry.takeIf { uiState.error != null },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(uiState.playlists, key = { it.id }) { playlist ->
                    PlaylistPlazaCard(
                        playlist = playlist,
                        onOpenClick = { viewModel.onPlaylistClick(playlist.id) },
                        onPlayClick = { viewModel.onPlaylistPlayClick(playlist.id) }
                    )
                }
            }
        }
    }
}

// 顶栏对齐 DownloadsScreen：左返回、居中标题、右等宽占位
@Composable
private fun PlaylistPlazaTopBar(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaylistPlazaHeaderIconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "歌单广场",
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
private fun CategoryTabRow(
    categories: List<PlaylistPlazaCategory>,
    selectedCategoryId: String,
    onCategorySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(categories, key = { it.id }) { category ->
            val selected = category.id == selectedCategoryId
            CategoryTab(
                label = category.name,
                selected = selected,
                onClick = { onCategorySelect(category.id) }
            )
        }
    }
}

@Composable
private fun CategoryTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Text(
            text = label,
            color = if (selected) colorScheme.onBackground else colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = (-0.2).sp
        )
        Spacer(modifier = Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(
                    if (selected) colorScheme.onBackground else Color.Transparent
                )
        )
    }
}

@Composable
private fun PlaylistPlazaCard(
    playlist: PlaylistPlazaItem,
    onOpenClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val clickScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val playInteraction = remember { MutableInteractionSource() }
    val playScope = rememberCoroutineScope()
    val playScale = remember { Animatable(1f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    clickScope.launch {
                        scale.animateTo(0.96f, tween(60))
                        scale.animateTo(1f, tween(100))
                    }
                    onOpenClick()
                }
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CoverShape)
                .background(colorScheme.surfaceVariant)
                .border(1.dp, colorScheme.surfaceDim, CoverShape)
        ) {
            if (playlist.coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = rememberCoverRequest(playlist.coverUrl, 180.dp),
                    contentDescription = playlist.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
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
                            onPlayClick()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_play),
                    contentDescription = "播放歌单",
                    colorFilter = ColorFilter.tint(Color(0xFF0E0E10)),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = playlist.title,
                color = colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
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

@Composable
private fun PlaylistPlazaStatusText(
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
private fun PlaylistPlazaHeaderIconButton(
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
