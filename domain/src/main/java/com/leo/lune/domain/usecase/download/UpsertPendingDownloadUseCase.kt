package com.leo.lune.domain.usecase.download

import com.leo.lune.domain.model.PendingDownload
import com.leo.lune.domain.repository.DownloadRepository
import javax.inject.Inject

// 写入 / 更新未完成下载任务（入队、暂停态变更）
class UpsertPendingDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(pending: PendingDownload) {
        downloadRepository.upsertPendingDownload(pending)
    }
}
