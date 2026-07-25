package com.leo.lune.data.local.entity

import androidx.room.Entity

// 未完成的本地下载任务（进程被杀后可恢复列表；进度用临时文件 length 校正）
// 同一首歌可并行下不同音质（主键 songId + bitrate）
@Entity(
    tableName = "pending_downloads",
    primaryKeys = ["songId", "bitrate"]
)
data class PendingDownloadEntity(
    val songId: Long,
    // 对应 DownloadQuality.bitrate
    val bitrate: Int,
    val name: String,
    val artists: String,
    val album: String,
    val coverUrl: String?,
    val durationMs: Long,
    // 用户暂停或进程恢复后的暂停态
    val paused: Boolean,
    // 下载过程中首次拿到的真实总长（Content-Length / Content-Range）；未知为 0
    val totalBytes: Long = 0L
)
