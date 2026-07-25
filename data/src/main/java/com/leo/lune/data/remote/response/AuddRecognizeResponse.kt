package com.leo.lune.data.remote.response

import com.google.gson.annotations.SerializedName

// AudD 标准识曲接口响应
data class AuddRecognizeResponse(
    val status: String? = null,
    val result: AuddRecognizeResult? = null,
    val error: AuddError? = null,
)

data class AuddRecognizeResult(
    val artist: String? = null,
    val title: String? = null,
    val album: String? = null,
    @SerializedName("release_date")
    val releaseDate: String? = null,
    val label: String? = null,
    val timecode: String? = null,
    @SerializedName("song_link")
    val songLink: String? = null,
)

data class AuddError(
    @SerializedName("error_code")
    val errorCode: Int? = null,
    @SerializedName("error_message")
    val errorMessage: String? = null,
)
