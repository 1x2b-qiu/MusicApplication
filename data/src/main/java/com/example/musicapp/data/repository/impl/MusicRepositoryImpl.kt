package com.example.musicapp.data.repository.impl

import com.example.musicapp.data.mapper.toSong
import com.example.musicapp.data.mapper.toSongUrl
import com.example.musicapp.data.remote.api.NeteaseApi
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.SongUrl
import com.example.musicapp.domain.repository.MusicRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val neteaseApi: NeteaseApi
) : MusicRepository {

    override suspend fun searchSongs(keywords: String, limit: Int): List<Song> {
        val response = neteaseApi.search(keywords, limit)
        if (response.code != 200) {
            throw IllegalStateException("Search failed with code ${response.code}")
        }
        return response.result?.songs.orEmpty().map { it.toSong() }
    }

    override suspend fun getSongUrl(songId: Long): SongUrl {
        val response = neteaseApi.getSongUrl(songId)
        if (response.code != 200) {
            throw IllegalStateException("Get song url failed with code ${response.code}")
        }
        val item = response.data?.firstOrNull()
            ?: throw IllegalStateException("No playable url for song $songId")
        return item.toSongUrl()
    }
}
