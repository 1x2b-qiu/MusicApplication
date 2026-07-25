package com.leo.lune.ui.identify

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.leo.lune.domain.model.Song
import com.leo.lune.permission.AppPermission
import com.leo.lune.permission.PermissionCoordinator
import com.leo.lune.ui.home.formatSongDuration
import com.leo.lune.util.consumePointersUnlessResumed
import com.leo.lune.util.rememberCoverRequest

private val SurfaceShape = RoundedCornerShape(28.dp)
private val OthersShape = RoundedCornerShape(24.dp)
private val CoverShape = RoundedCornerShape(16.dp)
private val Grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

private val WaveBarHeights = listOf(
    18, 31, 45, 27, 52, 36, 58, 24, 42, 55, 31, 49, 22, 39, 57, 32, 46, 28, 51, 35, 20
)

// 听歌识曲页：对齐设计稿；录 8 秒自动提交 AudD
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun IdentifyScreen(
    onBack: () -> Unit,
    darkTheme: Boolean,
    permissions: PermissionCoordinator,
    viewModel: IdentifyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .consumePointersUnlessResumed()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            IdentifyTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (uiState.phase) {
                    IdentifyPhase.Idle,
                    IdentifyPhase.Listening,
                    IdentifyPhase.Processing -> {
                        ListeningContent(
                            phase = uiState.phase,
                            elapsedSeconds = uiState.elapsedSeconds,
                            darkTheme = darkTheme,
                            onPrimaryClick = {
                                if (uiState.phase == IdentifyPhase.Idle) {
                                    if (permissions.isGranted(AppPermission.RecordAudio)) {
                                        viewModel.onPrimaryAction()
                                    } else {
                                        permissions.request(AppPermission.RecordAudio) { granted ->
                                            if (granted) viewModel.onPrimaryAction()
                                        }
                                    }
                                } else {
                                    viewModel.onPrimaryAction()
                                }
                            }
                        )
                    }
                    IdentifyPhase.Success -> {
                        SuccessContent(
                            uiState = uiState,
                            darkTheme = darkTheme,
                            onPlayPrimary = viewModel::playPrimary,
                            onPlaySong = viewModel::playSong,
                            onToggleOthers = viewModel::toggleOthersExpanded,
                            onRetry = {
                                if (permissions.isGranted(AppPermission.RecordAudio)) {
                                    viewModel.retry()
                                } else {
                                    permissions.request(AppPermission.RecordAudio) { granted ->
                                        if (granted) viewModel.retry()
                                    }
                                }
                            }
                        )
                    }
                    IdentifyPhase.Failure -> {
                        FailureContent(
                            message = uiState.failureMessage,
                            darkTheme = darkTheme,
                            onRetry = {
                                if (permissions.isGranted(AppPermission.RecordAudio)) {
                                    viewModel.retry()
                                } else {
                                    permissions.request(AppPermission.RecordAudio) { granted ->
                                        if (granted) viewModel.retry()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// 顶栏布局与本地下载页一致：top 14dp + 行高 46dp，返回按钮垂直居中
@Composable
private fun IdentifyTopBar(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IdentifyHeaderIconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "听歌识曲",
            modifier = Modifier.weight(1f),
            color = colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.3).sp,
            textAlign = TextAlign.Center
        )
        // 与左侧返回按钮等宽，保证标题视觉居中
        Spacer(modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun IdentifyHeaderIconButton(
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
private fun ListeningContent(
    phase: IdentifyPhase,
    elapsedSeconds: Int,
    darkTheme: Boolean,
    onPrimaryClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val active = phase == IdentifyPhase.Listening || phase == IdentifyPhase.Processing

    Spacer(modifier = Modifier.height(64.dp))

    Box(
        modifier = Modifier.size(224.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = if (active) {
                        if (darkTheme) Color.White.copy(alpha = 0.24f)
                        else Color.Black.copy(alpha = 0.20f)
                    } else {
                        if (darkTheme) Color.White.copy(alpha = 0.15f)
                        else Color.Black.copy(alpha = 0.12f)
                    },
                    shape = CircleShape
                )
                .background(
                    if (active) {
                        if (darkTheme) Color.White else Color.Black
                    } else {
                        if (darkTheme) Color.White.copy(alpha = 0.10f)
                        else Color.White.copy(alpha = 0.70f)
                    }
                )
                .clickable(
                    enabled = phase != IdentifyPhase.Processing,
                    onClick = onPrimaryClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (active) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (darkTheme) Color.Black else Color.White)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.MicNone,
                    contentDescription = "开始识别",
                    tint = if (darkTheme) Color.White else Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Box(
        modifier = Modifier.height(if (active) 40.dp else 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            WaveformBars(darkTheme = darkTheme)
        }
    }

    Text(
        text = when (phase) {
            IdentifyPhase.Listening -> "正在聆听周围的声音"
            IdentifyPhase.Processing -> "识别中"
            else -> "点击开始识别"
        },
        color = colorScheme.onBackground,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 8.dp)
    )

    if (phase == IdentifyPhase.Listening) {
        Text(
            text = formatElapsed(elapsedSeconds),
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}


@Composable
private fun WaveformBars(darkTheme: Boolean) {
    val transition = rememberInfiniteTransition(label = "wave")
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(40.dp)
    ) {
        WaveBarHeights.forEachIndexed { index, heightPct ->
            val anim by transition.animateFloat(
                initialValue = 0.36f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 780,
                        delayMillis = (index * 70),
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((40.dp * (heightPct / 100f) * anim).coerceAtLeast(4.dp))
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (darkTheme) Color.White.copy(alpha = 0.75f)
                        else Color.Black.copy(alpha = 0.65f)
                    )
            )
        }
    }
}

@Composable
private fun SuccessContent(
    uiState: IdentifyUiState,
    darkTheme: Boolean,
    onPlayPrimary: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onToggleOthers: () -> Unit,
    onRetry: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Spacer(modifier = Modifier.height(24.dp))

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (darkTheme) Color.White else Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = if (darkTheme) Color.Black else Color.White,
            modifier = Modifier.size(24.dp)
        )
    }

    Text(
        text = "识别成功",
        color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 16.dp)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .clip(SurfaceShape)
            .border(1.dp, colorScheme.surfaceDim, SurfaceShape)
            .background(colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = rememberCoverRequest(uiState.songs.firstOrNull()?.coverUrl, 80.dp),
                contentDescription = uiState.songs.firstOrNull()?.name
                    ?: uiState.track?.title.orEmpty(),
                modifier = Modifier
                    .size(80.dp)
                    .clip(CoverShape)
                    .background(Color.White.copy(alpha = 0.10f)),
                contentScale = ContentScale.Crop,
                colorFilter = Grayscale
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = (uiState.songs.firstOrNull()?.name
                        ?: uiState.track?.title.orEmpty()).ifBlank { "未知歌曲" },
                    color = colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = (uiState.songs.firstOrNull()?.artists
                        ?: uiState.track?.artist.orEmpty()).ifBlank { "未知歌手" },
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (uiState.songs.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (darkTheme) Color.White else Color.Black)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPlayPrimary
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "播放",
                        tint = if (darkTheme) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(1.dp)
                .background(
                    if (darkTheme) Color.White.copy(alpha = 0.10f)
                    else Color.Black.copy(alpha = 0.08f)
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "识别片段 · ${formatElapsed(uiState.recordedSeconds)}",
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                fontSize = 12.sp
            )
            Text(
                text = "已为你找到这首歌",
                color = colorScheme.onBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (uiState.songs.size > 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleOthers
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "其他识曲结果 ${uiState.songs.size - 1}",
                color = colorScheme.onBackground,
                fontSize = 14.sp
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .size(16.dp)
                    .rotate(if (uiState.othersExpanded) 90f else 0f)
            )
        }

        AnimatedVisibility(
            visible = uiState.othersExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(OthersShape)
                    .border(1.dp, colorScheme.surfaceDim, OthersShape)
                    .background(colorScheme.surfaceVariant)
            ) {
                uiState.songs.drop(1).forEach { song ->
                    OtherResultRow(
                        song = song,
                        onClick = { onPlaySong(song) }
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(28.dp))

    RetryChip(
        label = "重新识别",
        filled = false,
        darkTheme = darkTheme,
        onClick = onRetry
    )
}

@Composable
private fun OtherResultRow(
    song: Song,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = rememberCoverRequest(song.coverUrl, 44.dp),
            contentDescription = song.name,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
            colorFilter = Grayscale
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artists,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatSongDuration(song.durationMs),
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun FailureContent(
    message: String?,
    darkTheme: Boolean,
    onRetry: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Spacer(modifier = Modifier.height(80.dp))

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(if (darkTheme) Color.White else Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = null,
            tint = if (darkTheme) Color.Black else Color.White,
            modifier = Modifier.size(32.dp)
        )
    }

    Text(
        text = "没有识别到歌曲",
        color = colorScheme.onBackground,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 24.dp)
    )
    Text(
        text = message ?: "请靠近音源后再试一次",
        color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp)
    )

    Spacer(modifier = Modifier.height(32.dp))

    RetryChip(
        label = "再次识别",
        filled = true,
        darkTheme = darkTheme,
        onClick = onRetry
    )
}

@Composable
private fun RetryChip(
    label: String,
    filled: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit,
) {
    val bg = when {
        filled && darkTheme -> Color.White
        filled && !darkTheme -> Color.Black
        darkTheme -> Color.White.copy(alpha = 0.10f)
        else -> Color.Black.copy(alpha = 0.06f)
    }
    val fg = when {
        filled && darkTheme -> Color.Black
        filled && !darkTheme -> Color.White
        else -> if (darkTheme) Color.White else Color.Black
    }

    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = fg,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatElapsed(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "00:%02d".format(safe)
}
