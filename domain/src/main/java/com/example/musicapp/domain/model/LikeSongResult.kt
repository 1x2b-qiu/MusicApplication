package com.example.musicapp.domain.model

// 收藏/取消收藏歌曲的操作结果
data class LikeSongResult(
    // 操作是否成功
    val success: Boolean,
    // 服务端返回的状态码
    val code: Int
)
