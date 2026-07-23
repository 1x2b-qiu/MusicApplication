package com.leo.lune.domain.usecase.download

import com.leo.lune.domain.repository.DownloadRepository
import javax.inject.Inject

// 更新未完成下载的暂停标记
class UpdatePendingDownloadPausedUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(songId: Long, paused: Boolean) {
        downloadRepository.updatePendingPaused(songId, paused)
    }
}
