package com.leo.lune.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.leo.lune.R
import com.leo.lune.controller.PlaybackPosition
import com.leo.lune.controller.PlayerPlayMode
import com.leo.lune.domain.model.Song
import com.leo.lune.ui.component.download.DownloadQualityBottomSheet
import com.leo.lune.ui.component.player.PlayerQueueBottomSheet
import com.leo.lune.ui.home.formatSongDuration
import com.leo.lune.util.consumePointersUnlessResumed
import com.leo.lune.util.rememberCoverRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.StateFlow

// 底部浮层控制区预留高度：控制卡 + 间距 + 工具栏 + 底边距
private val PlayerBottomControlsInset = 240.dp

// 全屏播放页：封面 / 歌词在中部，磨砂控制栏浮在底部；沉浸模式单独全屏展示
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onDownloadsClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 播放页独立 Haze：背景氛围层为源，底栏卡片在源外采样
    val playerHazeState = rememberHazeState()

    // 播放队列
    var queueSheetOpen by remember { mutableStateOf(false) }
    var immersiveMode by remember { mutableStateOf(false) }
    var downloadSheetOpen by remember { mutableStateOf(false) }

    // 沉浸聆听：全屏歌词，与常规布局互斥
    if (immersiveMode) {
        ImmersivePlayerContent(
            uiState = uiState,
            onExit = { immersiveMode = false },
            onLyricClick = viewModel::seekToLyric
        )
        return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .consumePointersUnlessResumed() 
    ) {
        // Haze 源：模糊封面作氛围底，避免歌词延伸到控制栏下当背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = playerHazeState)
                .background(colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // 歌词止于控制栏上方，不钻到磨砂玻璃底下
                    .padding(bottom = PlayerBottomControlsInset)
            ) {
                PlayerTopBar(
                    onBack = onBack,
                    isDownloaded = uiState.isDownloaded,
                    isDownloading = uiState.isDownloading,
                    downloadProgress = uiState.downloadProgress,
                    onDownloadClick = {
                        when {
                            uiState.isDownloaded || uiState.isDownloading -> onDownloadsClick()
                            else -> downloadSheetOpen = true
                        }
                    },
                    onImmersiveClick = { immersiveMode = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = uiState.songName.ifBlank { "未知歌曲" },
                    color = colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 26.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = uiState.artistName.ifBlank { "未知歌手" },
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 中部：封面 + 歌词，歌词只占封面与控制栏之间
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlayerAlbumArt(
                        coverUrl = uiState.coverUrl,
                        songName = uiState.songName,
                        isPlaying = uiState.isPlaying,
                        decodeSizeFraction = 0.7f,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .aspectRatio(1f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PlayerLyricsSection(
                        lyrics = uiState.lyrics,
                        activeIndex = uiState.activeLyricIndex,
                        fallbackText = uiState.songName,
                        compact = true,
                        onLyricClick = viewModel::seekToLyric,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }

        // 与内容平级，在 hazeSource 外做 hazeEffect
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 14.dp)
        ) {
            PlayerControlsCard(
                hazeState = playerHazeState,
                uiState = uiState,
                positionState = viewModel.positionState,
                onSeek = viewModel::seekTo,
                onTogglePlayPause = viewModel::togglePlayPause,
                onSkipPrevious = viewModel::skipToPrevious,
                onSkipNext = viewModel::skipToNext
            )

            Spacer(modifier = Modifier.height(10.dp))

            PlayerToolBar(
                hazeState = playerHazeState,
                playMode = uiState.playMode,
                isFavorite = uiState.isFavorite,
                onPlayModeClick = viewModel::cyclePlayMode,
                onFavoriteClick = viewModel::toggleFavorite,
                onQueueClick = { queueSheetOpen = true }
            )
        }

        if (queueSheetOpen) {
            PlayerQueueBottomSheet(
                queue = uiState.queue,
                currentIndex = uiState.queueIndex,
                isPlaying = uiState.isPlaying,
                onDismiss = { queueSheetOpen = false },
                onSongClick = { index ->
                    viewModel.playQueueItemAt(index)
                },
                onRemoveSong = viewModel::removeFromQueue,
                onClearQueue = viewModel::clearQueue
            )
        }

        if (downloadSheetOpen && uiState.songId > 0L) {
            DownloadQualityBottomSheet(
                song = Song(
                    id = uiState.songId,
                    name = uiState.songName,
                    artists = uiState.artistName,
                    album = uiState.albumName,
                    coverUrl = uiState.coverUrl,
                    durationMs = uiState.durationMs
                ),
                onDismiss = { downloadSheetOpen = false },
                onConfirm = { quality ->
                    downloadSheetOpen = false
                    viewModel.downloadCurrentSong(quality)
                }
            )
        }
    }
}

// 顶栏：收起 + 下载 + 进入沉浸聆听
@Composable
private fun PlayerTopBar(
    onBack: () -> Unit,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    onDownloadClick: () -> Unit,
    onImmersiveClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    // 与 SearchTopBar 对齐：top 14dp + 行高 46dp，按钮在行内垂直居中
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlayerIconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "收起播放器",
                tint = colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PlayerDownloadButton(
            isDownloaded = isDownloaded,
            isDownloading = isDownloading,
            downloadProgress = downloadProgress,
            onClick = onDownloadClick
        )

        PlayerIconButton(onClick = onImmersiveClick) {
            Icon(
                painter = painterResource(R.drawable.ic_maximize_2),
                contentDescription = "进入沉浸聆听",
                tint = colorScheme.onBackground,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// 下载按钮：下载中描边为真实字节环形进度；已下载 / 下载中点击进列表
@Composable
private fun PlayerDownloadButton(
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val progress = downloadProgress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colorScheme.surfaceVariant)
            .then(
                if (isDownloading) {
                    Modifier
                } else {
                    Modifier.border(0.67.dp, colorScheme.outlineVariant, CircleShape)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isDownloading) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(36.dp),
                color = colorScheme.primary,
                strokeWidth = 1.5.dp,
                trackColor = colorScheme.outlineVariant,
                strokeCap = StrokeCap.Round
            )
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = "下载中，查看本地下载",
                tint = colorScheme.onBackground,
                modifier = Modifier.size(16.dp)
            )
        } else if (isDownloaded) {
            Icon(
                imageVector = Icons.Outlined.DownloadDone,
                contentDescription = "已下载，查看本地下载",
                tint = colorScheme.onBackground,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = "下载",
                tint = colorScheme.onBackground,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// 与 SearchScreen 顶栏一致的圆形图标按钮
@Composable
private fun PlayerIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
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

// 专辑封面
@Composable
private fun PlayerAlbumArt(
    coverUrl: String?,
    songName: String,
    isPlaying: Boolean,
    decodeSizeFraction: Float,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val decodeSize = LocalConfiguration.current.screenWidthDp.dp * decodeSizeFraction
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(34.dp))
            .border(1.dp, colorScheme.primary, RoundedCornerShape(34.dp))
    ) {
        AsyncImage(
            model = rememberCoverRequest(coverUrl, decodeSize),
            contentDescription = songName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

// 歌词列表：限制在给定区域内滚动，当前句跟随并放大，点击某句 seek
@Composable
private fun PlayerLyricsSection(
    lyrics: List<com.leo.lune.domain.model.LyricLine>,
    activeIndex: Int,
    fallbackText: String,
    // true = 常规播放页紧凑行高；false = 沉浸模式大字号
    compact: Boolean,
    onLyricClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val lineHeightDp = if (compact) 36.dp else 40.dp
    val listState = rememberLazyListState()

    val lyricLines = remember(lyrics, fallbackText) {
        if (lyrics.isNotEmpty()) {
            lyrics.map { it.text }
        } else {
            listOf(fallbackText.ifBlank { "暂无歌词" })
        }
    }

    val resolvedActiveIndex =
        if (lyrics.isEmpty()) 0 else activeIndex.coerceIn(0, lyricLines.lastIndex)

    // 当前句滚到可视区偏上位置
    LaunchedEffect(resolvedActiveIndex, lyricLines.size) {
        if (lyricLines.isEmpty()) return@LaunchedEffect
        val target = (resolvedActiveIndex - if (compact) 1 else 2).coerceAtLeast(0)
        listState.animateScrollToItem(target)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.clipToBounds(),
        horizontalAlignment = Alignment.CenterHorizontally,
        userScrollEnabled = lyrics.isNotEmpty()
    ) {
        itemsIndexed(
            items = lyricLines,
            key = { index, line -> "$index-$line" }
        ) { index, line ->
            val isActive = lyrics.isNotEmpty() && index == resolvedActiveIndex
            val scale by animateFloatAsState(
                targetValue = if (isActive) if (compact) 1.025f else 1.05f else 1f,
                animationSpec = tween(500),
                label = "lyric-scale-$index"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(lineHeightDp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable(
                        enabled = lyrics.isNotEmpty(),
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onLyricClick(index) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = line,
                    color = if (isActive) {
                        colorScheme.onBackground
                    } else {
                        colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                    fontSize = if (compact) 17.sp else 17.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    lineHeight = if (compact) 25.sp else 25.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// 与 MiniPlayerBar 同款磨砂玻璃容器：阴影 + haze + 顶边高光
@Composable
private fun PlayerGlassSurface(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = cardShape,
                spotColor = Color(0xB3000000),
                ambientColor = Color(0xB3000000)
            )
            .clip(cardShape)
            .hazeEffect(state = hazeState) {
                blurRadius = 24.dp
                tints = listOf(
                    HazeTint(colorScheme.background.copy(alpha = 0.5f)),
                    HazeTint(Color.White.copy(alpha = 0.08f))
                )
                noiseFactor = 0.15f
            }
            .border(1.dp, Color(0x26FFFFFF), cardShape)
    ) {
        // 顶部细高光，增强玻璃质感
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x66FFFFFF),
                            Color.Transparent
                        )
                    )
                )
        )
        content()
    }
}

// 底部工具栏：播放模式 / 收藏 / 视频占位 / 队列
@Composable
private fun PlayerToolBar(
    hazeState: HazeState,
    playMode: PlayerPlayMode,
    isFavorite: Boolean,
    onPlayModeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onQueueClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    PlayerGlassSurface(hazeState = hazeState) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerToolButton(onClick = onPlayModeClick) {
                Icon(
                    imageVector = when (playMode) {
                        PlayerPlayMode.Shuffle -> Icons.Outlined.Shuffle
                        PlayerPlayMode.Loop -> Icons.Outlined.Repeat
                        PlayerPlayMode.Single -> Icons.Outlined.RepeatOne
                    },
                    contentDescription = when (playMode) {
                        PlayerPlayMode.Shuffle -> "随机播放"
                        PlayerPlayMode.Loop -> "列表循环"
                        PlayerPlayMode.Single -> "单曲循环"
                    },
                    tint = colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
            PlayerToolButton(onClick = onFavoriteClick) {
                Icon(
                    painter = painterResource(
                        if (isFavorite) R.drawable.ic_heart2 else R.drawable.ic_heart
                    ),
                    contentDescription = if (isFavorite) "取消收藏" else "收藏",
                    tint = if (isFavorite) Color.Unspecified else colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
            // 音乐视频入口占位，暂无点击逻辑
            PlayerToolButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = "观看音乐视频",
                    tint = colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
            PlayerToolButton(onClick = onQueueClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                    contentDescription = "打开播放队列",
                    tint = colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerToolButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// 进度条 + 上一首 / 播放暂停 / 下一首
@Composable
private fun PlayerControlsCard(
    hazeState: HazeState,
    uiState: PlayerUiState,
    positionState: StateFlow<PlaybackPosition>,
    onSeek: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    // 高频进度仅在本卡片内订阅，重组范围被限制在这里，不波及歌词/封面
    val playbackPosition by positionState.collectAsStateWithLifecycle()
    // 播放器时长优先；未就绪时退回 Song 元数据时长
    val durationMs = playbackPosition.durationMs.takeIf { it > 0L } ?: uiState.durationMs
    // 拖动中用本地进度，松手后再 seek；切歌时重置
    var dragFraction by remember(uiState.songId) { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val progressFraction = when {
        isDragging -> dragFraction
        durationMs > 0L -> (playbackPosition.positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        else -> 0f
    }
    val displayPositionMs = if (isDragging && durationMs > 0L) {
        (dragFraction * durationMs).toLong()
    } else {
        playbackPosition.positionMs
    }

    PlayerGlassSurface(hazeState = hazeState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 当前时间 / 总时长，与进度条左右对齐
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatSongDuration(displayPositionMs),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = formatSongDuration(durationMs),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            when {
                uiState.isLoading -> {
                }

                uiState.error != null || uiState.downloadError != null -> {
                    Text(
                        text = uiState.error ?: uiState.downloadError.orEmpty(),
                        color = colorScheme.primary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                else -> {
                    // 细圆角进度条，无拇指；拖动结束再回调 onSeek
                    PlayerProgressBar(
                        progress = progressFraction.coerceIn(0f, 1f),
                        onProgressChange = { fraction ->
                            isDragging = true
                            dragFraction = fraction
                        },
                        onProgressChangeFinished = {
                            isDragging = false
                            if (durationMs > 0L) {
                                onSeek((dragFraction * durationMs).toLong())
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "上一首",
                    tint = colorScheme.onBackground,
                    modifier = Modifier
                        .size(30.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSkipPrevious
                        )
                )

                Spacer(modifier = Modifier.size(36.dp))

                Box(
                    modifier = Modifier
                        .size(53.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primary)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onTogglePlayPause
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            if (uiState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (uiState.isPlaying) "暂停" else "播放",
                        colorFilter = ColorFilter.tint(colorScheme.onPrimary),
                        modifier = Modifier.size(21.dp)
                    )
                }

                Spacer(modifier = Modifier.size(36.dp))

                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "下一首",
                    tint = colorScheme.onBackground,
                    modifier = Modifier
                        .size(30.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSkipNext
                        )
                )
            }
        }
    }
}

// 细圆角双色进度条，无可见拇指；整条可点按 / 拖动
@Composable
private fun PlayerProgressBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val fraction = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            // 扩大触控高度，视觉仍是 6dp 细条
            .height(12.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = onProgressChangeFinished,
                    onDragCancel = onProgressChangeFinished,
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        onProgressChange((change.position.x / width).coerceIn(0f, 1f))
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    onProgressChange((offset.x / width).coerceIn(0f, 1f))
                    onProgressChangeFinished()
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // 底轨
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(colorScheme.onBackground.copy(alpha = 0.2f))
        )
        // 已播放
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(colorScheme.onBackground)
        )
    }
}

// 沉浸聆听：大封面 + 歌词；顶栏右上角内收按钮退出（位置与进入沉浸按钮一致）
@Composable
private fun ImmersivePlayerContent(
    uiState: PlayerUiState,
    onExit: () -> Unit,
    onLyricClick: (Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, bottom = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayerAlbumArt(
                coverUrl = uiState.coverUrl,
                songName = uiState.songName,
                isPlaying = uiState.isPlaying,
                decodeSizeFraction = 0.4f,
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .aspectRatio(1f)
            )

            Text(
                text = "${uiState.songName.ifBlank { "未知歌曲" }} · ${uiState.artistName.ifBlank { "未知歌手" }}",
                color = colorScheme.primary,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 16.dp)
            )

            PlayerLyricsSection(
                lyrics = uiState.lyrics,
                activeIndex = uiState.activeLyricIndex,
                fallbackText = uiState.songName,
                compact = false,
                onLyricClick = onLyricClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }

        // 与 PlayerTopBar 同一套布局：top 14dp + 行高 46dp，按钮靠右
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            PlayerIconButton(onClick = onExit) {
                Icon(
                    painter = painterResource(R.drawable.ic_minimize_2),
                    contentDescription = "退出沉浸聆听",
                    tint = colorScheme.onBackground,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
