package com.leo.lune.ui.component.lyricsheader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.leo.lune.R
import com.leo.lune.util.rememberCoverRequest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// 顶栏歌词切换动画时长（毫秒）
private const val LyricTransitionMs = 450
// 主题切换图标旋转动画时长
private const val ThemeTransitionMs = 250
// 播放中头像光环脉冲一圈的时长
private const val PulseDurationMs = 1200

// 顶栏内容区高度（不含 statusBars）：padding(top=14) + height(46)
val HomeLyricsHeaderContentHeight = 60.dp

// 主 Tab 顶栏：用户头像 + 实时歌词 + 搜索/主题切换
@Composable
fun HomeLyricsHeader(
    darkTheme: Boolean,
    onSearchClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSidebar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeLyricsHeaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .zIndex(1f)
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(46.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarWithPulseRing(
            avatarUrl = uiState.avatarUrl,
            isPlaying = uiState.isPlaying,
            ringColor = colorScheme.primary,
            onClick = onOpenSidebar
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (uiState.hasPlaybackContent) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_music_note),
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "正在播放",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
            AnimatedLyricText(lyricLine = uiState.currentLyricLine)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeHeaderIconButton(
                onClick = onSearchClick,
                contentDescription = "搜索"
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colorScheme.onBackground,
                    modifier = Modifier.size(16.dp)
                )
            }
            HomeHeaderIconButton(
                onClick = onToggleTheme,
                contentDescription = "切换主题"
            ) {
                AnimatedThemeIcon(
                    darkTheme = darkTheme,
                    tint = colorScheme.onBackground
                )
            }
        }
    }
}

// 顶栏右侧圆形图标按钮（搜索、主题切换）
@Composable
private fun HomeHeaderIconButton(
    onClick: () -> Unit,
    contentDescription: String,
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

// 歌词切换动效：旧句上滑淡出，新句从下方滑入淡入
@Composable
private fun AnimatedLyricText(
    lyricLine: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val easing = FastOutSlowInEasing

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            // 裁剪切换动画的溢出区域，避免歌词滑动时影响布局
            .clip(RectangleShape),
        contentAlignment = Alignment.CenterStart
    ) {
        AnimatedContent(
            targetState = lyricLine,
            transitionSpec = {
                (slideInVertically(
                    animationSpec = tween(LyricTransitionMs, easing = easing),
                    initialOffsetY = { fullHeight -> fullHeight }
                ) + fadeIn(tween(LyricTransitionMs, easing = easing)))
                    .togetherWith(
                        slideOutVertically(
                            animationSpec = tween(LyricTransitionMs, easing = easing),
                            targetOffsetY = { fullHeight -> -fullHeight }
                        ) + fadeOut(tween(LyricTransitionMs, easing = easing))
                    )
            },
            label = "home_lyric_transition"
        ) { line ->
            Text(
                text = line,
                color = colorScheme.onBackground.copy(alpha = 0.9f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                lineHeight = 22.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// 用户头像；播放中时外圈紫色光环持续向外扩散
@Composable
private fun AvatarWithPulseRing(
    avatarUrl: String?,
    isPlaying: Boolean,
    ringColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(44.dp)
    ) {
        if (isPlaying) {
            val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
            // 圆环从 1× 扩散到 2×，同时透明度降至 0，形成脉冲效果
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(PulseDurationMs, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "avatar_pulse_scale"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(PulseDurationMs, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "avatar_pulse_alpha"
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .scale(scale)
                    .border(
                        width = 1.dp,
                        color = ringColor.copy(alpha = alpha),
                        shape = CircleShape
                    )
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colorScheme.surfaceContainerHigh)
                .border(0.67.dp, colorScheme.surfaceBright, CircleShape)
                .clickable(onClick = onClick)
        ) {
            AsyncImage(
                model = rememberCoverRequest(avatarUrl, 40.dp),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

// 主题切换图标：当前图标旋转 90° 淡出，新图标从 -90° 旋转进入
@Composable
private fun AnimatedThemeIcon(
    darkTheme: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val easing = FastOutSlowInEasing
    val halfDuration = ThemeTransitionMs / 2
    var displayedDarkTheme by remember { mutableStateOf(darkTheme) }
    val rotation = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(darkTheme) {
        if (displayedDarkTheme == darkTheme) return@LaunchedEffect

        // 前半段：退出当前图标
        coroutineScope {
            launch { rotation.animateTo(90f, tween(halfDuration, easing = easing)) }
            launch { alpha.animateTo(0f, tween(halfDuration, easing = easing)) }
        }

        displayedDarkTheme = darkTheme
        rotation.snapTo(-90f)
        alpha.snapTo(0f)

        // 后半段：新图标从反方向旋转进入
        coroutineScope {
            launch { rotation.animateTo(0f, tween(halfDuration, easing = easing)) }
            launch { alpha.animateTo(1f, tween(halfDuration, easing = easing)) }
        }
    }

    Icon(
        // 暗色主题显示太阳图标（点击切到亮色），亮色主题显示月亮图标
        imageVector = if (displayedDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(16.dp)
            .graphicsLayer {
                rotationZ = rotation.value
                this.alpha = alpha.value
            }
    )
}
