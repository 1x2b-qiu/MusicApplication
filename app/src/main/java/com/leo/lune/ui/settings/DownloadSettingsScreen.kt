package com.leo.lune.ui.settings

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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.util.consumePointersUnlessResumed

// 与本地下载页一致的列表外层卡片圆角
private val CardShape = RoundedCornerShape(24.dp)

// 存储位置图标容器圆角
private val IconBoxShape = RoundedCornerShape(12.dp)

// 设计稿音质体积说明（仅展示用）
private fun DownloadQuality.sizeHint(): String = when (this) {
    DownloadQuality.Standard -> "约 1.5 MB / 分钟"
    DownloadQuality.High -> "约 3.6 MB / 分钟"
    DownloadQuality.Lossless -> "约 10 MB / 分钟"
}

// 下载设置页：默认音质单选 + 存储位置入口（仅 UI）
@Composable
fun DownloadSettingsScreen(
    onBack: () -> Unit,
    darkTheme: Boolean,
    viewModel: DownloadSettingsViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .consumePointersUnlessResumed()
    ) {
        DownloadSettingsTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionLabel(title = "下载默认音质")
            Spacer(modifier = Modifier.height(12.dp))
            DownloadSettingsCard {
                DownloadQuality.entries.forEach { quality ->
                    QualityOptionRow(
                        title = quality.label,
                        detail = quality.sizeHint(),
                        selected = uiState.selectedQuality == quality,
                        onClick = { viewModel.selectQuality(quality) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            SectionLabel(title = "下载歌曲存储位置")
            Spacer(modifier = Modifier.height(12.dp))
            StorageLocationRow(
                location = uiState.storageLocation,
                darkTheme = darkTheme,
                onClick = { viewModel.toggleStorageLocation() }
            )
        }
    }
}

// 顶栏：左返回 + 居中「下载设置」（与本地下载页同结构）
@Composable
private fun DownloadSettingsTopBar(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DownloadSettingsHeaderIconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "返回",
                tint = colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "下载设置",
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
private fun SectionLabel(title: String) {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        text = title,
        color = colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

// 列表外层卡片外壳（surfaceVariant + 细描边，对齐本地下载页）
@Composable
private fun DownloadSettingsCard(
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
private fun QualityOptionRow(
    title: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    width = 1.5.dp,
                    color = if (selected) {
                        colorScheme.onBackground
                    } else {
                        colorScheme.onBackground.copy(alpha = 0.3f)
                    },
                    shape = CircleShape
                )
                .background(
                    color = if (selected) colorScheme.onBackground else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colorScheme.background)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StorageLocationRow(
    location: DownloadStorageLocation,
    darkTheme: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val iconBg = if (darkTheme) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.055f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colorScheme.surfaceVariant)
            .border(0.67.dp, colorScheme.outlineVariant, CardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(IconBoxShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = location.label,
                color = colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = location.pathHint,
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(16.dp)
        )
    }
}

// 与本地下载页一致的圆形图标按钮
@Composable
private fun DownloadSettingsHeaderIconButton(
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
