package com.example.musicapp.domain.usecase.download

import com.example.musicapp.domain.repository.DownloadRepository
import javax.inject.Inject

// 更新未完成下载的暂停标记
class UpdatePendingDownloadPausedUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(songId: Long, paused: Boolean) {
        downloadRepository.updatePendingPaused(songId, paused)
    }
}
