package com.example.musicapp.data.remote.response

// 歌曲详情接口响应
data class SongDetailResponse(
    // 业务状态码，200 表示成功
    val code: Int,
    // 与请求 ids 对应的歌曲详情列表
    val songs: List<SongDto>?
)
