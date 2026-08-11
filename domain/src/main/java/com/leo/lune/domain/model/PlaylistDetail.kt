package com.leo.lune.domain.model

import androidx.compose.runtime.Immutable

// 歌单详情元数据（封面、创建者、简介等）
@Immutable
data class PlaylistDetail(
    val id: Long,
    val name: String,
    // 歌单简介，可能为空
    val description: String?,
    val coverUrl: String?,
    val trackCount: Int,
    // 风格标签，可能为空
    val tags: List<String>,
    val creatorName: String?,
    val creatorAvatarUrl: String?,
    // 当前登录用户是否已收藏；未登录时一般为 false
    val subscribed: Boolean
)
