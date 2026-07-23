package com.leo.lune.ui.component.sidebar

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.leo.lune.util.rememberCoverRequest

private val MenuItemShape = RoundedCornerShape(16.dp)
private val IconBoxShape = RoundedCornerShape(12.dp)

// 侧边栏菜单项：id 供外部路由，label / icon 仅展示
private data class SidebarMenuItem(
    val id: String,
    val label: String,
    val icon: ImageVector
)

private val sidebarMenuItems = listOf(
    SidebarMenuItem("playlist", "添加歌单", Icons.AutoMirrored.Outlined.QueueMusic),
    SidebarMenuItem("download", "本地下载", Icons.Outlined.Download),
    SidebarMenuItem("together", "一起听", Icons.Outlined.Groups),
    SidebarMenuItem("identify", "听歌识曲", Icons.Outlined.MicNone),
    SidebarMenuItem("import_local", "导入本地歌曲", Icons.Outlined.LibraryMusic),
    SidebarMenuItem("settings", "设置", Icons.Outlined.Settings)
)

// 左侧抽屉侧边栏：实色面板 + 半透明遮罩；昵称/头像由 ViewModel 订阅
// 展开/收起动画：面板左侧滑入滑出、遮罩同步淡入淡出，统一 300ms
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun AppSidebar(
    // 是否打开；关闭时播放退出动画后再移出组合
    open: Boolean,
    // 点击遮罩 / 系统返回时回调
    onDismiss: () -> Unit,
    // 深浅色，影响遮罩与卡片高光
    darkTheme: Boolean = true,
    // 菜单项 id 回调（playlist / download / import_local / …）
    onMenuClick: (String) -> Unit = {},
    // 底部「退出登录」
    onLogoutClick: () -> Unit = {},
    viewModel: SidebarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 关闭后自动失效，避免退出动画期间重复触发
    BackHandler(enabled = open) { onDismiss() }

    // 宽度：屏宽 78%，上限 320dp（与设计稿 maxWidth 一致）
    val panelWidthDp = minOf(LocalConfiguration.current.screenWidthDp * 0.78f, 320f).dp

    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxSize()) {
        // 全屏遮罩：半透明压暗 + 点击关闭；淡入淡出，退出动画播完后再移出组合
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // 左侧实色面板：从屏外左侧滑入；退出时滑出动画播完后再移出组合
        AnimatedVisibility(
            visible = open,
            enter = slideInHorizontally(tween(300)) { -it },
            exit = slideOutHorizontally(tween(300)) { -it }
        ) {
            Box(
                modifier = Modifier
                    .width(panelWidthDp)
                    .fillMaxHeight()
                    .background(colorScheme.background)
                    // 吞掉点击，避免穿透到遮罩导致误关
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    // 顶部：头像 + 昵称
                    SidebarProfileSection(
                        nickname = uiState.nickname,
                        avatarUrl = uiState.avatarUrl,
                        darkTheme = darkTheme,
                        textPrimary = colorScheme.onBackground,
                    )

                    // 中部可滚动菜单；点击后回调并关闭侧栏
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        sidebarMenuItems.forEach { item ->
                            SidebarMenuRow(
                                item = item,
                                darkTheme = darkTheme,
                                textPrimary = colorScheme.onBackground,
                                textSecondary = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                onClick = {
                                    onMenuClick(item.id)
                                    onDismiss()
                                }
                            )
                        }
                    }

                    // 底部退出登录
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 24.dp, top = 8.dp)
                            .clip(MenuItemShape)
                            .clickable(onClick = {
                                onLogoutClick()
                                onDismiss()
                            })
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                tint = colorScheme.onBackground,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "退出登录",
                                color = colorScheme.onBackground,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// 资料区：头像 + 昵称
@Composable
private fun SidebarProfileSection(
    // 用户昵称；空则显示「未登录」
    nickname: String?,
    // 头像 URL
    avatarUrl: String?,
    // 深浅色，影响头像描边
    darkTheme: Boolean,
    // 主文字色（昵称）
    textPrimary: Color,
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = if (darkTheme) Color.White.copy(alpha = 0.15f)
                            else Color.Black.copy(alpha = 0.12f),
                            shape = CircleShape
                        )
                ) {
                    AsyncImage(
                        model = rememberCoverRequest(avatarUrl, 56.dp),
                        contentDescription = "用户头像",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                // 右下角在线状态绿点
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 2.dp, bottom = 2.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4ADE80))
                        .border(
                            width = 2.dp,
                            color = if (darkTheme) Color.Black else Color.White,
                            shape = CircleShape
                        )
                )
            }

            Text(
                text = nickname?.takeIf { it.isNotBlank() } ?: "未登录",
                modifier = Modifier.weight(1f),
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }
    }
}

// 单行菜单：图标盒 + 标题 + 右箭头
@Composable
private fun SidebarMenuRow(
    item: SidebarMenuItem,
    darkTheme: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    val iconBg = if (darkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
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
                imageVector = item.icon,
                contentDescription = null,
                tint = textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = item.label,
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
