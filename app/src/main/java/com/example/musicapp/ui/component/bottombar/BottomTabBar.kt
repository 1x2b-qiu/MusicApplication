package com.example.musicapp.ui.component.bottombar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.navigation.MainTab
import com.example.musicapp.ui.home.HomeColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun BottomTabBar(
    hazeState: HazeState,
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = barShape,
                spotColor = Color(0xB3000000),
                ambientColor = Color(0xB3000000)
            )
            .clip(barShape)
            .hazeEffect(state = hazeState) {
                blurRadius = 24.dp
                tints = listOf(
                    HazeTint(Color(0xFF0E0E10).copy(alpha = 0.5f)),
                    HazeTint(Color.White.copy(alpha = 0.08f))
                )
                noiseFactor = 0.15f
            }
            .border(1.dp, Color(0x26FFFFFF), barShape)
    ) {
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(11.dp),
            horizontalArrangement = Arrangement.spacedBy(17.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom
        ) {
            MainTab.entries.forEach { tab ->
                BottomTabItem(
                    tab = tab,
                    isSelected = tab == selectedTab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    tab: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (isSelected) HomeColors.TextPrimary else HomeColors.TextSecondary
    val bgColor = if (isSelected) HomeColors.ChipSelectedBg else Color.Transparent

    Box(
        modifier = Modifier
            .width(62.dp)
            .height(53.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Image(
                painter = painterResource(tab.iconRes),
                contentDescription = tab.label,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = tab.label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 11.sp
            )
        }
    }
}
