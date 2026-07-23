package com.leo.lune.domain.usecase.download

import com.leo.lune.domain.model.PendingDownload
import com.leo.lune.domain.repository.DownloadRepository
import javax.inject.Inject

// 读取全部未完成下载任务（启动恢复用）
class GetPendingDownloadsUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(): List<PendingDownload> {
        return downloadRepository.getPendingDownloads()
    }
}
