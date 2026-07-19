package com.example.musicapp.domain.repository

import com.example.musicapp.domain.model.DownloadedSong
import com.example.musicapp.domain.model.Song
import kotlinx.coroutines.flow.Flow

// 本地下载仓储：音频文件存应用私有目录，元数据存 Room
interface DownloadRepository {

    // 下载歌曲到私有目录；已存在则直接返回记录
    suspend fun downloadSong(song: Song): DownloadedSong

    // 若已下载返回本地绝对路径，否则 null
    suspend fun getLocalPath(songId: Long): String?

    suspend fun isDownloaded(songId: Long): Boolean

    fun observeIsDownloaded(songId: Long): Flow<Boolean>

    fun observeDownloadedSongs(): Flow<List<DownloadedSong>>

    suspend fun deleteDownload(songId: Long)
}
