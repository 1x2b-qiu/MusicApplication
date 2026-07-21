package com.example.musicapp.ui.liked

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.musicapp.R
import com.example.musicapp.domain.model.Song
import com.example.musicapp.util.rememberCoverRequest
import com.example.musicapp.ui.home.formatSongDuration
import com.example.musicapp.util.ClearFocusOnImeHidden
import com.example.musicapp.util.dismissKeyboardOnTap
import com.example.musicapp.util.rememberDismissKeyboard
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

// 「我喜欢的」全量页：固定顶栏/标题/身份行 + 可滚动喜欢列表
@Composable
fun LikedScreen(
    onBack: () -> Unit,
    darkTheme: Boolean,
    hazeState: HazeState,
    viewModel: LikedViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val dismissKeyboard = rememberDismissKeyboard()
    ClearFocusOnImeHidden()
    // 确认搜索：应用本地筛选并收起键盘
    val submitSearch: () -> Unit = {
        viewModel.confirmSearch()
        dismissKeyboard()
    }

    // 底部留白：迷你播放栏 66dp + 导航层间距 12dp
    val miniPlayerBottomInset = 78.dp
    // 列表区空态/错误文案；有数据时为 null，展示 LazyColumn
    val statusMessage = when {
        uiState.isLoading && uiState.songs.isEmpty() -> "加载中…"
        uiState.error != null && uiState.songs.isEmpty() -> uiState.error
        uiState.filteredSongs.isEmpty() && uiState.activeKeyword.isNotBlank() -> "没有匹配的歌曲"
        uiState.filteredSongs.isEmpty() -> "暂无喜欢的歌曲"
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .navigationBarsPadding()
    ) {
        LikedTopBar(
            query = uiState.query,
            darkTheme = darkTheme,
            hazeState = hazeState,
            onBack = onBack,
            onQueryChange = viewModel::onQueryChange,
            onClearQuery = { viewModel.onQueryChange("") },
            onConfirmSearch = submitSearch,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
        )

        // 顶栏以下整片内容区：点空白收键盘
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = miniPlayerBottomInset + 20.dp)
                .dismissKeyboardOnTap()
        ) {
            LikedIntroTitle()
            LikedIdentityRow(
                songCount = uiState.songs.size,
                coverUrl = uiState.songs.firstOrNull()?.coverUrl,
                isPlayingLiked = uiState.hasStartedPlayAll && uiState.isPlaying,
                onPlayAllClick = {
                    dismissKeyboard()
                    viewModel.onPlayAllClick()
                }
            )
            LikedLibraryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (statusMessage != null) {
                    LikedStatusText(
                        text = statusMessage,
                        actionLabel = "重试".takeIf {
                            uiState.error != null && uiState.songs.isEmpty()
                        },
                        onAction = viewModel::onRetry.takeIf {
                            uiState.error != null && uiState.songs.isEmpty()
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(
                            items = uiState.filteredSongs,
                            key = { _, song -> song.id }
                        ) { index, song ->
                            LikedTrackRow(
                                index = index,
                                song = song,
                                onClick = {
                                    dismissKeyboard()
                                    viewModel.onSongClick(song)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// 顶栏布局与 SearchScreen 一致：返回 + 毛玻璃搜索框 + 搜索按钮
@Composable
private fun LikedTopBar(
    query: String,
    darkTheme: Boolean,
    hazeState: HazeState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onConfirmSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 顶栏搜索框圆角
    val searchFieldShape = RoundedCornerShape(12.dp)
    val colorScheme = MaterialTheme.colorScheme
    // 聚焦时加重描边与阴影
    var isFocused by remember { mutableStateOf(false) }
    // 深浅色各自一套半透明叠层 / 模糊参数
    val fieldStyle = remember(darkTheme, colorScheme) {
        if (darkTheme) {
            LikedSearchFieldStyle(
                overlay = Color.White.copy(alpha = 0.06f),
                border = Color.White.copy(alpha = 0.1f),
                borderFocused = Color.White.copy(alpha = 0.28f),
                shadowFocused = Color.Black.copy(alpha = 0.5f),
                placeholder = Color.White.copy(alpha = 0.22f),
                input = Color.White,
                blurRadius = 24.dp,
                hazeTints = listOf(
                    HazeTint(colorScheme.background.copy(alpha = 0.45f)),
                    HazeTint(Color.White.copy(alpha = 0.08f))
                )
            )
        } else {
            LikedSearchFieldStyle(
                overlay = Color.White.copy(alpha = 0.72f),
                border = Color.Black.copy(alpha = 0.08f),
                borderFocused = Color.Black.copy(alpha = 0.22f),
                shadowFocused = Color.Black.copy(alpha = 0.12f),
                placeholder = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                input = colorScheme.onBackground,
                blurRadius = 28.dp,
                hazeTints = listOf(
                    HazeTint(Color.White.copy(alpha = 0.78f)),
                    HazeTint(Color.Black.copy(alpha = 0.04f))
                )
            )
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 有输入时先清搜索，无输入时返回上一页
        LikedRoundIconButton(
            onClick = {
                if (query.isNotBlank()) {
                    onClearQuery()
                } else {
                    onBack()
                }
            },
            contentDescription = "返回"
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }

        // 毛玻璃搜索输入框
        Box(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .shadow(
                    elevation = if (isFocused) 8.dp else 4.dp,
                    shape = searchFieldShape,
                    ambientColor = if (isFocused) fieldStyle.shadowFocused else Color.Transparent,
                    spotColor = if (isFocused) fieldStyle.shadowFocused else Color.Transparent
                )
                .clip(searchFieldShape)
                .hazeEffect(state = hazeState) {
                    blurRadius = fieldStyle.blurRadius
                    tints = fieldStyle.hazeTints
                }
                .background(fieldStyle.overlay)
                .border(
                    width = 1.dp,
                    color = if (isFocused) fieldStyle.borderFocused else fieldStyle.border,
                    shape = searchFieldShape
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = TextStyle(
                    color = fieldStyle.input,
                    fontSize = 15.sp,
                    letterSpacing = 0.6.sp
                ),
                singleLine = true,
                cursorBrush = SolidColor(fieldStyle.input),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onConfirmSearch() }),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "歌曲 · 歌手 · 专辑",
                                color = fieldStyle.placeholder,
                                fontSize = 15.sp,
                                letterSpacing = 0.8.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // 有内容时右侧显示清除按钮
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            Color.White.copy(
                                alpha = if (fieldStyle.input == Color.White) 0.18f else 0.12f
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClearQuery
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "清除",
                        tint = fieldStyle.input,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        LikedRoundIconButton(
            onClick = onConfirmSearch,
            contentDescription = "搜索"
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = colorScheme.onBackground,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// 搜索框在深浅色下的视觉参数
private data class LikedSearchFieldStyle(
    val overlay: Color,
    val border: Color,
    val borderFocused: Color,
    val shadowFocused: Color,
    val placeholder: Color,
    val input: Color,
    val blurRadius: Dp,
    val hazeTints: List<HazeTint>
)

// 页头：眉题 + 大标题 + 一句副文案
@Composable
private fun LikedIntroTitle(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_heart2),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "YOUR LIBRARY",
                color = colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        Text(
            text = "我喜欢的",
            color = colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.6).sp,
//            lineHeight = 40.sp,
            modifier = Modifier.padding(top = 5.dp)
        )
        Text(
            text = "收藏下此刻，也收藏那个瞬间。",
            color = colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

// 身份行：封面 + 首数文案 + 一键连播 / 暂停
@Composable
private fun LikedIdentityRow(
    songCount: Int,
    coverUrl: String?,
    isPlayingLiked: Boolean,
    onPlayAllClick: () -> Unit
) {
    // 身份行封面圆角
    val coverShape = RoundedCornerShape(15.dp)
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.background)
            .padding(top = 6.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = rememberCoverRequest(coverUrl, 54.dp),
            contentDescription = null,
            modifier = Modifier
                .size(54.dp)
                .clip(coverShape)
                .background(colorScheme.surfaceVariant)
                .border(1.dp, colorScheme.outlineVariant, coverShape),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (songCount > 0) "$songCount 首心动收藏" else "暂无收藏",
                color = colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "全量喜欢 · 一键连播",
                color = colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        // 无歌时禁用点击
        Box(
            modifier = Modifier
                .size(47.dp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFFF4F2FB))
                .clickable(
                    enabled = songCount > 0,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPlayAllClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    if (isPlayingLiked) R.drawable.ic_pause else R.drawable.ic_play
                ),
                contentDescription = if (isPlayingLiked) "暂停" else "播放全部",
                colorFilter = ColorFilter.tint(Color(0xFF0E0E10)),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// 列表外层卡片外壳（背景 / 描边与 HomeRecentItem 对齐）
@Composable
private fun LikedLibraryCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 歌曲列表外层卡片圆角（与 HomeRecentItem 一致）
    val libraryCardShape = RoundedCornerShape(16.dp)
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .clip(libraryCardShape)
            .background(colorScheme.surfaceVariant)
            .border(1.dp, colorScheme.surfaceDim, libraryCardShape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        content()
    }
}

// 喜欢列表单行：序号 + 封面 + 歌名/歌手专辑 + 时长
@Composable
private fun LikedTrackRow(
    index: Int,
    song: Song,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = (index + 1).toString().padStart(2, '0'),
                color = colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                letterSpacing = 0.3.sp,
                modifier = Modifier.width(23.dp)
            )
            AsyncImage(
                model = rememberCoverRequest(song.coverUrl, 43.dp),
                contentDescription = song.name,
                modifier = Modifier
                    .size(43.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.name,
                    color = colorScheme.onBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(song.artists)
                        if (song.album.isNotBlank()) {
                            append(" · ")
                            append(song.album)
                        }
                    },
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Text(
                text = formatSongDuration(song.durationMs),
                color = colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

// 列表卡片内的空态 / 错误文案；可选重试入口
@Composable
private fun LikedStatusText(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                color = colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clickable(onClick = onAction)
            )
        }
    }
}

// 顶栏圆形图标按钮（返回 / 搜索）
@Composable
private fun LikedRoundIconButton(
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
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
