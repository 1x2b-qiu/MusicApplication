package com.leo.lune.ui.main

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import com.leo.lune.navigation.MainTab
import com.leo.lune.ui.home.HomeScreen
import com.leo.lune.ui.library.LibraryScreen
import com.leo.lune.ui.radio.RadioScreen

/**
 * 主壳：三个 Tab 同层保活，切 Tab 只改显隐（对齐 the3rdworld HomeTab）。
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(
    selectedTab: MainTab,
    onLikedClick: () -> Unit,
    onRecentClick: () -> Unit,
    onDailyMixClick: () -> Unit,
    onPlaylistPlazaClick: () -> Unit,
    onChartsClick: () -> Unit = {},
    onChartClick: (Long) -> Unit = {},
    onPlaylistClick: (
        playlistId: Long,
        playlistName: String,
        coverUrl: String,
        trackCount: Int
    ) -> Unit = { _, _, _, _ -> },
    onGenreClick: (styleId: Long, styleName: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .tabVisibility(isVisible = selectedTab == MainTab.Home)
        ) {
            HomeScreen(
                onLikedClick = onLikedClick,
                onRecentClick = onRecentClick
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .tabVisibility(isVisible = selectedTab == MainTab.Radio)
        ) {
            RadioScreen()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .tabVisibility(isVisible = selectedTab == MainTab.Library)
        ) {
            LibraryScreen(
                onDailyMixClick = onDailyMixClick,
                onPlaylistPlazaClick = onPlaylistPlazaClick,
                onChartsClick = onChartsClick,
                onChartClick = onChartClick,
                onPlaylistClick = onPlaylistClick,
                onGenreClick = onGenreClick
            )
        }
    }
}

/** 控制 Tab 页显隐：保活组合，隐藏时不可点且沉底 */
private fun Modifier.tabVisibility(isVisible: Boolean): Modifier {
    return this
        .alpha(if (isVisible) 1f else 0f)
        .pointerInput(isVisible) {
            if (!isVisible) {
                detectTapGestures { }
            }
        }
        .zIndex(if (isVisible) 1f else 0f)
}
