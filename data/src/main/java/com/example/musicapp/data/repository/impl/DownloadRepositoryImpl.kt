package com.example.musicapp.data.repository.impl

import com.example.musicapp.data.download.AudioFileStore
import com.example.musicapp.data.download.SongDownloader
import com.example.musicapp.data.local.dao.DownloadedSongDao
import com.example.musicapp.data.mapper.toDownloadedSong
import com.example.musicapp.data.mapper.toDownloadedSongEntity
import com.example.musicapp.data.remote.api.NeteaseApi
import com.example.musicapp.domain.model.DownloadedSong
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// 本地下载仓储实现
// 拉流地址 → 写入应用私有目录 → Room 存元数据；播放侧用路径判断是否已下载
@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val neteaseApi: NeteaseApi,
    private val downloadedSongDao: DownloadedSongDao,
    private val audioFileStore: AudioFileStore,
    private val songDownloader: SongDownloader
) : DownloadRepository {

    // 下载歌曲：库里已有且文件仍在则直接返回；否则先下到临时文件再落盘，避免半成品被当成成功
    override suspend fun downloadSong(song: Song): DownloadedSong = withContext(Dispatchers.IO) {
        val existing = downloadedSongDao.getById(song.id)
        if (existing != null && File(existing.localPath).isFile) {
            return@withContext existing.toDownloadedSong()
        }

        val response = neteaseApi.getSongUrl(song.id)
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
        val tempFile = audioFileStore.tempFile(song.id)
        val targetFile = audioFileStore.targetFile(song.id, extension)

        try {
            if (tempFile.exists()) tempFile.delete()
            val size = songDownloader.download(url, tempFile)
            if (targetFile.exists()) targetFile.delete()
            // rename 失败时退化为 copy，保证跨存储场景也能落盘
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            val entity = song.toDownloadedSongEntity(
                localPath = targetFile.absolutePath,
                bitrate = item.br,
                fileSizeBytes = size
            )
            downloadedSongDao.upsert(entity)
            entity.toDownloadedSong()
        } catch (error: Throwable) {
            // 失败时清掉临时文件，避免残留占用空间
            tempFile.delete()
            throw error
        }
    }

    // 返回可用的本地绝对路径；记录存在但文件被删则视为未下载
    override suspend fun getLocalPath(songId: Long): String? = withContext(Dispatchers.IO) {
        val entity = downloadedSongDao.getById(songId) ?: return@withContext null
        if (File(entity.localPath).isFile) entity.localPath else null
    }

    // 是否已下载且本地文件仍存在
    override suspend fun isDownloaded(songId: Long): Boolean {
        return getLocalPath(songId) != null
    }

    // 观察某首歌是否在下载表中（用于播放页按钮状态）
    override fun observeIsDownloaded(songId: Long): Flow<Boolean> {
        return downloadedSongDao.observeIsDownloaded(songId)
    }

    // 观察全部已下载列表，按下载时间倒序
    override fun observeDownloadedSongs(): Flow<List<DownloadedSong>> {
        return downloadedSongDao.observeAll().map { list ->
            list.map { it.toDownloadedSong() }
        }
    }

    // 删除私有目录文件与 Room 记录
    override suspend fun deleteDownload(songId: Long) = withContext(Dispatchers.IO) {
        audioFileStore.deleteForSong(songId)
        downloadedSongDao.deleteById(songId)
    }
}
