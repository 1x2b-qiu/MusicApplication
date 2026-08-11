package com.leo.lune.data.remote.response

import com.google.gson.annotations.SerializedName

// 歌单详情接口响应（playlist/detail）
data class PlaylistDetailResponse(
    // 业务状态码，200 表示成功
    val code: Int,
    val playlist: PlaylistDetailDto?
)

// 歌单详情条目
data class PlaylistDetailDto(
    val id: Long,
    val name: String? = null,
    val description: String? = null,
    @SerializedName("coverImgUrl") val coverImgUrl: String? = null,
    val trackCount: Int? = null,
    val tags: List<String>? = null,
    // true 表示当前登录用户已收藏该歌单
    val subscribed: Boolean? = null,
    val creator: PlaylistCreatorDto? = null
)

// 歌单创建者
data class PlaylistCreatorDto(
    val nickname: String? = null,
    val avatarUrl: String? = null
)
