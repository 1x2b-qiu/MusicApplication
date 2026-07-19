package com.example.musicapp.data.mapper

import com.example.musicapp.data.local.entity.DownloadedSongEntity
import com.example.musicapp.domain.model.DownloadedSong
import com.example.musicapp.domain.model.Song

fun DownloadedSongEntity.toDownloadedSong(): DownloadedSong = DownloadedSong(
    songId = songId,
    name = name,
    artists = artists,
    album = album,
    coverUrl = coverUrl,
    durationMs = durationMs,
    localPath = localPath,
    bitrate = bitrate,
    fileSizeBytes = fileSizeBytes,
    downloadedAt = downloadedAt
)

fun Song.toDownloadedSongEntity(
    localPath: String,
    bitrate: Int,
    fileSizeBytes: Long,
    downloadedAt: Long = System.currentTimeMillis()
): DownloadedSongEntity = DownloadedSongEntity(
    songId = id,
    name = name,
    artists = artists,
    album = album,
    coverUrl = coverUrl,
    durationMs = durationMs,
    localPath = localPath,
    bitrate = bitrate,
    fileSizeBytes = fileSizeBytes,
    downloadedAt = downloadedAt
)
