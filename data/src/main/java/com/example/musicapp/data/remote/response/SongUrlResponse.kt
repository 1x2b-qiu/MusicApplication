package com.example.musicapp.data.remote.response

// 歌曲播放地址接口响应
data class SongUrlResponse(
    // 业务状态码，200 表示成功
    val code: Int,
    val data: List<SongUrlDto>?
)

// 单首歌曲的播放地址信息
data class SongUrlDto(
    val id: Long,
    // 可播放的音频 URL，可能为空（无版权等情况）
    val url: String?,
    // 比特率（kbps）
    val br: Int
)
