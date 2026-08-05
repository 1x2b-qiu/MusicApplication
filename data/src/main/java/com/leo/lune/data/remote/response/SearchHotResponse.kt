package com.leo.lune.data.remote.response

// 热搜列表（简略）接口响应
data class SearchHotResponse(
    val code: Int,
    val result: SearchHotResult?
)

data class SearchHotResult(
    val hots: List<SearchHotItemDto>?
)

// first 为热搜关键词文案
data class SearchHotItemDto(
    val first: String? = null
)
