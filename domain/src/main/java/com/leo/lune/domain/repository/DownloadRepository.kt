package com.leo.lune.domain.repository

import com.leo.lune.domain.model.DownloadedSong
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.PendingDownload
import com.leo.lune.domain.model.Song
import kotlinx.coroutines.flow.Flow

// 本地下载仓储：音频文件存应用私有目录，元数据存 Room
// 同一首歌可按音质各存一份
interface DownloadRepository {

    // 下载歌曲指定音质到私有目录；该档已存在则直接返回记录
    // onProgress：已读字节 / Content-Length（未知为 -1）
    // isCancelled：为 true 时中断下载
    suspend fun downloadSong(
        song: Song,
        quality: DownloadQuality = DownloadQuality.Default,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null,
        isCancelled: () -> Boolean = { false }
    ): DownloadedSong

    // 若已下载返回本地绝对路径，否则 null
    // quality 非空时取该档；为空则取最高音质
    suspend fun getLocalPath(songId: Long, quality: DownloadQuality? = null): String?

    suspend fun isDownloaded(songId: Long): Boolean

    // 是否至少有一档已下载（播放页图标等）
    fun observeIsDownloaded(songId: Long): Flow<Boolean>

    // 已下载的音质档位集合
    fun observeDownloadedQualities(songId: Long): Flow<Set<DownloadQuality>>

    fun observeDownloadedSongs(): Flow<List<DownloadedSong>>

    // 删除指定音质的本地文件与 Room 记录
    suspend fun deleteDownload(songId: Long, quality: DownloadQuality)

    // 丢弃未完成的临时下载文件（用户取消时调用；暂停续传需保留临时文件）
    suspend fun discardPartialDownload(songId: Long, quality: DownloadQuality)

    // 未完成下载任务：入队 / 暂停态持久化；进程恢复时读取
    suspend fun upsertPendingDownload(pending: PendingDownload)

    suspend fun updatePendingPaused(songId: Long, quality: DownloadQuality, paused: Boolean)

    // 首次获知真实总长时写入；已有有效值不覆盖
    suspend fun updatePendingTotalBytes(songId: Long, quality: DownloadQuality, totalBytes: Long)

    suspend fun deletePendingDownload(songId: Long, quality: DownloadQuality)

    suspend fun getPendingDownloads(): List<PendingDownload>

    // 临时文件已写入字节数；无文件时为 0
    suspend fun getPartialDownloadBytes(songId: Long, quality: DownloadQuality): Long

    // 设置页副标题：SAF tree URI → 可读路径；未选则返回私有下载目录
    fun resolveStorageDisplayPath(treeUri: String?): String
}
