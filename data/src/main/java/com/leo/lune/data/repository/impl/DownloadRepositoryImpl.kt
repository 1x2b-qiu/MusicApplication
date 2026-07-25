package com.leo.lune.data.repository.impl

import com.leo.lune.data.download.AudioFileStore
import com.leo.lune.data.download.SongDownloader
import com.leo.lune.data.local.dao.DownloadedSongDao
import com.leo.lune.data.local.dao.PendingDownloadDao
import com.leo.lune.data.mapper.toDownloadedSong
import com.leo.lune.data.mapper.toDownloadedSongEntity
import com.leo.lune.data.mapper.toEntity
import com.leo.lune.data.mapper.toPendingDownload
import com.leo.lune.data.remote.api.NeteaseApi
import com.leo.lune.domain.model.DownloadedSong
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.PendingDownload
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.DownloadRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// 本地下载仓储实现
// 拉流 → 私有临时文件 → 提交到 SAF 目录或私有目录 → Room 存元数据
@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val neteaseApi: NeteaseApi,
    private val downloadedSongDao: DownloadedSongDao,
    private val pendingDownloadDao: PendingDownloadDao,
    private val audioFileStore: AudioFileStore,
    private val songDownloader: SongDownloader
) : DownloadRepository {

    // 下载指定音质：该档已有且文件仍在则直接返回；否则先下到临时文件再落盘
    override suspend fun downloadSong(
        song: Song,
        quality: DownloadQuality,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?,
        isCancelled: () -> Boolean
    ): DownloadedSong = withContext(Dispatchers.IO) {
        val existing = downloadedSongDao.getById(song.id, quality.bitrate)
        if (existing != null && audioFileStore.exists(existing.localPath)) {
            onProgress?.invoke(existing.fileSizeBytes, existing.fileSizeBytes)
            return@withContext existing.toDownloadedSong()
        }

        val response = neteaseApi.getSongUrl(songId = song.id, bitrate = quality.bitrate)
        if (response.code != 200) {
            throw IllegalStateException("获取下载地址失败：${response.code}")
        }
        val item = response.data?.firstOrNull()
            ?: throw IllegalStateException("该歌曲暂无下载权限")
        val url = item.url
        if (url.isNullOrBlank()) {
            throw IllegalStateException("该歌曲暂无下载权限")
        }

        val extension = songDownloader.guessExtension(url, contentType = null)
        val tempFile = audioFileStore.tempFile(song.id, quality.bitrate)

        try {
            // 保留已有临时文件，供 SongDownloader 通过 Range 断点续传
            val size = songDownloader.download(url, tempFile, onProgress, isCancelled)
            val localPath = audioFileStore.commitDownload(
                songId = song.id,
                bitrate = quality.bitrate,
                extension = extension,
                tempFile = tempFile
            )
            val entity = song.toDownloadedSongEntity(
                localPath = localPath,
                quality = quality,
                fileSizeBytes = size
            )
            downloadedSongDao.upsert(entity)
            // 成功落盘后清掉进行中记录
            pendingDownloadDao.deleteById(song.id, quality.bitrate)
            entity.toDownloadedSong()
        } catch (error: CancellationException) {
            // 暂停会取消协程：保留临时文件以便继续下载；用户取消由 discardPartialDownload 清理
            throw error
        } catch (error: Throwable) {
            // 真正失败时清掉临时文件，避免残留占用空间
            tempFile.delete()
            throw error
        }
    }

    // 返回本地路径/URI；指定 quality 则取该档，否则取最高音质；文件缺失则视为无
    override suspend fun getLocalPath(
        songId: Long,
        quality: DownloadQuality?
    ): String? = withContext(Dispatchers.IO) {
        if (quality != null) {
            val entity = downloadedSongDao.getById(songId, quality.bitrate) ?: return@withContext null
            return@withContext entity.localPath.takeIf { audioFileStore.exists(it) }
        }
        downloadedSongDao.getAllBySongId(songId)
            .firstOrNull { audioFileStore.exists(it.localPath) }
            ?.localPath
    }

    // 是否至少有一档已下载且本地文件仍存在
    override suspend fun isDownloaded(songId: Long): Boolean {
        return getLocalPath(songId) != null
    }

    // 观察某首歌是否至少有一档在下载表中（用于播放页按钮状态）
    override fun observeIsDownloaded(songId: Long): Flow<Boolean> {
        return downloadedSongDao.observeIsDownloaded(songId)
    }

    override fun observeDownloadedQualities(songId: Long): Flow<Set<DownloadQuality>> {
        return downloadedSongDao.observeDownloadedBitrates(songId).map { bitrates ->
            bitrates.map { DownloadQuality.fromBitrate(it) }.toSet()
        }
    }

    // 观察全部已下载列表，按下载时间倒序（同曲多档各占一行）
    override fun observeDownloadedSongs(): Flow<List<DownloadedSong>> {
        return downloadedSongDao.observeAll().map { list ->
            list.map { it.toDownloadedSong() }
        }
    }

    // 删除指定音质的本地文件与 Room 记录
    override suspend fun deleteDownload(songId: Long, quality: DownloadQuality) =
        withContext(Dispatchers.IO) {
            val existing = downloadedSongDao.getById(songId, quality.bitrate)
            audioFileStore.deleteForSong(songId, quality.bitrate, existing?.localPath)
            downloadedSongDao.deleteById(songId, quality.bitrate)
            pendingDownloadDao.deleteById(songId, quality.bitrate)
        }

    // 仅删除未完成的临时文件
    override suspend fun discardPartialDownload(songId: Long, quality: DownloadQuality) {
        withContext(Dispatchers.IO) {
            audioFileStore.tempFile(songId, quality.bitrate).delete()
        }
    }

    override suspend fun upsertPendingDownload(pending: PendingDownload) {
        withContext(Dispatchers.IO) {
            pendingDownloadDao.upsert(pending.toEntity())
        }
    }

    override suspend fun updatePendingPaused(
        songId: Long,
        quality: DownloadQuality,
        paused: Boolean
    ) {
        withContext(Dispatchers.IO) {
            pendingDownloadDao.updatePaused(songId, quality.bitrate, paused)
        }
    }

    override suspend fun updatePendingTotalBytes(
        songId: Long,
        quality: DownloadQuality,
        totalBytes: Long
    ) {
        if (totalBytes <= 0L) return
        withContext(Dispatchers.IO) {
            pendingDownloadDao.updateTotalBytesIfAbsent(songId, quality.bitrate, totalBytes)
        }
    }

    override suspend fun deletePendingDownload(songId: Long, quality: DownloadQuality) {
        withContext(Dispatchers.IO) {
            pendingDownloadDao.deleteById(songId, quality.bitrate)
        }
    }

    override suspend fun getPendingDownloads(): List<PendingDownload> = withContext(Dispatchers.IO) {
        pendingDownloadDao.getAll().map { it.toPendingDownload() }
    }

    override suspend fun getPartialDownloadBytes(
        songId: Long,
        quality: DownloadQuality
    ): Long = withContext(Dispatchers.IO) {
        val file = audioFileStore.tempFile(songId, quality.bitrate)
        if (file.isFile) file.length() else 0L
    }
}
