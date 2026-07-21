package com.example.musicapp.domain.model

import androidx.compose.runtime.Immutable

// 本地播放统计：本周次数 + 累计听歌时长
@Immutable
data class PlayStats(
    // 本周播放次数（不去重；跨周显示为 0）
    val weekPlayCount: Int = 0,
    // 累计实际听歌毫秒数
    val totalListenDurationMs: Long = 0L
)
