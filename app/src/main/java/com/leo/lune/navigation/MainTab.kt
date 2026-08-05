package com.leo.lune.navigation

import androidx.annotation.DrawableRes
import com.leo.lune.R

enum class MainTab(
    val label: String,
    @DrawableRes val iconRes: Int
) {
    Home("首页", R.drawable.ic_tab_home),
    Radio("电台", R.drawable.ic_tab_radio),
    Library("曲库", R.drawable.ic_tab_library)
}

fun MainTab.toRoute(): MusicRoute = when (this) {
    MainTab.Home -> MusicRoute.Home
    MainTab.Radio -> MusicRoute.Radio
    MainTab.Library -> MusicRoute.Library
}

fun MusicRoute.toMainTab(): MainTab? = when (this) {
    MusicRoute.Home -> MainTab.Home
    MusicRoute.Radio -> MainTab.Radio
    MusicRoute.Library -> MainTab.Library
    else -> null
}

val mainTabs: List<MainTab> = MainTab.entries
