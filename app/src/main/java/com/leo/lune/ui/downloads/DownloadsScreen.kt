package com.leo.lune.ui.downloads

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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
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
import com.leo.lune.R
import com.leo.lune.controller.ActiveDownloadTask
import com.leo.lune.domain.model.DownloadedSong
import com.leo.lune.ui.home.formatSongDuration
import com.leo.lune.util.formatFileSize
import com.leo.lune.util.rememberCoverRequest

// 封面缩略图圆角
private val CoverShape = RoundedCornerShape(12.dp)

// 下载列表外层卡片圆角
private val CardShape = RoundedCornerShape(24.dp)

// 空态图标容器圆角
private val EmptyIconShape = RoundedCornerShape(28.dp)

// 本地下载页：顶栏居中标题；下载中 / 已下载圆角卡片；全空时空态
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 底部留白：迷你播放栏 66dp + 导航层间距 12dp
    val miniPlayerBottomInset = 78.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(bottom = miniPlayerBottomInset)
    ) {
        DownloadsTopBar(onBack = onBack)

        // 无进行中下载且无已下载文件时展示空态
        if (uiState.activeTasks.isEmpty() && uiState.downloadedSongs.isEmpty()) {
            DownloadsEmptyState(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            // 有内容时：分区展示「正在下载」与「已下载」
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                if (uiState.activeTasks.isNotEmpty()) {
                    SectionHeader(
                        title = "正在下载",
                        trailing = "${uiState.activeTasks.size} 首"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DownloadsCard {
                        uiState.activeTasks.forEach { task ->
                            ActiveDownloadRow(
                                task = task,
                                onTogglePause = { viewModel.togglePauseDownload(task.songId) },
                                onCancel = { viewModel.cancelDownload(task.songId) }
                            )
                        }
                    }
                }

                if (uiState.downloadedSongs.isNotEmpty()) {
                    // 两区同时存在时加大间距
                    if (uiState.activeTasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    SectionHeader(
                        title = "已下载",
                        trailing = "${uiState.downloadedSongs.size} 首 · ${formatFileSize(uiState.totalSizeBytes)}"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DownloadsCard {
                        uiState.downloadedSongs.forEach { song ->
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

// 顶栏布局与 SearchScreen 一致：top 14dp + 行高 46dp，返回按钮垂直居中
@Composable
private fun DownloadsTopBar(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DownloadsHeaderIconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "本地下载",
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

// 全空态：居中图标 +「还没有本地音乐」文案
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
        Icon(
            painter = painterResource(R.drawable.ic_downloads_empty),
            contentDescription = null,
            tint = colorScheme.onBackground,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "还没有本地音乐",
            color = colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

// 分区标题行：左侧标题 + 右侧统计文案
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

// 列表外层卡片外壳（surfaceVariant + 细描边）
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

// 进行中下载单行：封面 + 暂停/继续 + 删除；进度条在下方（与设计稿一致）
@Composable
private fun ActiveDownloadRow(
    task: ActiveDownloadTask,
    onTogglePause: () -> Unit,
    onCancel: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val progressPercent = (task.progress * 100f).toInt().coerceIn(0, 100)
    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CoverShape)
            ) {
                AsyncImage(
                    model = rememberCoverRequest(task.coverUrl, 48.dp),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (task.paused) Modifier.alpha(0.7f) else Modifier),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (task.paused) grayscaleFilter else null
                )
                if (task.paused) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f))
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // 时长紧跟歌手；歌手过长时省略，不挤掉时长
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.artist,
                        modifier = Modifier.weight(1f, fill = false),
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.durationMs > 0L) {
                        Text(
                            text = " · ${formatSongDuration(task.durationMs)}",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.paused) {
                                colorScheme.onBackground
                            } else {
                                colorScheme.onBackground.copy(alpha = 0.055f)
                            }
                        )
                        .semantics {
                            contentDescription =
                                if (task.paused) "继续下载 ${task.title}" else "暂停下载 ${task.title}"
                        }
                        .clickable(onClick = onTogglePause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (task.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = null,
                        tint = if (task.paused) {
                            colorScheme.background
                        } else {
                            colorScheme.onBackground.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.size(14.dp)
                    )
                }
//                Box(
//                    modifier = Modifier
//                        .size(32.dp)
//                        .clip(CircleShape)
//                        .semantics { contentDescription = "删除下载任务 ${task.title}" }
//                        .clickable(onClick = onCancel),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        imageVector = Icons.Outlined.DeleteOutline,
//                        contentDescription = null,
//                        tint = colorScheme.onSurfaceVariant,
//                        modifier = Modifier.size(16.dp)
//                    )
//                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colorScheme.onBackground.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(task.progress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .alpha(if (task.paused) 0.35f else 1f)
                        .background(colorScheme.onBackground.copy(alpha = 0.7f))
                )
            }
            Text(
                text = "$progressPercent%",
                modifier = Modifier.width(32.dp),
                color = if (task.paused) {
                    colorScheme.onSurfaceVariant
                } else {
                    colorScheme.onBackground
                },
                fontSize = 11.sp,
                textAlign = TextAlign.End
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
                    // 时长紧跟歌手；歌手过长时省略，不挤掉时长
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = song.artists,
                            modifier = Modifier.weight(1f, fill = false),
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (song.durationMs > 0L) {
                            Text(
                                text = " · ${formatSongDuration(song.durationMs)}",
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
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

// 与 SearchScreen 顶栏一致的圆形图标按钮
@Composable
private fun DownloadsHeaderIconButton(
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
