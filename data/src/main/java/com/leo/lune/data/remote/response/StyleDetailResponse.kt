package com.leo.lune.data.remote.response

// 曲风详情接口响应（style/detail）
data class StyleDetailResponse(
    val code: Int,
    val data: StyleDetailDto?
)

// 曲风详情：名称、简介、封面、歌曲数量文案等
data class StyleDetailDto(
    val tagId: Long? = null,
    val name: String? = null,
    val enName: String? = null,
    val desc: String? = null,
    // 封面 URL 列表，取首张即可
    val cover: List<String>? = null,
    // 展示用数量文案，如「999999+」
    val songNum: String? = null
)
