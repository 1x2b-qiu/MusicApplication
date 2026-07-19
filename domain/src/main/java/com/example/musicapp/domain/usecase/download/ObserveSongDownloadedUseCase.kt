package com.example.musicapp.domain.usecase.download

import com.example.musicapp.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// 观察某首歌是否已下载到本地
class ObserveSongDownloadedUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    operator fun invoke(songId: Long): Flow<Boolean> {
        return downloadRepository.observeIsDownloaded(songId)
    }
}
