package com.example.musicapp.ui.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicapp.domain.model.Song
import com.example.musicapp.ui.home.HomeRecentItem
import com.example.musicapp.util.ClearFocusOnImeHidden
import com.example.musicapp.util.dismissKeyboardOnTap
import com.example.musicapp.util.rememberDismissKeyboard
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay

// 底部留白：迷你播放栏 66dp + 导航层间距 12dp，避免列表最后一项被遮挡
private val MiniPlayerBottomInset = 78.dp

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    darkTheme: Boolean,
    hazeState: HazeState,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val themeStyle = rememberSearchThemeStyle(darkTheme)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dismissKeyboard = rememberDismissKeyboard()
    ClearFocusOnImeHidden()

    // 主动确认搜索：执行搜索后收起键盘
    val submitSearch: () -> Unit = {
        viewModel.confirmSearch()
        dismissKeyboard()
    }

    // 入场动画：顶栏与内容区依次淡入上移
    val mounted = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(60)
        mounted.animateTo(
            1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
    }

    val showResults = uiState.activeKeyword.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
//            .navigationBarsPadding()
            .imePadding()
    ) {

        Column(modifier = Modifier.fillMaxSize()) {
            // 顶栏：返回 + 毛玻璃搜索框 + 确认按钮
            SearchTopBar(
                query = uiState.query,
                themeStyle = themeStyle,
                hazeState = hazeState,
                mountedAlpha = mounted.value,
                onBack = onBack,
                onQueryChange = viewModel::onQueryChange,
                onClearQuery = { viewModel.onQueryChange("") },
                onConfirmSearch = submitSearch,
                modifier = Modifier.padding(top = 14.dp)
            )

            // 渐变分割线
            SearchDivider(themeStyle = themeStyle)

            // 可滚动内容区：点击空白处收起键盘
            Box(
                modifier = Modifier
                    .weight(1f)
                    .dismissKeyboardOnTap()
            ) {
                when {
                    uiState.isLoading && showResults -> {
//                        Box(
//                            modifier = Modifier.fillMaxSize(),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            CircularProgressIndicator(
//                                color = colorScheme.onBackground.copy(alpha = 0.6f),
//                                strokeWidth = 2.dp
//                            )
//                        }
                    }

                    uiState.error != null && showResults -> {
                    }

                    showResults -> {
                        SearchResultsSection(
                            songs = uiState.songs,
                            themeStyle = themeStyle,
                            onSongClick = viewModel::onSongClick
                        )
                    }

                    else -> {
                        RecentSearchSection(
                            recents = uiState.recentSearches,
                            themeStyle = themeStyle,
                            mountedAlpha = mounted.value,
                            onRecentClick = {
                                viewModel.onRecentSearchClick(it)
                                dismissKeyboard()
                            },
                            onRemoveRecent = viewModel::removeRecentSearch,
                            onClearAll = viewModel::clearRecentSearches
                        )
                    }
                }
            }
        }
    }
}

// 搜索页主题样式：从 MaterialTheme 派生，深/浅两套参数
private data class SearchThemeStyle(
    val ambientGlowColor: Color,
    val ambientGlowAlpha: Float,
    val ambientGlowFocusedAlpha: Float,
    val searchFieldOverlay: Color,
    val searchFieldBorder: Color,
    val searchFieldBorderFocused: Color,
    val searchFieldShadowFocused: Color,
    val confirmButtonBorder: Color,
    val dividerBrush: Brush,
    val sectionLabelColor: Color,
    val chipBackground: Color,
    val chipBorder: Color,
    val chipTextColor: Color,
    val chipRemoveIconColor: Color,
    val emptyTextColor: Color,
    val placeholderColor: Color,
    val inputTextColor: Color,
    val iconMutedColor: Color,
    val blurRadius: Dp,
    val hazeTints: List<HazeTint>
)

@Composable
private fun rememberSearchThemeStyle(darkTheme: Boolean): SearchThemeStyle {
    val colorScheme = MaterialTheme.colorScheme
    return remember(darkTheme, colorScheme) {
        if (darkTheme) {
            // 深色主题：暗底 + 白色半透明描边/高光，贴近设计稿 #0a0a0a 风格
            SearchThemeStyle(
                ambientGlowColor = Color.White,
                ambientGlowAlpha = 0.5f,
                ambientGlowFocusedAlpha = 1f,
                searchFieldOverlay = Color.White.copy(alpha = 0.06f),
                searchFieldBorder = Color.White.copy(alpha = 0.1f),
                searchFieldBorderFocused = Color.White.copy(alpha = 0.28f),
                searchFieldShadowFocused = Color.Black.copy(alpha = 0.5f),
                confirmButtonBorder = Color.White.copy(alpha = 0.15f),
                dividerBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                ),
                sectionLabelColor = Color.White.copy(alpha = 0.45f),
                chipBackground = Color.White.copy(alpha = 0.05f),
                chipBorder = Color.White.copy(alpha = 0.1f),
                chipTextColor = Color.White.copy(alpha = 0.7f),
                chipRemoveIconColor = Color.White.copy(alpha = 0.28f),
                emptyTextColor = Color.White.copy(alpha = 0.25f),
                placeholderColor = Color.White.copy(alpha = 0.22f),
                inputTextColor = Color.White,
                iconMutedColor = Color.White.copy(alpha = 0.65f),
                blurRadius = 24.dp,
                hazeTints = listOf(
                    HazeTint(colorScheme.background.copy(alpha = 0.45f)),
                    HazeTint(Color.White.copy(alpha = 0.08f))
                )
            )
        } else {
            // 浅色主题：乳白磨砂 + 深色描边，保证对比度与可读性
            SearchThemeStyle(
                ambientGlowColor = colorScheme.tertiary,
                ambientGlowAlpha = 0.35f,
                ambientGlowFocusedAlpha = 0.65f,
                searchFieldOverlay = Color.White.copy(alpha = 0.72f),
                searchFieldBorder = Color.Black.copy(alpha = 0.08f),
                searchFieldBorderFocused = Color.Black.copy(alpha = 0.22f),
                searchFieldShadowFocused = Color.Black.copy(alpha = 0.12f),
                confirmButtonBorder = Color.Black.copy(alpha = 0.12f),
                dividerBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.06f),
                        Color.Black.copy(alpha = 0.06f),
                        Color.Transparent
                    )
                ),
                sectionLabelColor = colorScheme.onSurfaceVariant,
                chipBackground = Color.Black.copy(alpha = 0.04f),
                chipBorder = Color.Black.copy(alpha = 0.08f),
                chipTextColor = colorScheme.onBackground.copy(alpha = 0.75f),
                chipRemoveIconColor = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                emptyTextColor = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                placeholderColor = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                inputTextColor = colorScheme.onBackground,
                iconMutedColor = colorScheme.onSurfaceVariant,
                blurRadius = 28.dp,
                hazeTints = listOf(
                    HazeTint(Color.White.copy(alpha = 0.78f)),
                    HazeTint(Color.Black.copy(alpha = 0.04f))
                )
            )
        }
    }
}

// 顶栏：返回按钮、毛玻璃输入框、搜索确认按钮
@Composable
private fun SearchTopBar(
    query: String,
    themeStyle: SearchThemeStyle,
    hazeState: HazeState,
    mountedAlpha: Float,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onConfirmSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = mountedAlpha
                translationY = (1f - mountedAlpha) * -10f
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 返回：有输入时清空，无输入时返回上一页
        SearchHeaderIconButton(
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
                tint = MaterialTheme.colorScheme.onBackground,
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
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = if (isFocused) themeStyle.searchFieldShadowFocused else Color.Transparent,
                    spotColor = if (isFocused) themeStyle.searchFieldShadowFocused else Color.Transparent
                )
                .clip(RoundedCornerShape(12.dp))
                .hazeEffect(state = hazeState) {
                    blurRadius = themeStyle.blurRadius
                    tints = themeStyle.hazeTints
                }
                .background(themeStyle.searchFieldOverlay)
                .border(
                    width = 1.dp,
                    color = if (isFocused) {
                        themeStyle.searchFieldBorderFocused
                    } else {
                        themeStyle.searchFieldBorder
                    },
                    shape = RoundedCornerShape(12.dp)
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
                    color = themeStyle.inputTextColor,
                    fontSize = 15.sp,
                    letterSpacing = 0.6.sp
                ),
                singleLine = true,
                cursorBrush = SolidColor(themeStyle.inputTextColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onConfirmSearch() }),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "歌曲 · 歌手 · 专辑",
                                color = themeStyle.placeholderColor,
                                fontSize = 15.sp,
                                letterSpacing = 0.8.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // 输入框内清除按钮
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (themeStyle.inputTextColor == Color.White) 0.18f else 0.12f))
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
                        tint = themeStyle.inputTextColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // 搜索确认按钮
        SearchHeaderIconButton(
            onClick = onConfirmSearch,
            contentDescription = "搜索"
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// 与 HomeScreen 顶栏一致的圆形图标按钮
@Composable
private fun SearchHeaderIconButton(
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

// 顶栏与内容区之间的渐变分割线
@Composable
private fun SearchDivider(themeStyle: SearchThemeStyle) {
    Spacer(modifier = Modifier.height(20.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(1.dp)
            .background(themeStyle.dividerBrush)
    )
}

// 最近搜索区域
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSearchSection(
    recents: List<String>,
    themeStyle: SearchThemeStyle,
    mountedAlpha: Float,
    onRecentClick: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = mountedAlpha
                translationY = (1f - mountedAlpha) * 12f
            }
            .padding(top = 20.dp, bottom = 32.dp + MiniPlayerBottomInset)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "最近搜索",
                color = themeStyle.sectionLabelColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.5.sp
            )

            if (recents.isNotEmpty()) {
                SearchHeaderIconButton(
                    onClick = onClearAll,
                    contentDescription = "清空最近搜索"
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                )
            }
        }

        if (recents.isEmpty()) {
            Text(
                text = "暂无搜索记录",
                color = themeStyle.emptyTextColor,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                recents.forEachIndexed { index, term ->
                    RecentSearchChip(
                        term = term,
                        themeStyle = themeStyle,
                        animationDelayMs = index * 40,
                        mountedAlpha = mountedAlpha,
                        onSelect = { onRecentClick(term) },
                        onRemove = { onRemoveRecent(term) }
                    )
                }
            }
        }
    }
}

// 最近搜索 Chip：点击复搜，右侧 × 删除单条
@Composable
private fun RecentSearchChip(
    term: String,
    themeStyle: SearchThemeStyle,
    animationDelayMs: Int,
    mountedAlpha: Float,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    val chipAlpha = remember { Animatable(0f) }

    LaunchedEffect(mountedAlpha) {
        if (mountedAlpha >= 1f) {
            delay(animationDelayMs.toLong())
            chipAlpha.animateTo(1f, animationSpec = tween(500))
        }
    }

    Row(
        modifier = Modifier
            .graphicsLayer {
                alpha = chipAlpha.value
                translationY = (1f - chipAlpha.value) * 8f
            }
            .clip(RoundedCornerShape(8.dp))
            .background(themeStyle.chipBackground)
            .border(1.dp, themeStyle.chipBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = term,
            color = themeStyle.chipTextColor,
            fontSize = 13.5.sp,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSelect
                ),
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "删除",
                tint = themeStyle.chipRemoveIconColor,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

// 搜索结果列表
@Composable
private fun SearchResultsSection(
    songs: List<Song>,
    themeStyle: SearchThemeStyle,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp, bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "结果",
                color = themeStyle.sectionLabelColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.5.sp
            )
        }

        if (songs.isEmpty()) {
            SearchEmptyResults(
                themeStyle = themeStyle,
                modifier = Modifier.padding(bottom = MiniPlayerBottomInset)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = MiniPlayerBottomInset)
            ) {
                items(songs, key = { it.id }) { song ->
                    HomeRecentItem(
                        song = song,
                        onClick = { onSongClick(song) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// 无搜索结果空态
@Composable
private fun SearchEmptyResults(
    themeStyle: SearchThemeStyle,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "♪",
            color = themeStyle.emptyTextColor.copy(alpha = 0.5f),
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "未找到相关内容",
            color = themeStyle.emptyTextColor,
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )
    }
}
