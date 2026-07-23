package com.leo.lune.data.remote.response

import com.google.gson.annotations.SerializedName

// 用户歌单列表接口响应
data class UserPlaylistResponse(
  // 业务状态码，200 表示成功
  val code: Int,
  // 用户创建的歌单列表
  val playlist: List<PlaylistDto>?
)

// 歌单元数据
data class PlaylistDto(
  val id: Long,
  val name: String,
  // 歌单内歌曲数量
  val trackCount: Int,
  @SerializedName("coverImgUrl") val coverImgUrl: String?,
  // 特殊类型：10 表示「我喜欢的音乐」
  val specialType: Int?
)
