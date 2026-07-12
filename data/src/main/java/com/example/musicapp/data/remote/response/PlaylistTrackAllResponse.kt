package com.example.musicapp.data.remote.response

// 歌单全部歌曲接口响应
data class PlaylistTrackAllResponse(
    // 业务状态码，200 表示成功
    val code: Int,
    // 歌单内的歌曲列表
    val songs: List<SongDto>?
)
