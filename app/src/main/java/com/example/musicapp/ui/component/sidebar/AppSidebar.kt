package com.example.musicapp.ui.component.sidebar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musicapp.util.rememberCoverRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.roundToInt

private val SidebarShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp)
private val MenuItemShape = RoundedCornerShape(16.dp)
private val IconBoxShape = RoundedCornerShape(12.dp)
private val StatsCardShape = RoundedCornerShape(16.dp)

private val SidebarSlideEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private const val SidebarAnimMs = 380
private const val BackdropAnimMs = 300

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
    SidebarMenuItem("settings", "设置", Icons.Outlined.Settings)
)

/**
 * 左侧抽屉侧边栏：遮罩淡入 + 面板滑入，仅 UI / 动画。
 */
@Composable
fun AppSidebar(
    open: Boolean,
    onDismiss: () -> Unit,
    nickname: String?,
    avatarUrl: String?,
    likedCount: Int = 1_024,
    listeningHours: Int = 2_481,
    darkTheme: Boolean = true,
    hazeState: HazeState,
    onMenuClick: (String) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var visible by remember { mutableStateOf(open) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(open) {
        if (open) {
            visible = true
        }
        dragOffsetPx = 0f
    }

    val panelProgress by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(SidebarAnimMs, easing = SidebarSlideEasing),
        label = "sidebar_panel",
        finishedListener = { if (!open) visible = false }
    )
    val backdropProgress by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(BackdropAnimMs),
        label = "sidebar_backdrop"
    )

    if (!visible && panelProgress == 0f) return

    BackHandler(enabled = open) { onDismiss() }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val panelWidthDp = minOf(configuration.screenWidthDp * 0.78f, 320f).dp
    val panelWidthPx = with(density) { panelWidthDp.toPx() }

    val colorScheme = MaterialTheme.colorScheme
    val textPrimary = colorScheme.onBackground
    val textSecondary = colorScheme.onSurfaceVariant.copy(alpha = if (darkTheme) 0.55f else 0.45f)

    Box(modifier = Modifier.fillMaxSize()) {
        // 遮罩：点击关闭
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = backdropProgress }
                .drawBehind {
                    val gradient = if (darkTheme) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.04f),
                                Color.Black.copy(alpha = 0.6f)
                            ),
                            center = Offset(0f, 0f),
                            radius = size.maxDimension
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.02f),
                                Color.Black.copy(alpha = 0.25f)
                            ),
                            center = Offset(0f, 0f),
                            radius = size.maxDimension
                        )
                    }
                    drawRect(brush = gradient)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // 面板
        Box(
            modifier = Modifier
                .width(panelWidthDp)
                .fillMaxHeight()
                .offset {
                    val slide = (1f - panelProgress) * -panelWidthPx
                    IntOffset((slide + dragOffsetPx).roundToInt(), 0)
                }
                .pointerInput(open) {
                    if (!open) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffsetPx < -panelWidthPx * 0.28f) {
                                onDismiss()
                            } else {
                                dragOffsetPx = 0f
                            }
                        },
                        onDragCancel = { dragOffsetPx = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(-panelWidthPx, 0f)
                        }
                    )
                }
                .clip(SidebarShape)
                .then(
                    Modifier.hazeEffect(state = hazeState) {
                        blurRadius = 24.dp
                        tints = listOf(
                            HazeTint(
                                if (darkTheme) Color.Black.copy(alpha = 0.55f)
                                else Color.White.copy(alpha = 0.55f)
                            ),
                            HazeTint(
                                if (darkTheme) Color.White.copy(alpha = 0.06f)
                                else Color.Black.copy(alpha = 0.04f)
                            )
                        )
                        noiseFactor = 0.12f
                    }
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = if (darkTheme) {
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        }
                    ),
                    shape = SidebarShape
                )
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
                SidebarProfileSection(
                    nickname = nickname,
                    avatarUrl = avatarUrl,
                    likedCount = likedCount,
                    listeningHours = listeningHours,
                    darkTheme = darkTheme,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    sidebarMenuItems.forEach { item ->
                        SidebarMenuRow(
                            item = item,
                            darkTheme = darkTheme,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onClick = {
                                onMenuClick(item.id)
                                onDismiss()
                            }
                        )
                    }
                }

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
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "退出登录",
                            color = textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarProfileSection(
    nickname: String?,
    avatarUrl: String?,
    likedCount: Int,
    listeningHours: Int,
    darkTheme: Boolean,
    textPrimary: Color,
    textSecondary: Color
) {
    val glowColor = if (darkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box {
                // 头像背后柔光
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .graphicsLayer { alpha = 0.9f }
                        .background(glowColor, CircleShape)
                )
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
                // 在线绿点
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                Text(
                    text = nickname?.takeIf { it.isNotBlank() } ?: "未登录",
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "%,d 首喜欢".format(likedCount),
                    color = textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 听歌时长卡片
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(StatsCardShape)
                .background(
                    brush = if (darkTheme) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.05f),
                                Color.Black.copy(alpha = 0.02f)
                            )
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (darkTheme) Color.White.copy(alpha = 0.08f)
                    else Color.White.copy(alpha = 0.9f),
                    shape = StatsCardShape
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "%,d".format(listeningHours),
                    color = textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp
                )
                Text(
                    text = "小时",
                    color = textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "听歌总时长",
                color = textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

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
            .clip(MenuItemShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
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
