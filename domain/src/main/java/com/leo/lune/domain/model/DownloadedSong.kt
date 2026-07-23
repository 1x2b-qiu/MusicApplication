package com.leo.lune.domain.model

import androidx.compose.runtime.Immutable

// 已下载到本地的歌曲元数据
@Immutable
data class DownloadedSong(
    val songId: Long,
    val name: String,
    val artists: String,
    val album: String,
    val coverUrl: String?,
    val durationMs: Long,
    // 应用私有目录下的绝对路径
    val localPath: String,
    val bitrate: Int,
    val fileSizeBytes: Long,
    val downloadedAt: Long
) {
    fun toSong(): Song = Song(
        id = songId,
        name = name,
        artists = artists,
        album = album,
        coverUrl = coverUrl,
        durationMs = durationMs
    )
}
