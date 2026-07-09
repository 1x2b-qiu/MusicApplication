package com.example.musicapp.data.remote.response

data class CaptchaSentResponse(
    val code: Int,
    val data: Boolean?,
    val message: String?
)
