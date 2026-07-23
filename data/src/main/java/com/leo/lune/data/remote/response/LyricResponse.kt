package com.leo.lune.data.remote.response

// 歌词接口响应
data class LyricResponse(
    // 逐行歌词内容
    val lrc: LyricContentDto?
)

// 歌词内容容器
data class LyricContentDto(
    // LRC 格式原始歌词文本
    val lyric: String?
)
