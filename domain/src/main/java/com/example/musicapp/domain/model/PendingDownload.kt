package com.example.musicapp.domain.model

import androidx.compose.runtime.Immutable

// 未完成的本地下载任务（持久化用；已下字节用临时文件 length，总长优先用 totalBytes）
@Immutable
data class PendingDownload(
    val song: Song,
    val quality: DownloadQuality,
    val paused: Boolean,
    // 真实文件总长；0 表示尚未从下载响应获知
    val totalBytes: Long = 0L
)
