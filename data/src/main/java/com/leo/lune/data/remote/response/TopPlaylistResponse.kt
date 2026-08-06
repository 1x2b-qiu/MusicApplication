package com.leo.lune.data.remote.response

// 网友精选碟（分类歌单）接口响应
data class TopPlaylistResponse(
    val code: Int,
    val playlists: List<TopPlaylistDto>?
)

data class TopPlaylistDto(
    val id: Long? = null,
    val name: String? = null,
    val coverImgUrl: String? = null,
    // 播放量；部分响应为小数，故用 Double
    val playCount: Double? = null,
    val trackCount: Int? = null
)
