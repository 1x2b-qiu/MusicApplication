package com.example.musicapp.domain.model

// 主题设置：未手动选择时跟随系统，手动选择后固定深浅色
sealed interface ThemeSetting {
    // 跟随系统深色模式
    data object FollowSystem : ThemeSetting
    // 用户手动指定的固定主题，darkTheme 为 true 表示深色
    data class Fixed(val darkTheme: Boolean) : ThemeSetting
}