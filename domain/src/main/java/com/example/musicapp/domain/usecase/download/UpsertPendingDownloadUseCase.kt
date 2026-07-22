package com.example.musicapp.domain.usecase.download

import com.example.musicapp.domain.model.PendingDownload
import com.example.musicapp.domain.repository.DownloadRepository
import javax.inject.Inject

// 写入 / 更新未完成下载任务（入队、暂停态变更）
class UpsertPendingDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(pending: PendingDownload) {
        downloadRepository.upsertPendingDownload(pending)
    }
}
