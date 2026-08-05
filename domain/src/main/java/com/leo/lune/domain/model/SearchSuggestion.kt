package com.leo.lune.domain.model

import androidx.compose.runtime.Immutable

// 搜索建议类型（对应网易云 suggest 中的歌曲 / 歌手 / 专辑）
enum class SearchSuggestionType {
    Song,
    Artist,
    Album
}

// 搜索联想项：展示文案 + 实际用于搜索的关键词
@Immutable
data class SearchSuggestion(
    val text: String,
    val keyword: String,
    val type: SearchSuggestionType
)
