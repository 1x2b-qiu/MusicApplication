package com.example.musicapp.data.remote.response

import com.google.gson.annotations.SerializedName

data class SearchResponse(
  val code: Int,
  val result: SearchResult?
)

data class SearchResult(
  val songs: List<SongDto>?
)

data class SongDto(
  val id: Long,
  val name: String,
  val duration: Long,
  val artists: List<ArtistDto>?,
  val album: AlbumDto?
)

data class ArtistDto(
  val name: String
)

data class AlbumDto(
  val name: String,
  @SerializedName("picUrl") val picUrl: String?
)
