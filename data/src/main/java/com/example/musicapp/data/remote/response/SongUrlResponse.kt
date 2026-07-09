package com.example.musicapp.data.remote.response

data class SongUrlResponse(
    val code: Int,
    val data: List<SongUrlDto>?
)

data class SongUrlDto(
    val id: Long,
    val url: String?,
    val br: Int
)
