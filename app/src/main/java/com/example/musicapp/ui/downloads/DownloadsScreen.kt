package com.example.musicapp.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.musicapp.domain.model.DownloadedSong
import com.example.musicapp.util.rememberCoverRequest

private val CoverShape = RoundedCornerShape(12.dp)

// 本地下载列表页：下载中 / 已下载两段；列表行用上下分割线（对齐设计稿）
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloaded = uiState.downloadedSongs
    // 底部迷你播放栏预留
    val miniPlayerBottomInset = 78.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(bottom = miniPlayerBottomInset)
    ) {
        // 顶栏：仅返回
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DownloadsBackButton(onClick = onBack)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "本地下载",
                modifier = Modifier.padding(top = 12.dp),
                color = colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.6).sp
            )

            Spacer(modifier = Modifier.height(32.dp))
            SectionLabel(text = "下载中")
            Spacer(modifier = Modifier.height(12.dp))
            DownloadsEmptyRow(text = "没有正在下载的歌曲")

            Spacer(modifier = Modifier.height(36.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel(
                    text = "已下载",
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = downloaded.size.coerceAtMost(99).toString().padStart(2, '0'),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (downloaded.isEmpty()) {
                DownloadsEmptyRow(text = "暂时没有已下载的歌曲")
            } else {
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.9f))
                downloaded.forEach { song ->
                    DownloadedSongRow(
                        song = song,
                        onClick = { viewModel.playSong(song) }
                    )
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.9f))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.2.sp
    )
}

// 空态行：上下分割线 + 文案
@Composable
private fun DownloadsEmptyRow(text: String) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.9f))
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            color = colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.9f))
    }
}

// 已下载行：封面 + 歌名/歌手 + 音质（分割线由外层统一绘制）
@Composable
private fun DownloadedSongRow(
    song: DownloadedSong,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                fontSize = 14.sp,
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
        Text(
            text = formatQualityLabel(song.bitrate),
            color = colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun DownloadsBackButton(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(36.dp)
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

// 按码率映射设计稿音质文案
private fun formatQualityLabel(bitrate: Int): String = when {
    bitrate >= 900_000 -> "无损"
    bitrate >= 320_000 -> "高品质"
    bitrate >= 192_000 -> "较高"
    else -> "标准"
}
