package com.leo.lune.ui.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.leo.lune.util.consumePointersUnlessResumed
import com.leo.lune.util.rememberCoverRequest
import kotlinx.coroutines.launch

private val HeroShape = RoundedCornerShape(18.dp)

// 排行榜总览：官方榜单均使用 Hero 大卡竖向列表
@Composable
fun ChartsScreen(
    onBack: () -> Unit,
    onChartClick: (Long) -> Unit = {},
    viewModel: ChartsViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val miniPlayerBottomInset = 78.dp
    val statusMessage = when {
        uiState.isLoading && uiState.charts.isEmpty() -> "加载中…"
        uiState.error != null && uiState.charts.isEmpty() -> uiState.error
        !uiState.isLoading && uiState.charts.isEmpty() -> "暂无榜单"
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
        ChartsTopBar(onBack = onBack)

        if (statusMessage != null) {
            ChartsStatusText(
                text = statusMessage,
                actionLabel = "重试".takeIf { uiState.error != null },
                onAction = viewModel::onRetry.takeIf { uiState.error != null },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(uiState.charts, key = { it.id }) { chart ->
                    ChartsHeroCard(
                        chart = chart,
                        onClick = { onChartClick(chart.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartsTopBar(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChartsHeaderIconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "排行榜",
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
private fun ChartsHeroCard(
    chart: ChartsHubItem,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val topCoverUrl = chart.songs.firstOrNull()?.coverUrl.orEmpty()
    val previewSongs = chart.songs.take(3)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(HeroShape)
            .border(1.dp, colorScheme.surfaceDim, HeroShape)
            .pressScaleClickable(0.97f, onClick)
    ) {
        if (topCoverUrl.isNotEmpty()) {
            AsyncImage(
                model = rememberCoverRequest(topCoverUrl, 360.dp),
                contentDescription = chart.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.surfaceVariant)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.15f),
                            0.35f to Color.Black.copy(alpha = 0.08f),
                            0.7f to Color.Black.copy(alpha = 0.55f),
                            1.0f to Color.Black.copy(alpha = 0.82f)
                        )
                    )
                )
        )

        Text(
            text = "1",
            color = Color.White.copy(alpha = 0.18f),
            fontSize = 120.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 120.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 10.dp, top = 0.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = chart.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chart.subtitle,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (previewSongs.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    previewSongs.forEachIndexed { index, song ->
                        Text(
                            text = "${index + 1}  ${song.title}  ·  ${song.artist}",
                            color = if (index == 0) {
                                Color.White.copy(alpha = 0.92f)
                            } else {
                                Color.White.copy(alpha = 0.7f)
                            },
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartsStatusText(
    text: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = actionLabel,
                color = colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

@Composable
private fun ChartsHeaderIconButton(
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
