package com.example.musicapp.ui.downloads

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.musicapp.controller.ActiveDownloadTask
import com.example.musicapp.domain.model.DownloadedSong
import com.example.musicapp.ui.home.formatSongDuration
import com.example.musicapp.util.rememberCoverRequest

private val CoverShape = RoundedCornerShape(12.dp)
private val CardShape = RoundedCornerShape(24.dp)
private val EmptyIconShape = RoundedCornerShape(28.dp)

// 设计稿空态图标：托盘 + 音符（细线描边）
private const val EmptyIconTrayPath =
    "M5.25 14.25v3.375A2.625 2.625 0 007.875 20.25h8.25a2.625 2.625 0 002.625-2.625V14.25M4.5 14.25h15"
private const val EmptyIconNotesPath =
    "M14.25 5.25v7.125a2.625 2.625 0 11-1.5-2.385V6.375l5.25-1.5V10.5a2.625 2.625 0 11-1.5-2.385V3.75l-3.75 1.5"

// 本地下载页：顶栏居中标题；下载中 / 已下载圆角卡片；全空时空态
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tasks = uiState.activeTasks
    val downloaded = uiState.downloadedSongs
    val hasContent = tasks.isNotEmpty() || downloaded.isNotEmpty()
    // 底部迷你播放栏预留
    val miniPlayerBottomInset = 78.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(bottom = miniPlayerBottomInset)
    ) {
        DownloadsTopBar(onBack = onBack)

        if (!hasContent) {
            DownloadsEmptyState(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                if (tasks.isNotEmpty()) {
                    SectionHeader(
                        title = "正在下载",
                        trailing = "${tasks.size} 首"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DownloadsCard {
                        tasks.forEach { task ->
                            ActiveDownloadRow(
                                task = task,
                                onCancel = { viewModel.cancelDownload(task.songId) }
                            )
                        }
                    }
                }

                if (downloaded.isNotEmpty()) {
                    if (tasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                    SectionHeader(
                        title = "已下载",
                        trailing = "${downloaded.size} 首 · ${formatFileSize(uiState.totalSizeBytes)}"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DownloadsCard {
                        downloaded.forEach { song ->
                            DownloadedSongRow(
                                song = song,
                                onClick = { viewModel.playSong(song) },
                                onDelete = { viewModel.deleteDownload(song.songId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsTopBar(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp)
            .height(46.dp)
    ) {
        DownloadsBackButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Text(
            text = "本地下载",
            modifier = Modifier.align(Alignment.Center),
            color = colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp
        )
    }
}

@Composable
private fun DownloadsEmptyState(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    // 与设计稿 min-h-[52vh] 对齐，在顶栏下方居中
    val minHeight = (LocalConfiguration.current.screenHeightDp * 0.52f).dp

    Column(
        modifier = modifier
            .heightIn(min = minHeight)
            .padding(horizontal = 32.dp)
            .padding(bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 容器表面与列表卡片一致：surfaceVariant + outline，外加设计稿柔和投影
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = EmptyIconShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.16f)
                )
                .clip(EmptyIconShape)
                .background(colorScheme.surfaceVariant)
                .border(0.67.dp, colorScheme.outlineVariant, EmptyIconShape),
            contentAlignment = Alignment.Center
        ) {
            // 顶部内高光，贴近设计稿 inset 高光
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colorScheme.onBackground.copy(alpha = 0.1f))
            )
            DownloadsEmptyMusicIcon(
                tint = colorScheme.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "还没有本地音乐",
            color = colorScheme.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

// 设计稿空态线框图标（托盘 + 双音符）
@Composable
private fun DownloadsEmptyMusicIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    val trayPath = remember {
        PathParser().parsePathString(EmptyIconTrayPath).toPath()
    }
    val notesPath = remember {
        PathParser().parsePathString(EmptyIconNotesPath).toPath()
    }

    Canvas(modifier = modifier) {
        val stroke = Stroke(
            width = 1.3.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val scaleFactor = size.minDimension / 24f
        scale(
            scaleX = scaleFactor,
            scaleY = scaleFactor,
            pivot = Offset.Zero
        ) {
            drawPath(path = trayPath, color = tint, style = stroke)
            drawPath(path = notesPath, color = tint, style = stroke)
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    trailing: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = colorScheme.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = trailing,
            color = colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun DownloadsCard(
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colorScheme.surfaceVariant)
            .border(0.67.dp, colorScheme.outlineVariant, CardShape)
    ) {
        content()
    }
}

@Composable
private fun ActiveDownloadRow(
    task: ActiveDownloadTask,
    onCancel: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val progressPercent = (task.progress * 100f).toInt().coerceIn(0, 100)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = rememberCoverRequest(task.coverUrl, 48.dp),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CoverShape),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    color = colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$progressPercent%",
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Text(
                text = task.artist,
                modifier = Modifier.padding(top = 2.dp),
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colorScheme.onBackground.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(task.progress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colorScheme.onBackground.copy(alpha = 0.7f))
                )
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colorScheme.onBackground.copy(alpha = 0.06f))
                .semantics { contentDescription = "取消下载 ${task.title}" }
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .border(1.dp, colorScheme.onSurfaceVariant, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun DownloadedSongRow(
    song: DownloadedSong,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var menuOpen by remember(song.songId) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = rememberCoverRequest(song.coverUrl, 44.dp),
                    contentDescription = "${song.name} 封面",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CoverShape),
                    contentScale = ContentScale.Crop
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
                        text = "${song.artists} · ${formatSongDuration(song.durationMs)}",
                        modifier = Modifier.padding(top = 2.dp),
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { menuOpen = !menuOpen }
                    .semantics { contentDescription = "${song.name} 更多操作" },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreHoriz,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (menuOpen) {
            Popup(
                alignment = Alignment.TopEnd,
                onDismissRequest = { menuOpen = false },
                properties = PopupProperties(focusable = true)
            ) {
                Text(
                    text = "删除下载",
                    modifier = Modifier
                        .padding(top = 52.dp, end = 12.dp)
                        .shadow(12.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.onBackground)
                        .clickable {
                            menuOpen = false
                            onDelete()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = colorScheme.background,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DownloadsBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(colorScheme.surfaceVariant)
            .border(0.67.dp, colorScheme.outlineVariant, CircleShape)
            .semantics { contentDescription = "返回" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            contentDescription = null,
            tint = colorScheme.onBackground,
            modifier = Modifier.size(20.dp)
        )
    }
}

// 字节数格式化为 MB 文案
private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 10.0) {
        "${mb.toInt()} MB"
    } else {
        String.format("%.1f MB", mb)
    }
}
