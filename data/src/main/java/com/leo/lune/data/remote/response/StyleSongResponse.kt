package com.leo.lune.data.remote.response

// 曲风歌曲接口响应（style/song）
data class StyleSongResponse(
    val code: Int,
    val data: StyleSongDataDto?
)

data class StyleSongDataDto(
    val page: StyleSongPageDto? = null,
    val songs: List<SongDto>? = null
)

// 曲风歌曲分页；下一页 cursor = 当前 cursor + size
data class StyleSongPageDto(
    val cursor: Long? = null,
    val size: Int? = null,
    val total: Int? = null,
    val more: Boolean? = null
)
