package com.leo.lune.data.remote.response

// 曲风列表接口响应（style/list）
data class StyleListResponse(
    val code: Int,
    val data: List<StyleTagDto>?
)

// 曲风标签；L1 为顶级风格，childrenTags 为子风格
data class StyleTagDto(
    val tagId: Long? = null,
    val tagName: String? = null,
    val enName: String? = null,
    val level: Int? = null,
    val picUrl: String? = null,
    val childrenTags: List<StyleTagDto>? = null
)
