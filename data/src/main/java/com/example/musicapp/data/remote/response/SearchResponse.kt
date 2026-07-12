package com.example.musicapp.data.remote.response

import com.google.gson.annotations.SerializedName

// 搜索接口响应
data class SearchResponse(
  // 业务状态码，200 表示成功
  val code: Int,
  val result: SearchResult?
)

// 搜索结果分页数据
data class SearchResult(
  val songs: List<SongDto>?
)

// 歌曲 DTO，兼容网易云 API 多种字段命名
data class SongDto(
  val id: Long,
  val name: String,
  // 时长（毫秒），部分接口用 duration
  @SerializedName("duration") val duration: Long = 0,
  // 时长（毫秒），部分接口用 dt
  @SerializedName("dt") val dt: Long = 0,
  @SerializedName("artists") val artists: List<ArtistDto>? = null,
  @SerializedName("ar") val ar: List<ArtistDto>? = null,
  @SerializedName("album") val album: AlbumDto? = null,
  @SerializedName("al") val al: AlbumDto? = null
)

// 歌手信息
data class ArtistDto(
  val name: String
)

// 专辑信息
data class AlbumDto(
  val name: String,
  // 专辑封面 URL
  @SerializedName("picUrl") val picUrl: String?
)
