package com.leo.lune.data.remote.response

import com.google.gson.annotations.SerializedName

// 搜索建议接口响应（默认返回歌曲 / 歌手 / 专辑等分类）
data class SearchSuggestResponse(
    val code: Int,
    val result: SearchSuggestResult?
)

data class SearchSuggestResult(
    val songs: List<SuggestSongDto>? = null,
    val artists: List<SuggestArtistDto>? = null,
    val albums: List<SuggestAlbumDto>? = null,
    // type=mobile 时的关键词列表（兜底）
    val allMatch: List<SuggestAllMatchDto>? = null
)

data class SuggestSongDto(
    val name: String? = null,
    @SerializedName("artists") val artists: List<SuggestNameDto>? = null,
    @SerializedName("ar") val ar: List<SuggestNameDto>? = null
)

data class SuggestArtistDto(
    val name: String? = null
)

data class SuggestAlbumDto(
    val name: String? = null,
    @SerializedName("artist") val artist: SuggestNameDto? = null
)

data class SuggestNameDto(
    val name: String? = null
)

data class SuggestAllMatchDto(
    val keyword: String? = null
)
