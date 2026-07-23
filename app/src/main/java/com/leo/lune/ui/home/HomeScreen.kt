package com.leo.lune.ui.home

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.leo.lune.R
import com.leo.lune.domain.model.Song
import com.leo.lune.util.consumePointersUnlessResumed
import com.leo.lune.util.rememberCoverRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 顶栏歌词切换动画时长（毫秒）
private const val LyricTransitionMs = 450
// 主题切换图标旋转动画时长
private const val ThemeTransitionMs = 250
// 播放中头像光环脉冲一圈的时长
private const val PulseDurationMs = 1200

// 「我喜欢的」区块动画与布局参数
// 封面切换动画时长
private const val FavoritesCoverTransitionMs = 400
// 歌名切换动画时长
private const val FavoritesTitleTransitionMs = 300
// 缩略图选中态动画时长
private const val FavoritesThumbTransitionMs = 300
// 自动轮播间隔
private const val FavoritesAutoCarouselIntervalMs = 4_000L
// 用户无操作后恢复自动轮播的等待时长
private const val FavoritesAutoCarouselResumeDelayMs = 5_000L
// 磨砂玻璃淡入延迟（毫秒），首帧先用纯色背景
private const val FavoritesHazeFadeDelayMs = 150L
// 磨砂玻璃淡入动画时长（毫秒）
private const val FavoritesHazeFadeDurationMs = 400
// 磨砂玻璃主卡圆角
private val GlassCardShape = RoundedCornerShape(26.dp)
// 主卡内封面图圆角
private val CoverShape = RoundedCornerShape(16.dp)
// 底部缩略图圆角
private val ThumbShape = RoundedCornerShape(16.dp)
// 缩略图外层占位尺寸，为选中放大预留空间
private val ThumbOuterSize = 66.dp

// 封面进入：轻微放大 + 淡入
private val favoritesCoverEnterTransition = scaleIn(
    initialScale = 1.05f,
    animationSpec = tween(FavoritesCoverTransitionMs, easing = FastOutSlowInEasing)
) + fadeIn(tween(FavoritesCoverTransitionMs, easing = FastOutSlowInEasing))

// 封面退出：淡出
private val favoritesCoverExitTransition = fadeOut(
    tween(FavoritesCoverTransitionMs, easing = FastOutSlowInEasing)
)

// 歌名进入：自下而上滑入 + 淡入
private val favoritesTitleEnterTransition = slideInVertically(
    animationSpec = tween(FavoritesTitleTransitionMs, easing = FastOutSlowInEasing),
    initialOffsetY = { it / 3 }
) + fadeIn(tween(FavoritesTitleTransitionMs, easing = FastOutSlowInEasing))

// 歌名退出：向上滑出 + 淡出
private val favoritesTitleExitTransition = slideOutVertically(
    animationSpec = tween(FavoritesTitleTransitionMs, easing = FastOutSlowInEasing),
    targetOffsetY = { -it / 3 }
) + fadeOut(tween(FavoritesTitleTransitionMs, easing = FastOutSlowInEasing))

// 主卡磨砂玻璃在不同主题下的视觉参数
private data class FavoritesGlassStyle(
    // 背景模糊半径
    val blurRadius: Dp,
    // Haze 叠色层
    val hazeTints: List<HazeTint>,
    // 噪点强度，增强磨砂质感
    val noiseFactor: Float,
    // 半透明表面叠层
    val surfaceOverlay: Color,
    // 卡片描边颜色
    val borderColor: Color,
    // 顶部高光线中心透明度
    val highlightCenterAlpha: Float,
    // 卡片阴影高度
    val shadowElevation: Dp,
    // 主阴影颜色
    val spotColor: Color,
    // 环境阴影颜色
    val ambientColor: Color
)

// 首页：固定顶栏 + 可滚动内容区
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    hazeState: HazeState,
    onSearchClick: () -> Unit,
    onLikedClick: () -> Unit,
    onRecentClick: () -> Unit,
    onLoginClick: () -> Unit,
    onOpenSidebar: () -> Unit = {},
    darkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .consumePointersUnlessResumed()
    ) {
        // 顶栏固定在 Column 顶部，不随列表滚动
        HomeLyricsHeader(
            currentLyricLine = uiState.currentLyricLine,
            isPlaying = uiState.isPlaying,
            hasPlaybackContent = uiState.hasPlaybackContent,
            avatarUrl = uiState.loginState.avatarUrl,
            darkTheme = darkTheme,
            onSearchClick = onSearchClick,
            onToggleTheme = onToggleTheme,
            onAvatarClick = {
                // 未登录跳转登录；已登录打开侧边栏
                if (!uiState.loginState.isLoggedIn) {
                    onLoginClick()
                } else {
                    onOpenSidebar()
                }
            }
        )

        when {
            uiState.error != null && uiState.recentSongs.isEmpty() -> {
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 161.dp)
                ) {
                    if (uiState.likedSongs.isNotEmpty()) {
                        item {
                            HomeFavoritesSection(
                                songs = uiState.likedSongs,
                                hazeState = hazeState,
                                darkTheme = darkTheme,
                                isPlaying = uiState.isPlaying,
                                currentSongId = uiState.currentSongId,
                                onPlaySong = viewModel::playSong,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onViewAllClick = onLikedClick
                            )
                        }
                    }
                    item {
                        if(uiState.recentSongs.isNotEmpty()) {
                            HomeSectionHeader(
                                title = "最近播放",
                                onViewAllClick = onRecentClick
                            )
                        }
                    }
                    items(uiState.recentSongs, key = { it.id }) { song ->
                        HomeRecentItem(
                            song = song,
                            onClick = { viewModel.playSong(song, uiState.recentSongs) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// 首页顶栏：用户头像 + 实时歌词 + 搜索/主题切换
@Composable
private fun HomeLyricsHeader(
    currentLyricLine: String,
    isPlaying: Boolean,
    hasPlaybackContent: Boolean,
    avatarUrl: String?,
    darkTheme: Boolean,
    onSearchClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(46.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarWithPulseRing(
            avatarUrl = avatarUrl,
            isPlaying = isPlaying,
            ringColor = colorScheme.primary,
            onClick = onAvatarClick
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (hasPlaybackContent) {
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
            AnimatedLyricText(lyricLine = currentLyricLine)
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

// 区块标题行：左侧可选图标 + 标题，右侧「全部」
@Composable
fun HomeSectionHeader(
    title: String,
    // 传 null 时不显示图标；彩色 PNG 请保持 iconTint 默认 Unspecified
    @DrawableRes iconRes: Int? = null,
    iconTint: Color = Color.Unspecified,
    onViewAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                color = colorScheme.onBackground,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 25.5.sp,
                letterSpacing = (-0.34).sp
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onViewAllClick
                ),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "全部",
                color = colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 19.5.sp
            )
            Image(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// 「最近播放」列表单行
// 首页 / 搜索页共用的歌曲列表项：封面 + 歌名 + 歌手 + 时长
@Composable
fun HomeRecentItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.surfaceDim, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = rememberCoverRequest(song.coverUrl, 48.dp),
            contentDescription = song.name,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 21.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artists,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatSongDuration(song.durationMs),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        )
    }
}

// 「我喜欢的」磨砂玻璃主卡 + 缩略图横向选择
@Composable
private fun HomeFavoritesSection(
    songs: List<Song>,
    hazeState: HazeState,
    darkTheme: Boolean,
    isPlaying: Boolean,
    currentSongId: Long?,
    onPlaySong: (Song, List<Song>) -> Unit,
    onTogglePlayPause: () -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return

    // 按歌曲 ID 索引，供 AnimatedContent 切换时 O(1) 取歌
    val songById = remember(songs) { songs.associateBy { it.id } }
    // 当前选中的缩略图下标，驱动主卡封面与歌名
    var selectedIndex by remember(songs) { mutableIntStateOf(0) }
    // 是否启用自动轮播；用户手动操作后会暂时关闭
    var isAutoCarouselEnabled by remember { mutableStateOf(true) }
    // 每次用户操作递增，用于重置「5 秒后恢复轮播」计时
    var idleResumeEpoch by remember { mutableIntStateOf(0) }
    // 底部缩略图横向列表滚动状态，供自动轮播 animateScrollToItem
    val thumbnailListState = rememberLazyListState()
    // 当前可见的缩略图下标集合，离屏项不启动动画
    val visibleIndices by remember {
        derivedStateOf {
            thumbnailListState.layoutInfo.visibleItemsInfo.map { it.index }.toSet()
        }
    }
    // 防止列表变化后 selectedIndex 越界
    val safeIndex = selectedIndex.coerceIn(0, songs.lastIndex)
    // 当前主卡展示的歌曲
    val selectedSong = songs[safeIndex]

    // 用户手动操作：暂停自动轮播，并重启空闲恢复计时
    val onUserInteraction: () -> Unit = {
        isAutoCarouselEnabled = false
        idleResumeEpoch++
    }
    // 包一层 rememberUpdatedState，供 NestedScrollConnection 安全读取最新回调
    val onUserInteractionState = rememberUpdatedState(onUserInteraction)
    // 仅拦截用户手势滚动，避免自动轮播触发的 animateScrollToItem 误停轮播
    val thumbnailScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    onUserInteractionState.value()
                }
                return Offset.Zero
            }
        }
    }

    // 用户停止操作 5 秒后，重新开启自动轮播
    LaunchedEffect(idleResumeEpoch, songs) {
        if (idleResumeEpoch == 0) return@LaunchedEffect
        delay(FavoritesAutoCarouselResumeDelayMs)
        if (songs.size > 1) {
            isAutoCarouselEnabled = true
        }
    }

    // 自动轮播：定时切歌并滚动缩略图；手动选中时不触发列表滚动
    LaunchedEffect(songs, isAutoCarouselEnabled) {
        if (!isAutoCarouselEnabled || songs.size <= 1) return@LaunchedEffect
        while (true) {
            delay(FavoritesAutoCarouselIntervalMs)
            val nextIndex = (selectedIndex + 1) % songs.size
            selectedIndex = nextIndex
            thumbnailListState.animateScrollToItem(nextIndex)
        }
    }

    Column(modifier = modifier) {
        HomeSectionHeader(
            title = "我喜欢的",
            iconRes = R.drawable.ic_heart2,
            onViewAllClick = onViewAllClick
        )

        FavoritesGlassMainCard(
            selectedSongId = selectedSong.id,
            songById = songById,
            hazeState = hazeState,
            darkTheme = darkTheme,
            isPlayingThis = isPlaying && currentSongId == selectedSong.id,
            onPlayClick = {
                onUserInteraction()
                val isPlayingThis = isPlaying && currentSongId == selectedSong.id
                if (isPlayingThis) {
                    onTogglePlayPause()
                } else {
                    onPlaySong(selectedSong, songs)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            state = thumbnailListState,
            modifier = Modifier.nestedScroll(thumbnailScrollConnection),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                FavoritesThumbnailItem(
                    song = song,
                    isSelected = index == safeIndex,
                    isLazyAnimated = index in visibleIndices,
                    onClick = {
                        selectedIndex = index
                        onUserInteraction()
                    }
                )
            }
        }
    }
}

// 磨砂玻璃主卡：封面/歌名动画层 + 固定渐变与播放按钮
@Composable
private fun FavoritesGlassMainCard(
    selectedSongId: Long,
    songById: Map<Long, Song>,
    hazeState: HazeState,
    darkTheme: Boolean,
    isPlayingThis: Boolean,
    onPlayClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val glassStyle = rememberFavoritesGlassStyle(darkTheme)
    val coverDecodeWidth = LocalConfiguration.current.screenWidthDp.dp
    val playInteraction = remember { MutableInteractionSource() }
    val isPlayPressed by playInteraction.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.9f else 1f,
        animationSpec = tween(120),
        label = "play_button_scale"
    )

    // 延迟 Haze 模糊：首帧用纯色背景，延迟后淡入模糊效果
    var isHazeReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(FavoritesHazeFadeDelayMs)
        isHazeReady = true
    }
    val hazeAlpha by animateFloatAsState(
        targetValue = if (isHazeReady) 1f else 0f,
        animationSpec = tween(FavoritesHazeFadeDurationMs, easing = FastOutSlowInEasing),
        label = "haze_fade_in"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = glassStyle.shadowElevation,
                shape = GlassCardShape,
                spotColor = glassStyle.spotColor,
                ambientColor = glassStyle.ambientColor
            )
            .clip(GlassCardShape)
            .hazeEffect(state = hazeState) {
                blurRadius = glassStyle.blurRadius * hazeAlpha
                tints = glassStyle.hazeTints.map { tint ->
                    HazeTint(tint.color.copy(alpha = tint.color.alpha * hazeAlpha))
                }
                noiseFactor = glassStyle.noiseFactor * hazeAlpha
            }
            .background(glassStyle.surfaceOverlay)
            .border(0.67.dp, glassStyle.borderColor, GlassCardShape)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = glassStyle.highlightCenterAlpha),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(208.dp)
                .clip(CoverShape)
        ) {
            // 仅封面图参与切换动画；lambda 必须用 songId 查歌，保证退出层显示旧封面
            AnimatedContent(
                targetState = selectedSongId,
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    favoritesCoverEnterTransition togetherWith favoritesCoverExitTransition
                },
                label = "favorites_cover"
            ) { songId ->
                val song = songById[songId] ?: return@AnimatedContent
                AsyncImage(
                    model = rememberCoverRequest(song.coverUrl, coverDecodeWidth, 208.dp),
                    contentDescription = song.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    // 歌名/艺人单独动画；播放按钮固定在外层 Row，不参与切换
                    AnimatedContent(
                        targetState = selectedSongId,
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomStart,
                        transitionSpec = {
                            favoritesTitleEnterTransition togetherWith favoritesTitleExitTransition
                        },
                        label = "favorites_title"
                    ) { songId ->
                        val song = songById[songId] ?: return@AnimatedContent
                        Column(modifier = Modifier.padding(end = 12.dp)) {
                            Text(
                                text = song.name,
                                color = Color(0xFFF4F2FB),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 25.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artists,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                lineHeight = 19.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(playScale)
                        .shadow(12.dp, CircleShape, spotColor = colorScheme.primary)
                        .clip(CircleShape)
                        .background(Color(0xFFF4F2FB))
                        .clickable(
                            interactionSource = playInteraction,
                            indication = null,
                            onClick = onPlayClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            if (isPlayingThis) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (isPlayingThis) "暂停" else "播放",
                        colorFilter = ColorFilter.tint(Color(0xFF0E0E10)),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// 底部缩略图项：选中放大 + 光环/蒙层渐变
@Composable
private fun FavoritesThumbnailItem(
    song: Song,
    isSelected: Boolean,
    isLazyAnimated: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    // 可见时用 tween 平滑动画，离屏时用 snap 跳过动画帧
    val thumbTransition = if (isLazyAnimated) {
        tween<Float>(FavoritesThumbTransitionMs, easing = FastOutSlowInEasing)
    } else {
        snap()
    }

    // 选中时放大，未选中时缩小
    val thumbSize by animateDpAsState(
        targetValue = if (isSelected) 62.dp else 54.dp,
        animationSpec = if (isLazyAnimated) {
            tween(FavoritesThumbTransitionMs, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "thumb_size"
    )
    // 选中光环透明度，驱动描边颜色插值
    val ringAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = thumbTransition,
        label = "thumb_ring_alpha"
    )
    // 未选中时叠加暗色蒙层，选中时完全透明
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0f else 0.4f,
        animationSpec = thumbTransition,
        label = "thumb_overlay_alpha"
    )
    // 描边颜色：未选中偏白，选中过渡到主题紫色
    val innerBorderColor = lerp(
        Color.White.copy(alpha = 0.12f),
        colorScheme.primary.copy(alpha = 0.7f),
        ringAlpha
    )

    Box(
        modifier = Modifier.size(ThumbOuterSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(thumbSize)
                .clip(ThumbShape)
                .border(width = 0.67.dp, color = innerBorderColor, shape = ThumbShape)
                .clickable(onClick = onClick)
        ) {
            AsyncImage(
                model = rememberCoverRequest(song.coverUrl, 62.dp),
                contentDescription = song.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (overlayAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = overlayAlpha))
                )
            }
        }
    }
}

// 按深/浅主题返回对应的玻璃样式
@Composable
private fun rememberFavoritesGlassStyle(darkTheme: Boolean): FavoritesGlassStyle {
    val colorScheme = MaterialTheme.colorScheme
    return remember(darkTheme, colorScheme) {
        if (darkTheme) {
            // 深色主题：暗底 + 白色描边/高光
            FavoritesGlassStyle(
                blurRadius = 20.dp,
                hazeTints = listOf(
                    HazeTint(colorScheme.background.copy(alpha = 0.5f)),
                    HazeTint(Color.White.copy(alpha = 0.08f))
                ),
                noiseFactor = 0.12f,
                surfaceOverlay = Color.White.copy(alpha = 0.06f),
                borderColor = Color.White.copy(alpha = 0.18f),
                highlightCenterAlpha = 0.5f,
                shadowElevation = 24.dp,
                spotColor = Color(0x807B5CFF),
                ambientColor = Color(0x407B5CFF)
            )
        } else {
            // 浅色主题：乳白磨砂 + 黑色描边，提高对比度
            FavoritesGlassStyle(
                blurRadius = 28.dp,
                hazeTints = listOf(
                    HazeTint(Color.White.copy(alpha = 0.72f)),
                    HazeTint(Color.Black.copy(alpha = 0.04f))
                ),
                noiseFactor = 0.2f,
                surfaceOverlay = Color.White.copy(alpha = 0.62f),
                borderColor = Color.Black.copy(alpha = 0.18f),
                highlightCenterAlpha = 0.85f,
                shadowElevation = 20.dp,
                spotColor = Color(0x667B5CFF),
                ambientColor = Color(0x26000000)
            )
        }
    }
}