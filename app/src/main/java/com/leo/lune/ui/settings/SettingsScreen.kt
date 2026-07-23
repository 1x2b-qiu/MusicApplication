package com.leo.lune.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// 退出登录行的强调色（浅红，区别于普通设置项）
private val LogoutTextColor = Color(0xFFEB958F)

// 设置页：列表展示偏好项；业务逻辑暂由 ViewModel 占位，后续再接
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // 顶栏布局对齐搜索页：top 14dp + 46dp 行高，返回按钮垂直居中
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(46.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsBackButton(onClick = onBack)
            }

            // 页面标题
            Text(
                text = "设置",
                color = colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.8).sp,
                modifier = Modifier.padding(top = 28.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 状态栏歌词：整行点击切换；开关仅作展示，避免与行点击重复触发
            SettingsRow(
                label = "状态栏歌词",
                showDivider = true,
                trailing = {
                    SettingsSwitch(
                        enabled = uiState.statusBarLyricsEnabled,
                        onClick = null
                    )
                },
                onClick = viewModel::toggleStatusBarLyrics
            )

            // 下载设置：打开弹层（功能暂未实现）
            SettingsRow(
                label = "下载设置",
                detail = if (uiState.wifiOnlyDownload) "仅 Wi-Fi" else "允许移动网络",
                showDivider = true,
                onClick = { }
            )

            // 清理缓存：右侧展示缓存体积文案
            SettingsRow(
                label = "清理缓存",
                detail = uiState.cacheSizeLabel,
                showDivider = true,
                onClick = {  }
            )

            // 检查更新：右侧展示版本号；检查中时隐藏箭头
            SettingsRow(
                label = "检查更新",
                detail = if (uiState.checkingUpdate) "检查中" else "v${uiState.appVersion}",
                showChevron = !uiState.checkingUpdate,
                showDivider = true,
                onClick = viewModel::checkUpdate
            )

            // 退出登录：危险操作，用强调色区分
            SettingsRow(
                label = "退出登录",
                labelColor = LogoutTextColor,
                chevronTint = LogoutTextColor.copy(alpha = 0.6f),
                showDivider = false,
                onClick = {}
            )
        }
    }
}

// 与 SearchScreen 顶栏圆形按钮一致：36dp 圆底 + 细描边
@Composable
private fun SettingsBackButton(onClick: () -> Unit) {
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
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            contentDescription = "返回",
            tint = colorScheme.onBackground,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 设置列表单行。
 * trailing 非空时优先渲染自定义尾部（如开关）；否则渲染 detail + 右箭头。
 * onClick 为 null 时整行不可点（仅由尾部自行处理点击）。
 */
@Composable
private fun SettingsRow(
    // 左侧主文案
    label: String,
    // 作用于整行外层 Column
    modifier: Modifier = Modifier,
    // 整行点击回调；为 null 时不可点
    onClick: (() -> Unit)? = null,
    // 右侧次要文案（如「仅 Wi-Fi」「286 MB」）；有 trailing 时不展示
    detail: String? = null,
    // 左侧主文案颜色（退出登录等可用强调色）
    labelColor: Color = MaterialTheme.colorScheme.onBackground,
    // 右侧箭头颜色
    chevronTint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
    // 是否显示右箭头（如检查更新中可隐藏）
    showChevron: Boolean = true,
    // 是否在行底绘制分割线（末行通常关闭）
    showDivider: Boolean = true,
    // 自定义右侧内容（如开关）；非空时覆盖 detail 与箭头
    trailing: (@Composable () -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .padding(vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = labelColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (trailing != null) {
                trailing()
            } else {
                if (detail != null) {
                    Text(
                        text = detail,
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                if (showChevron) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = chevronTint,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // 行间细分割线（末行可通过 showDivider=false 去掉）
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colorScheme.outlineVariant.copy(alpha = 0.05f))
            )
        }
    }
}

/**
 * 自定义开关。
 * [onClick] 为 null 时仅作状态展示（由外层行点击切换），避免嵌套 clickable 连点两次。
 */
@Composable
private fun SettingsSwitch(
    enabled: Boolean,
    onClick: (() -> Unit)?
) {
    val colorScheme = MaterialTheme.colorScheme
    // 滑块左右位移：开启靠右 16dp，关闭靠左
    val thumbOffset by animateDpAsState(
        targetValue = if (enabled) 16.dp else 0.dp,
        animationSpec = spring(stiffness = 520f, dampingRatio = 0.82f),
        label = "settingsSwitchThumb"
    )

    Box(
        modifier = Modifier
            .width(40.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(50))
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(50))
            .background(if (enabled) colorScheme.onBackground else colorScheme.surfaceContainerHigh)
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onClick
                        )
                        .semantics { contentDescription = if (enabled) "关闭" else "开启" }
                } else {
                    Modifier
                }
            )
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(20.dp)
                .clip(CircleShape)
                .background(if (enabled) colorScheme.background else colorScheme.onBackground.copy(alpha = 0.8f))
        )
    }
}
