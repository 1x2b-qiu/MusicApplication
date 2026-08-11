package com.leo.lune.domain.model

import androidx.compose.runtime.Immutable

// 曲风（网易云 style-tag），用于曲库「风格分类」入口
@Immutable
data class MusicStyle(
    val id: Long,
    val name: String,
    val coverUrl: String?
)

// 曲风详情元数据
@Immutable
data class MusicStyleDetail(
    val id: Long,
    val name: String,
    val description: String?,
    val coverUrl: String?,
    // 接口返回的展示文案，如「999999+」
    val songCountLabel: String?
)

// 曲风下单曲分页结果
@Immutable
data class MusicStyleSongsPage(
    val songs: List<Song>,
    // 下一页起始 cursor；无更多时为 null
    val nextCursor: Long?,
    val hasMore: Boolean,
    val total: Int
)
