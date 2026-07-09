package com.example.musicapp.domain.model

data class Song(
    val id: Long,
    val name: String,
    val artists: String,
    val album: String,
    val coverUrl: String?,
    val durationMs: Long
)
