package com.example.musicapp.navigation

import androidx.annotation.DrawableRes
import com.example.musicapp.R

enum class MainTab(
    val label: String,
    @DrawableRes val iconRes: Int
) {
    Home("首页", R.drawable.ic_tab_home),
    Radio("电台", R.drawable.ic_tab_radio),
    Category("分类", R.drawable.ic_tab_category)
}

fun MainTab.toRoute(): MusicRoute = when (this) {
    MainTab.Home -> MusicRoute.Home
    MainTab.Radio -> MusicRoute.Radio
    MainTab.Category -> MusicRoute.Category
}

fun MusicRoute.toMainTab(): MainTab? = when (this) {
    MusicRoute.Home -> MainTab.Home
    MusicRoute.Radio -> MainTab.Radio
    MusicRoute.Category -> MainTab.Category
    else -> null
}

val mainTabs: List<MainTab> = MainTab.entries
