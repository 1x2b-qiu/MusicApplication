package com.leo.lune.domain.usecase.download

import com.leo.lune.domain.model.DownloadedSong
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.DownloadRepository
import javax.inject.Inject

// 将歌曲下载到应用私有目录
class DownloadSongUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(
        song: Song,
        quality: DownloadQuality = DownloadQuality.Default,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null,
        isCancelled: () -> Boolean = { false }
    ): DownloadedSong {
        return downloadRepository.downloadSong(song, quality, onProgress, isCancelled)
    }
}
