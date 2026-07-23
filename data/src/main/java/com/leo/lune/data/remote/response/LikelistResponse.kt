package com.leo.lune.data.remote.response

// 用户红心歌单 ID 列表接口响应
data class LikelistResponse(
    // 业务状态码，200 表示成功
    val code: Int,
    // 已收藏的歌曲 ID 列表
    val ids: List<Long>?
)
