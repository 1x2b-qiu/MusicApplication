package com.example.musicapp.domain.repository

import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.SongUrl

interface MusicRepository {
    suspend fun searchSongs(keywords: String, limit: Int = 20): List<Song>
    suspend fun getSongUrl(songId: Long): SongUrl
}
