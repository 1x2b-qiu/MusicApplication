package com.leo.lune.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// 未完成的本地下载任务（进程被杀后可恢复列表；进度用临时文件 length 校正）
@Entity(tableName = "pending_downloads")
data class PendingDownloadEntity(
    @PrimaryKey val songId: Long,
    val name: String,
    val artists: String,
    val album: String,
    val coverUrl: String?,
    val durationMs: Long,
    // 对应 DownloadQuality.bitrate
    val bitrate: Int,
    // 用户暂停或进程恢复后的暂停态
    val paused: Boolean,
    // 下载过程中首次拿到的真实总长（Content-Length / Content-Range）；未知为 0
    val totalBytes: Long = 0L
)
