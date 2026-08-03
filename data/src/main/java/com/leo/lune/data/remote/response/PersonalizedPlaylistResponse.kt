package com.leo.lune.data.remote.response

// 推荐歌单（甄选歌单）接口响应
data class PersonalizedPlaylistResponse(
    // 业务状态码，200 表示成功
    val code: Int,
    // 推荐歌单列表
    val result: List<PersonalizedPlaylistDto>?
)

// 推荐歌单条目
data class PersonalizedPlaylistDto(
    val id: Long? = null,
    val name: String? = null,
    // 推荐文案，可作副标题
    val copywriter: String? = null,
    val picUrl: String? = null,
    // 播放量；部分响应为小数，故用 Double
    val playCount: Double? = null,
    val trackCount: Int? = null
)
