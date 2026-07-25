package com.leo.lune.data.local.entity

import androidx.room.Entity

// 本地下载记录：音频文件在应用私有目录，此处存元数据与路径
// 同一首歌可按音质各存一条（主键 songId + bitrate）
@Entity(
    tableName = "downloaded_songs",
    primaryKeys = ["songId", "bitrate"]
)
data class DownloadedSongEntity(
    val songId: Long,
    // 对应 DownloadQuality.bitrate（请求档位，非服务端实际码率）
    val bitrate: Int,
    val name: String,
    val artists: String,
    val album: String,
    val coverUrl: String?,
    val durationMs: Long,
    val localPath: String,
    val fileSizeBytes: Long,
    val downloadedAt: Long
)
