package com.leo.lune.ui.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.leo.lune.util.consumePointersUnlessResumed

// 设置列表外层卡片圆角（对齐设计稿 rounded-[26px]）
private val CardShape = RoundedCornerShape(26.dp)

// 行内图标容器圆角（与 SidebarMenuRow 一致）
private val IconBoxShape = RoundedCornerShape(12.dp)

// 设置项：仅 UI 占位，点击暂无业务
private data class SettingsItem(
    val id: String,
    val label: String,
    val icon: ImageVector
)

private val SettingsItems = listOf(
    SettingsItem("account", "账号管理", Icons.Outlined.PersonOutline),
    SettingsItem("playback", "播放设置", Icons.Outlined.PlayArrow),
    SettingsItem("download", "下载设置", Icons.Outlined.Download),
    SettingsItem("cache", "清理缓存", Icons.Outlined.Cached),
    SettingsItem("about", "关于", Icons.Outlined.Info)
)

// 设置页：顶栏居中标题 + 圆角卡片列表（对齐设计稿，功能后续再接）
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    darkTheme: Boolean,
    @Suppress("UNUSED_PARAMETER")
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .consumePointersUnlessResumed()
    ) {
        SettingsTopBar(onBack = onBack)

        Spacer(modifier = Modifier.height(8.dp))

        SettingsCard {
            SettingsItems.forEach { item ->
                SettingsRow(
                    label = item.label,
                    icon = item.icon,
                    darkTheme = darkTheme,
                    onClick = { }
                )
            }
        }
    }
}

// 顶栏：左返回 + 居中「设置」（与本地下载页同结构）
@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsHeaderIconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "返回",
                tint = colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "设置",
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

// 列表外层卡片外壳（surfaceVariant + 细描边）
@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
            .clip(CardShape)
            .background(colorScheme.surfaceVariant)
            .border(0.67.dp, colorScheme.outlineVariant, CardShape)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        content()
    }
}

// 设置单行：图标盒 + 标题 + 右箭头（样式与 SidebarMenuRow 一致）
@Composable
private fun SettingsRow(
    label: String,
    icon: ImageVector,
    darkTheme: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val textPrimary = colorScheme.onBackground
    val textSecondary = colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val iconBg = if (darkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .clip(IconBoxShape)
            .clickable(
                onClick = onClick
            )
            .padding(horizontal = 8.dp)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(IconBoxShape)
                .background(iconBg)
                .border(
                    width = 1.dp,
                    color = if (darkTheme) Color.White.copy(alpha = 0.08f)
                    else Color.White.copy(alpha = 0.7f),
                    shape = IconBoxShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = textSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

// 与本地下载页一致的圆形图标按钮
@Composable
private fun SettingsHeaderIconButton(
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
