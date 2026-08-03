package com.leo.lune.domain.model

import androidx.compose.runtime.Immutable

// 个性化推荐歌单（甄选歌单）
@Immutable
data class PersonalizedPlaylist(
    val id: Long,
    val name: String,
    // 推荐文案，可能为空
    val copywriter: String?,
    val coverUrl: String?,
    val trackCount: Int,
    val playCount: Long
)
