package com.example.musicapp.domain.usecase.download

import com.example.musicapp.domain.model.DownloadedSong
import com.example.musicapp.domain.model.DownloadQuality
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.repository.DownloadRepository
import javax.inject.Inject

// 将歌曲下载到应用私有目录
class DownloadSongUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(
        song: Song,
        quality: DownloadQuality = DownloadQuality.Default,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null
    ): DownloadedSong {
        return downloadRepository.downloadSong(song, quality, onProgress)
    }
}
