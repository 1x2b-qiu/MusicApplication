package com.leo.lune.domain.model

import androidx.compose.runtime.Immutable

// 热门歌单风格分类（曲库「风格分类」）
@Immutable
data class PlaylistGenre(
    val id: Long,
    val name: String,
    val coverUrl: String?
)
