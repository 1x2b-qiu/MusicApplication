package com.leo.lune.domain.repository

import com.leo.lune.domain.model.DownloadedSong
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.PendingDownload
import com.leo.lune.domain.model.Song
import kotlinx.coroutines.flow.Flow

// 本地下载仓储：音频文件存应用私有目录，元数据存 Room
interface DownloadRepository {

    // 下载歌曲到私有目录；已存在则直接返回记录
    // onProgress：已读字节 / Content-Length（未知为 -1）
    // isCancelled：为 true 时中断下载
    suspend fun downloadSong(
        song: Song,
        quality: DownloadQuality = DownloadQuality.Default,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null,
        isCancelled: () -> Boolean = { false }
    ): DownloadedSong

    // 若已下载返回本地绝对路径，否则 null
    suspend fun getLocalPath(songId: Long): String?

    suspend fun isDownloaded(songId: Long): Boolean

    fun observeIsDownloaded(songId: Long): Flow<Boolean>

    fun observeDownloadedSongs(): Flow<List<DownloadedSong>>

    suspend fun deleteDownload(songId: Long)

    // 丢弃未完成的临时下载文件（用户取消时调用；暂停续传需保留临时文件）
    suspend fun discardPartialDownload(songId: Long)

    // 未完成下载任务：入队 / 暂停态持久化；进程恢复时读取
    suspend fun upsertPendingDownload(pending: PendingDownload)

    suspend fun updatePendingPaused(songId: Long, paused: Boolean)

    // 首次获知真实总长时写入；已有有效值不覆盖
    suspend fun updatePendingTotalBytes(songId: Long, totalBytes: Long)

    suspend fun deletePendingDownload(songId: Long)

    suspend fun getPendingDownloads(): List<PendingDownload>

    // 临时文件已写入字节数；无文件时为 0
    suspend fun getPartialDownloadBytes(songId: Long): Long
}
