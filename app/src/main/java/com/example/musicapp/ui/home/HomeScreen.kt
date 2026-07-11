package com.example.musicapp.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.musicapp.R
import com.example.musicapp.domain.model.Song
import com.example.musicapp.ui.component.loading.Loading

@Composable
fun HomeScreen(
    onSearchClick: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeColors.Background)
    ) {
        when {
            uiState.isLoading && uiState.recommendedSongs.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Loading(isVisible = true)
                }
            }

            uiState.error != null && uiState.recommendedSongs.isEmpty() -> {
                HomeErrorContent(
                    message = uiState.error.orEmpty(),
                    onRetry = viewModel::onRetry
                )
            }

            else -> {
                HomeContent(
                    uiState = uiState,
                    onSearchClick = onSearchClick,
                    onPlaySong = viewModel::playSong,
                    onLoginClick = onLoginClick,
                    onCategorySelected = viewModel::onCategorySelected,
                    onNotificationClick = viewModel::onNotificationClick,
                    onFeaturedPlayClick = {
                        uiState.featuredSong?.let { song ->
                            viewModel.playSong(song, uiState.recommendedSongs)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onSearchClick: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onLoginClick: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onNotificationClick: () -> Unit,
    onFeaturedPlayClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 161.dp)
    ) {
        item {
            HomeHeader(
                greetingPrefix = uiState.greetingPrefix,
                userName = uiState.userName,
                hasNotification = uiState.hasNotification,
                onNotificationClick = onNotificationClick,
                onAvatarClick = {
                    if (!uiState.loginState.isLoggedIn) {
                        onLoginClick()
                    }
                }
            )
        }
        item {
            HomeSearchBar(onClick = onSearchClick)
        }
        item {
            HomeCategoryChips(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onCategorySelected = onCategorySelected
            )
        }
        item {
            HomeFeaturedCard(
                song = uiState.featuredSong,
                onPlayClick = onFeaturedPlayClick
            )
        }
        item {
            HomeSectionHeader(
                title = "为你精选",
                onViewAllClick = onSearchClick
            )
        }
        item {
            HomeRecommendedRow(
                songs = uiState.recommendedSongs,
                onPlaySong = onPlaySong
            )
        }
        item {
            HomeSectionHeader(
                title = "最近播放",
                onViewAllClick = onSearchClick
            )
        }
        items(uiState.recentSongs, key = { it.id }) { song ->
            HomeRecentItem(
                song = song,
                onClick = { onPlaySong(song, uiState.recentSongs) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun HomeHeader(
    greetingPrefix: String,
    userName: String,
    hasNotification: Boolean,
    onNotificationClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "$greetingPrefix，$userName",
                color = HomeColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.5.sp
            )
            Text(
                text = "现在听点什么？",
                color = HomeColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 39.sp,
                letterSpacing = (-0.52).sp
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(HomeColors.SurfaceBg)
                    .border(1.dp, HomeColors.SurfaceBorder, CircleShape)
                    .clickable(onClick = onNotificationClick),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_notification),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                if (hasNotification) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(HomeColors.AccentPurple)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(HomeColors.AvatarBg)
                    .border(1.dp, HomeColors.AvatarBorder, CircleShape)
                    .clickable(onClick = onAvatarClick)
            ) {
                Image(
                    painter = painterResource(R.drawable.img_avatar_default),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun HomeSearchBar(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, start = 20.dp, end = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(HomeColors.SurfaceBg)
            .border(1.dp, HomeColors.SurfaceBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.size(width = 18.dp, height = 20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "搜索歌曲、艺人、播放列表",
            color = HomeColors.TextSecondary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun HomeCategoryChips(
    categories: List<HomeCategory>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category.id == selectedCategoryId
            val bgColor = if (isSelected) HomeColors.ChipSelectedBg else HomeColors.ChipDefaultBg
            val borderColor = if (isSelected) HomeColors.ChipSelectedBorder else HomeColors.ChipDefaultBorder
            val textColor = if (isSelected) HomeColors.TextPrimary else HomeColors.TextSecondary

            Text(
                text = category.label,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(50))
                    .clickable { onCategorySelected(category.id) }
                    .padding(horizontal = 17.dp, vertical = 9.dp),
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 19.5.sp
            )
        }
    }
}

@Composable
private fun HomeFeaturedCard(
    song: Song?,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 20.dp, end = 20.dp)
            .height(258.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = HomeColors.HeroShadow,
                ambientColor = HomeColors.HeroShadow
            )
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(28.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.img_hero_featured),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x00000000),
                            Color(0x33000000),
                            Color(0xD9000000)
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(HomeColors.BadgeBg)
                        .border(1.dp, HomeColors.BadgeBorder, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "本周臻选",
                        color = HomeColors.TextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 16.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = song?.name ?: "Midnight Aurora",
                    color = HomeColors.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 35.sp,
                    letterSpacing = (-0.56).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song?.artists ?: "Lena Vø",
                    color = HomeColors.ArtistText,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(15.dp, CircleShape, spotColor = HomeColors.AccentPurple)
                    .clip(CircleShape)
                    .background(HomeColors.AccentPurple)
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_play),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    onViewAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = HomeColors.TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 28.5.sp,
            letterSpacing = (-0.38).sp
        )
        Row(
            modifier = Modifier.clickable(onClick = onViewAllClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "全部",
                color = HomeColors.TextSecondary,
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

@Composable
private fun HomeRecommendedRow(
    songs: List<Song>,
    onPlaySong: (Song, List<Song>) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            HomeRecommendedItem(
                song = song,
                onClick = { onPlaySong(song, songs) }
            )
        }
    }
}

@Composable
private fun HomeRecommendedItem(
    song: Song,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(144.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(146.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, HomeColors.SurfaceBorder, RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = song.coverUrl ?: R.drawable.img_album_cover,
                contentDescription = song.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.img_album_cover),
                error = painterResource(R.drawable.img_album_cover)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.name,
            color = HomeColors.TextPrimary,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artists,
            color = HomeColors.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeRecentItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HomeColors.CardBg)
            .border(1.dp, HomeColors.CardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = song.coverUrl ?: R.drawable.img_album_cover,
            contentDescription = song.name,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.img_album_cover),
            error = painterResource(R.drawable.img_album_cover)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = HomeColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 21.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artists,
                color = HomeColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatSongDuration(song.durationMs),
            color = HomeColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun HomeErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = HomeColors.TextSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "点击重试",
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(HomeColors.ChipSelectedBg)
                .border(1.dp, HomeColors.ChipSelectedBorder, RoundedCornerShape(50))
                .clickable(onClick = onRetry)
                .padding(horizontal = 24.dp, vertical = 10.dp),
            color = HomeColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
