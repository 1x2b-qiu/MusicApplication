package com.leo.lune.data.remote.response

// 热门歌单分类接口响应
data class PlaylistHotResponse(
    val code: Int,
    val tags: List<PlaylistHotTagDto>?
)

// 热门分类标签
data class PlaylistHotTagDto(
    val id: Long? = null,
    val name: String? = null
)
