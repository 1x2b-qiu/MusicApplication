package com.leo.lune.data.remote.response

// 收藏/取消收藏歌曲接口响应
data class LikeResponse(
    // 业务状态码，200 表示成功
    val code: Int,
    // 红心歌单 ID（收藏成功时返回）
    val playlistId: Long? = null
)
