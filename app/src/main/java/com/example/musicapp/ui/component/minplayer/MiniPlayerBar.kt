package com.example.musicapp.ui.component.minplayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.musicapp.R
import com.example.musicapp.domain.model.Song
import com.example.musicapp.player.rememberMusicPlayerController
import com.example.musicapp.ui.home.HomeColors

@Composable
fun MiniPlayerBar(
    onPlayerClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val playerController = rememberMusicPlayerController()
    val playbackState by playerController.playbackState.collectAsStateWithLifecycle()
    val song = playbackState.displaySong ?: return

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color(0xB3000000),
                    ambientColor = Color(0xB3000000)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x14FFFFFF))
                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp))
                .padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPlayerClick(song) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = song.coverUrl ?: R.drawable.img_album_cover,
                    contentDescription = song.name,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.img_album_cover),
                    error = painterResource(R.drawable.img_album_cover)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = song.name,
                        color = HomeColors.TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artists,
                        color = HomeColors.TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = playerController::toggleFavorite),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_heart),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    alpha = if (playbackState.isFavorite) 1f else 0.7f
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(HomeColors.AccentPurple)
                    .clickable(onClick = playerController::togglePlayPause),
                contentAlignment = Alignment.Center
            ) {
                if (playbackState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = HomeColors.TextPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Image(
                        painter = painterResource(
                            if (playbackState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = playerController::skipToNext),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_skip_next),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
