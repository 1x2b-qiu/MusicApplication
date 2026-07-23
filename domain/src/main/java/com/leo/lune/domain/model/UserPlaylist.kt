package com.leo.lune.domain.model

import androidx.compose.runtime.Immutable

// 用户创建或收藏的歌单
@Immutable
data class UserPlaylist(
    // 歌单 ID
    val id: Long,
    // 歌单名称
    val name: String,
    // 歌单内歌曲数量
    val trackCount: Int,
    // 歌单封面 URL，可能为空
    val coverUrl: String?,
    // 5 表示「我喜欢的音乐」歌单
    val specialType: Int?
) {
    // 是否为「我喜欢的音乐」特殊歌单
    val isLikedMusicPlaylist: Boolean
        get() = specialType == 5
}
