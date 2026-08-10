package com.leo.lune.navigation

import androidx.annotation.DrawableRes
import com.leo.lune.R

enum class MainTab(
    val label: String,
    @DrawableRes val iconRes: Int
) {
    Library("曲库", R.drawable.ic_tab_library),
    Radio("电台", R.drawable.ic_tab_radio),
    Home("我的", R.drawable.ic_tab_home)
}

val mainTabs: List<MainTab> = MainTab.entries
