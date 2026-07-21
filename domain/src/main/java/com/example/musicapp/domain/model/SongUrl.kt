package com.example.musicapp.domain.model

import androidx.compose.runtime.Immutable

// 歌曲播放地址信息
@Immutable
data class SongUrl(
    // 对应的歌曲 ID
    val songId: Long,
    // 实际流媒体 URL，未获取到时为 null
    val url: String?,
    // 比特率（kbps）
    val bitrate: Int
)
