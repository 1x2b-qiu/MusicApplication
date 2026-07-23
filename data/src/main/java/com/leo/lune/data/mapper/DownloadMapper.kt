package com.leo.lune.data.mapper

import com.leo.lune.data.local.entity.DownloadedSongEntity
import com.leo.lune.data.local.entity.PendingDownloadEntity
import com.leo.lune.domain.model.DownloadedSong
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.PendingDownload
import com.leo.lune.domain.model.Song

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

fun PendingDownloadEntity.toPendingDownload(): PendingDownload = PendingDownload(
    song = Song(
        id = songId,
        name = name,
        artists = artists,
        album = album,
        coverUrl = coverUrl,
        durationMs = durationMs
    ),
    quality = DownloadQuality.fromBitrate(bitrate),
    paused = paused,
    totalBytes = totalBytes
)

fun PendingDownload.toEntity(): PendingDownloadEntity = PendingDownloadEntity(
    songId = song.id,
    name = song.name,
    artists = song.artists,
    album = song.album,
    coverUrl = song.coverUrl,
    durationMs = song.durationMs,
    bitrate = quality.bitrate,
    paused = paused,
    totalBytes = totalBytes
)
