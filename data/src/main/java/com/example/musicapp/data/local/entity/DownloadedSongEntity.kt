package com.example.musicapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// 本地下载记录：音频文件在应用私有目录，此处存元数据与路径
@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey val songId: Long,
    val name: String,
    val artists: String,
    val album: String,
    val coverUrl: String?,
    val durationMs: Long,
    val localPath: String,
    val bitrate: Int,
    val fileSizeBytes: Long,
    val downloadedAt: Long
)
