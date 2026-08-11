package com.leo.lune.domain.model

import androidx.compose.runtime.Immutable

// 收藏/取消收藏歌单的操作结果
@Immutable
data class SubscribePlaylistResult(
    // 操作是否成功
    val success: Boolean,
    // 服务端返回的状态码
    val code: Int
)
