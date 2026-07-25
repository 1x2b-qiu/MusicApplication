package com.leo.lune.ui.component.download

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.Song
import com.leo.lune.util.formatFileSize
import com.leo.lune.util.rememberCoverRequest

private val SheetShape = RoundedCornerShape(28.dp)
private val CoverShape = RoundedCornerShape(12.dp)
private val OptionShape = RoundedCornerShape(16.dp)
private val ConfirmShape = RoundedCornerShape(16.dp)

/**
 * 下载音质选择底部弹层：
 * 歌曲信息 + 三档音质（已下载显示对号且不可再选）+ 确认下载
 * 体积优先展示 song/url 返回的真实 size，失败再回退估算
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQualityBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onConfirm: (DownloadQuality) -> Unit,
    viewModel: DownloadQualitySheetViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedQuality by remember(song.id) { mutableStateOf<DownloadQuality?>(null) }

    LaunchedEffect(song.id) {
        viewModel.load(song.id)
        selectedQuality = null
    }

    // 预选：优先默认音质（若未下载）；否则第一个未下载档；用户手动点选后以 selectedQuality 为准
    val effectiveSelected: DownloadQuality = selectedQuality
        ?.takeUnless { it in uiState.downloadedQualities }
        ?: uiState.defaultQuality.takeUnless { it in uiState.downloadedQualities }
        ?: DownloadQuality.entries.firstOrNull { it !in uiState.downloadedQualities }
        ?: uiState.defaultQuality

    // 当前选中档若已下完，清掉手动选择以便回落到下一个可用档
    LaunchedEffect(uiState.downloadedQualities, effectiveSelected) {
        if (selectedQuality != null && selectedQuality in uiState.downloadedQualities) {
            selectedQuality = null
        }
    }

    val canConfirm = effectiveSelected !in uiState.downloadedQualities &&
        uiState.downloadedQualities.size < DownloadQuality.entries.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.background,
        scrimColor = colorScheme.scrim.copy(alpha = 0.55f),
        dragHandle = null,
        shape = SheetShape,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colorScheme.primary, SheetShape)
                .padding(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = rememberCoverRequest(song.coverUrl, 48.dp),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CoverShape),
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
                        modifier = Modifier.padding(top = 4.dp),
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DownloadQuality.entries.forEach { quality ->
                    val downloaded = quality in uiState.downloadedQualities
                    val realBytes = uiState.sizeByQuality[quality] ?: 0L
                    QualityOptionRow(
                        quality = quality,
                        selected = !downloaded && quality == effectiveSelected,
                        downloaded = downloaded,
                        sizeHint = sizeLabel(
                            durationMs = song.durationMs,
                            quality = quality,
                            realBytes = realBytes
                        ),
                        onClick = {
                            if (!downloaded) selectedQuality = quality
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ConfirmShape)
                    .background(colorScheme.primary)
                    .alpha(if (canConfirm) 1f else 0.4f)
                    .clickable(enabled = canConfirm) {
                        onConfirm(effectiveSelected)
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "确认下载",
                    color = colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun QualityOptionRow(
    quality: DownloadQuality,
    selected: Boolean,
    downloaded: Boolean,
    sizeHint: String,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OptionShape)
            .then(
                if (selected) {
                    Modifier.background(colorScheme.secondaryContainer)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = !downloaded, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = quality.label,
            modifier = Modifier.weight(1f),
            color = colorScheme.onBackground,
            fontSize = 14.sp
        )
        Text(
            text = "${quality.detailPrefix} · $sizeHint",
            modifier = Modifier.padding(end = 12.dp),
            color = colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Box(
            modifier = Modifier
                .size(17.dp)
                .border(
                    width = 1.dp,
                    color = when {
                        downloaded || selected -> colorScheme.primary
                        else -> colorScheme.outlineVariant
                    },
                    shape = CircleShape
                )
                .then(
                    if (downloaded || selected) {
                        Modifier.background(colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                downloaded -> {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                }
                selected -> {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(colorScheme.onPrimary, CircleShape)
                    )
                }
            }
        }
    }
}

// 优先真实 size；未返回时回退按时长与码率估算（带「约」）
private fun sizeLabel(
    durationMs: Long,
    quality: DownloadQuality,
    realBytes: Long
): String {
    if (realBytes > 0L) return formatFileSize(realBytes)
    return estimateSizeLabel(durationMs, quality)
}

// 按时长与目标码率估算体积文案；时长未知时用设计稿量级占位
private fun estimateSizeLabel(durationMs: Long, quality: DownloadQuality): String {
    if (durationMs <= 0L) {
        return when (quality) {
            DownloadQuality.Standard -> "约 5 MB"
            DownloadQuality.High -> "约 12 MB"
            DownloadQuality.Lossless -> "约 35 MB"
        }
    }
    // 无损档按约 1000kbps 估算展示体积
    val estimateBps = when (quality) {
        DownloadQuality.Lossless -> 1_000_000
        else -> quality.bitrate
    }
    val bytes = durationMs / 1000.0 * (estimateBps / 8.0)
    val mb = bytes / (1024.0 * 1024.0)
    return String.format("约 %.1f MB", mb)
}
