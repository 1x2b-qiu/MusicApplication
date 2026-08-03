package com.leo.lune.data.remote.response

// 网友精选碟（分类歌单）接口响应；此处只取封面
data class TopPlaylistResponse(
    val code: Int,
    val playlists: List<TopPlaylistDto>?
)

data class TopPlaylistDto(
    val id: Long? = null,
    val coverImgUrl: String? = null
)
