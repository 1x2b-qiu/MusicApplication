package com.example.musicapp.data.mapper

import com.example.musicapp.data.remote.response.SongDto
import com.example.musicapp.data.remote.response.SongUrlDto
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.SongUrl

fun SongDto.toSong(): Song {
    return Song(
        id = id,
        name = name,
        artists = artists?.joinToString(" / ") { it.name }.orEmpty(),
        album = album?.name.orEmpty(),
        coverUrl = album?.picUrl,
        durationMs = duration
    )
}

fun SongUrlDto.toSongUrl(): SongUrl {
    return SongUrl(
        songId = id,
        url = url,
        bitrate = br
    )
}
