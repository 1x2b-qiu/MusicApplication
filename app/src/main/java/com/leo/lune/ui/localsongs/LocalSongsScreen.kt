package com.leo.lune.ui.localsongs

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.leo.lune.R
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.DownloadedSong
import com.leo.lune.ui.home.formatSongDuration
import com.leo.lune.util.ClearFocusOnImeHidden
import com.leo.lune.util.consumePointersUnlessResumed
import com.leo.lune.util.dismissKeyboardOnTap
import com.leo.lune.util.rememberCoverRequest
import com.leo.lune.util.rememberDismissKeyboard
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

// 「本地歌曲」全量页：布局对齐「我喜欢的」
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LocalSongsScreen(
    onBack: () -> Unit,
    darkTheme: Boolean,
    hazeState: HazeState,
    viewModel: LocalSongsViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val dismissKeyboard = rememberDismissKeyboard()
    ClearFocusOnImeHidden()
    val submitSearch: () -> Unit = {
        viewModel.confirmSearch()
        dismissKeyboard()
    }

    // 底部留白：迷你播放栏 66dp + 导航层间距 12dp
    val miniPlayerBottomInset = 78.dp
    val statusMessage = when {
        uiState.isLoading && uiState.songs.isEmpty() -> "加载中…"
        uiState.filteredSongs.isEmpty() && uiState.activeKeyword.isNotBlank() -> "没有匹配的歌曲"
        uiState.filteredSongs.isEmpty() -> "暂无本地歌曲"
        else -> null
    }
    val displaySongCount = uiState.trackCount.takeIf { it > 0 } ?: uiState.songs.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .consumePointersUnlessResumed()
    ) {
        LocalSongsBackdrop(coverUrl = uiState.coverUrl ?: uiState.songs.firstOrNull()?.coverUrl)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            LocalSongsTopBar(
                query = uiState.query,
                darkTheme = darkTheme,
                hazeState = hazeState,
                onBack = onBack,
                onQueryChange = viewModel::onQueryChange,
                onClearQuery = { viewModel.onQueryChange("") },
                onConfirmSearch = submitSearch,
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = miniPlayerBottomInset + 20.dp)
                    .dismissKeyboardOnTap()
            ) {
                LocalSongsIntroTitle()
                LocalSongsIdentityRow(
                    songCount = displaySongCount,
                    coverUrl = uiState.coverUrl ?: uiState.songs.firstOrNull()?.coverUrl,
                    isPlayingLocal = uiState.hasStartedPlayAll && uiState.isPlaying,
                    onPlayAllClick = {
                        dismissKeyboard()
                        viewModel.onPlayAllClick()
                    }
                )
                if (statusMessage != null) {
                    LocalSongsStatusText(
                        text = statusMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(
                            items = uiState.filteredSongs,
                            key = { _, song -> "${song.songId}_${song.bitrate}" }
                        ) { index, song ->
                            LocalSongsTrackRow(
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

@Composable
private fun LocalSongsBackdrop(coverUrl: String?) {
    val colorScheme = MaterialTheme.colorScheme
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = rememberCoverRequest(coverUrl, screenWidth),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.35f
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp)
                    .size(176.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.background.copy(alpha = 0.2f),
                            colorScheme.background.copy(alpha = 0.72f),
                            colorScheme.background
                        )
                    )
                )
        )
    }
}

@Composable
private fun LocalSongsTopBar(
    query: String,
    darkTheme: Boolean,
    hazeState: HazeState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onConfirmSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchFieldShape = RoundedCornerShape(12.dp)
    val colorScheme = MaterialTheme.colorScheme
    var isFocused by remember { mutableStateOf(false) }
    val fieldStyle = remember(darkTheme, colorScheme) {
        if (darkTheme) {
            LocalSongsSearchFieldStyle(
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
            LocalSongsSearchFieldStyle(
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
        LocalSongsRoundIconButton(
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

        LocalSongsRoundIconButton(
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

private data class LocalSongsSearchFieldStyle(
    val overlay: Color,
    val border: Color,
    val borderFocused: Color,
    val shadowFocused: Color,
    val placeholder: Color,
    val input: Color,
    val blurRadius: Dp,
    val hazeTints: List<HazeTint>
)

@Composable
private fun LocalSongsIntroTitle(modifier: Modifier = Modifier) {
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
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "ON THIS DEVICE",
                color = colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        Text(
            text = "本地歌曲",
            color = colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.6).sp,
            modifier = Modifier.padding(top = 5.dp)
        )
        Text(
            text = "已下载到本机，离线也能听。",
            color = colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

@Composable
private fun LocalSongsIdentityRow(
    songCount: Int,
    coverUrl: String?,
    isPlayingLocal: Boolean,
    onPlayAllClick: () -> Unit
) {
    val coverShape = RoundedCornerShape(15.dp)
    val colorScheme = MaterialTheme.colorScheme
    val playInteraction = remember { MutableInteractionSource() }
    val playScope = rememberCoroutineScope()
    val playScale = remember { Animatable(1f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                text = if (songCount > 0) "$songCount 首本地歌曲" else "暂无本地歌曲",
                color = colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "已下载 · 一键连播",
                color = colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(47.dp)
                .scale(playScale.value)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFFF4F2FB))
                .clickable(
                    enabled = songCount > 0,
                    interactionSource = playInteraction,
                    indication = null,
                    onClick = {
                        playScope.launch {
                            playScale.animateTo(0.9f, tween(60))
                            playScale.animateTo(1f, tween(100))
                        }
                        onPlayAllClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    if (isPlayingLocal) R.drawable.ic_pause else R.drawable.ic_play
                ),
                contentDescription = if (isPlayingLocal) "暂停" else "播放全部",
                colorFilter = ColorFilter.tint(Color(0xFF0E0E10)),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

// 列表行：歌手后跟时长（歌手过长省略、时长始终可见）；右侧显示音质
@Composable
private fun LocalSongsTrackRow(
    index: Int,
    song: DownloadedSong,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = (index + 1).toString(),
            color = colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Start,
            modifier = Modifier.width(20.dp)
        )
        AsyncImage(
            model = rememberCoverRequest(song.coverUrl, 43.dp),
            contentDescription = song.name,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(43.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 9.dp, end = 9.dp)
        ) {
            Text(
                text = song.name,
                color = colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // 时长紧跟歌手；歌手过长时省略，不挤掉时长
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = song.artists,
                    modifier = Modifier.weight(1f, fill = false),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (song.durationMs > 0L) {
                    Text(
                        text = " · ${formatSongDuration(song.durationMs)}",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
        Text(
            text = DownloadQuality.fromBitrate(song.bitrate).label,
            color = colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun LocalSongsStatusText(
    text: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun LocalSongsRoundIconButton(
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
