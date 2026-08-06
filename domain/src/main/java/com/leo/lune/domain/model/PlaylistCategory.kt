package com.leo.lune.domain.model

import androidx.compose.runtime.Immutable

// 热门歌单分类标签（歌单广场 Tab；不含封面）
@Immutable
data class PlaylistCategory(
    val id: Long,
    val name: String
)
