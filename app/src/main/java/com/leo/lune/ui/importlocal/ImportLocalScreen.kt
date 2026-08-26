package com.leo.lune.ui.importlocal

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leo.lune.local.ScannedLocalTrack
import com.leo.lune.ui.home.formatSongDuration
import com.leo.lune.util.consumePointersUnlessResumed

private val ActionButtonShape = RoundedCornerShape(14.dp)
private val RowShape = RoundedCornerShape(12.dp)

// 「导入本地歌曲」：自定义文件夹扫描，确认后写入本地曲库
@Composable
fun ImportLocalScreen(
    onBack: () -> Unit,
    viewModel: ImportLocalViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    // 底部留白：迷你播放栏 66dp + 导航层间距 12dp
    val miniPlayerBottomInset = 78.dp

    val openTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // 会话内可读；持久化权限以便导入后仍能打开文件
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val folderName = DocumentFile.fromTreeUri(context, uri)?.name
            ?.takeIf { it.isNotBlank() }
            ?: "已选择文件夹"
        viewModel.onFolderPicked(uri, folderName)
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
        ImportLocalTopBar(onBack = onBack)

        Text(
            text = "选择手机中的文件夹，扫描音频后点「导入」写入曲库。不会复制或删除原文件。",
            color = colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ImportLocalActionButton(
                text = if (uiState.isScanning) "扫描中…" else "选择文件夹",
                enabled = !uiState.isScanning && !uiState.isImporting,
                filled = true,
                onClick = { openTreeLauncher.launch(null) },
                modifier = Modifier.weight(1f)
            )
            if (uiState.isScanning) {
                ImportLocalActionButton(
                    text = "取消",
                    enabled = true,
                    filled = false,
                    onClick = viewModel::cancelScan,
                    modifier = Modifier.weight(1f)
                )
            } else if (uiState.tracks.isNotEmpty() || uiState.error != null) {
                ImportLocalActionButton(
                    text = "清空",
                    enabled = !uiState.isImporting,
                    filled = false,
                    onClick = viewModel::clearResults,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!uiState.isScanning && uiState.tracks.isNotEmpty()) {
            ImportLocalActionButton(
                text = when {
                    uiState.isImporting -> "导入中…"
                    else -> "导入 ${uiState.tracks.size} 首到曲库"
                },
                enabled = !uiState.isImporting,
                filled = true,
                onClick = viewModel::importScannedTracks,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
        }

        uiState.importMessage?.let { message ->
            Text(
                text = message,
                color = colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        when {
            uiState.isScanning -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = colorScheme.onBackground
                        )
                        Text(
                            text = buildString {
                                append("正在扫描")
                                uiState.folderName?.let {
                                    append("「")
                                    append(it)
                                    append("」")
                                }
                                append(" · 已发现 ")
                                append(uiState.scannedCount)
                                append(" 首")
                            },
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    if (uiState.tracks.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(top = 12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(
                                items = uiState.tracks,
                                key = { index, track -> "${track.uri}_$index" }
                            ) { index, track ->
                                ScannedTrackRow(index = index, track = track)
                            }
                        }
                    }
                }
            }
            uiState.error != null -> {
                Text(
                    text = uiState.error.orEmpty(),
                    color = colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
            uiState.tracks.isEmpty() && uiState.folderName == null -> {
                ImportLocalEmptyHint(modifier = Modifier.weight(1f))
            }
            uiState.tracks.isEmpty() -> {
                Text(
                    text = "该文件夹下未发现音频文件",
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
            else -> {
                Text(
                    text = buildString {
                        uiState.folderName?.let {
                            append(it)
                            append(" · ")
                        }
                        append("共 ")
                        append(uiState.tracks.size)
                        append(" 首")
                    },
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(
                        items = uiState.tracks,
                        key = { index, track -> "${track.uri}_$index" }
                    ) { index, track ->
                        ScannedTrackRow(index = index, track = track)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportLocalTopBar(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colorScheme.surfaceVariant)
                .border(0.67.dp, colorScheme.outlineVariant, CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "返回",
                tint = colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "导入本地歌曲",
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
private fun ImportLocalActionButton(
    text: String,
    enabled: Boolean,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val background = when {
        !enabled -> colorScheme.surfaceVariant.copy(alpha = 0.6f)
        filled -> colorScheme.onBackground
        else -> colorScheme.surfaceVariant
    }
    val content = when {
        !enabled -> colorScheme.onSurfaceVariant
        filled -> colorScheme.background
        else -> colorScheme.onBackground
    }
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(ActionButtonShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = content,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ImportLocalEmptyHint(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(colorScheme.onBackground.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = colorScheme.onBackground.copy(alpha = 0.45f),
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = "还没有扫描结果",
            color = colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "点「选择文件夹」开始自定义扫描",
            color = colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ScannedTrackRow(
    index: Int,
    track: ScannedLocalTrack
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = (index + 1).toString(),
            color = colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(end = 10.dp)
                .size(width = 22.dp, height = 18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    val artist = track.artists.ifBlank { "未知艺术家" }
                    append(artist)
                    if (track.album.isNotBlank()) {
                        append(" · ")
                        append(track.album)
                    }
                },
                color = colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        if (track.durationMs > 0L) {
            Text(
                text = formatSongDuration(track.durationMs),
                color = colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}
