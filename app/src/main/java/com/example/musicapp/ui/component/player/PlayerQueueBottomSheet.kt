package com.example.musicapp.ui.component.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.musicapp.domain.model.Song
import com.example.musicapp.ui.home.formatSongDuration
import com.example.musicapp.util.rememberCoverRequest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

private val SheetShape = RoundedCornerShape(34.dp)
private val CoverShape = RoundedCornerShape(12.dp)
private val RowShape = RoundedCornerShape(16.dp)
private val ClearConfirmShape = RoundedCornerShape(21.dp)
private val ClearConfirmRed = Color(0xFFF04444)
private val ClearConfirmEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/**
 * 播放队列底部弹层，对齐设计稿：
 * 标题 + 清空二次确认（底部红色按钮）+ 封面列表 + 当前曲音柱 + 移除单曲 + 空态
 *
 * sheetGesturesEnabled = false：禁止下滑关闭，点击遮罩 / 返回键仍可关闭。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerQueueBottomSheet(
    // 当前播放队列
    queue: List<Song>,
    // 正在播放的歌曲在队列中的下标
    currentIndex: Int,
    // 是否正在播放（用于当前曲音柱动画）
    isPlaying: Boolean,
    // 关闭弹层（点遮罩 / 返回键）
    onDismiss: () -> Unit,
    // 点击队列中某首歌，参数为下标
    onSongClick: (Int) -> Unit,
    // 从队列移除某首歌，参数为下标
    onRemoveSong: (Int) -> Unit,
    // 清空整个播放队列
    onClearQueue: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val showConfirm = remember { mutableStateOf(false) }

    // 打开时定位到当前曲，避免总是停在列表顶部
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentIndex.coerceIn(
            0,
            (queue.size - 1).coerceAtLeast(0)
        )
    )

    // 二次确认展示时拦截 Hidden，只取消确认、不收起弹层（避免半展开残留）
    // 闭包捕获 MutableState 引用，始终读到最新值
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.Hidden && showConfirm.value) {
                showConfirm.value = false
                false
            } else {
                true
            }
        }
    )

    ModalBottomSheet(
        // 遮罩收起时若确认态被拦截，Sheet 仍可见：只取消确认，不拆弹层
        onDismissRequest = {
            if (sheetState.isVisible) {
                showConfirm.value = false
            } else {
                onDismiss()
            }
        },
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        shape = SheetShape,
        containerColor = colorScheme.background,
        scrimColor = colorScheme.scrim.copy(alpha = 0.55f),
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colorScheme.primary, SheetShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "播放队列",
                            color = colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "共 ${queue.size} 首",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (queue.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colorScheme.surfaceVariant)
                                .border(0.67.dp, colorScheme.outlineVariant, CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { showConfirm.value = !showConfirm.value }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "清空播放队列",
                                tint = colorScheme.onBackground,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (queue.isEmpty()) {
                    QueueEmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 44.dp)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
                            QueueSongRow(
                                index = index,
                                song = song,
                                isCurrent = index == currentIndex,
                                isPlaying = isPlaying && index == currentIndex,
                                onClick = { onSongClick(index) },
                                onRemove = { onRemoveSong(index) }
                            )
                        }
                    }
                }
            }

            // 确认清空时给队列内容加遮罩；点击遮罩取消确认
            QueueConfirmScrim(
                visible = showConfirm.value && queue.isNotEmpty(),
                onDismissConfirm = { showConfirm.value = false },
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(1f)
            )

            // 设计稿二次确认：底部红色「确认清空」上滑出现
            ClearQueueConfirmButton(
                visible = showConfirm.value && queue.isNotEmpty(),
                onConfirm = {
                    showConfirm.value = false
                    onClearQueue()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(2f)
            )
        }
    }
}

@Composable
private fun QueueConfirmScrim(
    visible: Boolean,
    onDismissConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.scrim.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissConfirm
                )
        )
    }
}

@Composable
private fun ClearQueueConfirmButton(
    visible: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = tween(320, easing = ClearConfirmEasing),
            initialOffsetY = { it }
        ) + fadeIn(tween(320, easing = ClearConfirmEasing)),
        exit = slideOutVertically(
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            targetOffsetY = { it / 2 }
        ) + fadeOut(tween(220))
    ) {
        Text(
            text = "确认清空",
            color = Color.Black,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = ClearConfirmShape,
                    ambientColor = ClearConfirmRed.copy(alpha = 0.28f),
                    spotColor = ClearConfirmRed.copy(alpha = 0.28f)
                )
                .clip(ClearConfirmShape)
                .background(ClearConfirmRed, ClearConfirmShape)
                .border(1.dp, colorScheme.primary, ClearConfirmShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onConfirm
                )
                .padding(vertical = 16.dp)
        )
    }
}

@Composable
private fun QueueEmptyState(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colorScheme.onBackground.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                contentDescription = null,
                tint = colorScheme.onBackground.copy(alpha = 0.45f),
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = "队列空空如也",
            color = colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "选择歌曲后会出现在这里",
            color = colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun QueueSongRow(
    index: Int,
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .then(
                if (isCurrent) {
                    Modifier.background(colorScheme.secondaryContainer)
                } else {
                    Modifier
                }
            )
            .clickable(
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        AsyncImage(
            model = rememberCoverRequest(song.coverUrl, 44.dp),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(CoverShape)
                .background(colorScheme.secondaryContainer),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artists,
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Box(
            modifier = Modifier.width(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                PlayingEqualizer(color = colorScheme.onBackground)
            }
        }
        Text(
            text = formatSongDuration(song.durationMs),
            color = colorScheme.onBackground.copy(alpha = 0.35f),
            fontSize = 12.sp
        )

    }
}

/** 设计稿当前播放指示：三根错峰脉冲音柱 */
@Composable
private fun PlayingEqualizer(color: Color) {
    val transition = rememberInfiniteTransition(label = "queue-eq")
    val bar1 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq-1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq-2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq-3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(12.dp)
    ) {
        EqualizerBar(color = color, heightFraction = bar1, maxHeight = 12.dp)
        EqualizerBar(color = color, heightFraction = bar2, maxHeight = 12.dp)
        EqualizerBar(color = color, heightFraction = bar3, maxHeight = 12.dp)
    }
}

@Composable
private fun EqualizerBar(
    color: Color,
    heightFraction: Float,
    maxHeight: Dp
) {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(maxHeight)
            .graphicsLayer {
                scaleY = heightFraction.coerceIn(0.2f, 1f)
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .background(color, RoundedCornerShape(1.dp))
    )
}
