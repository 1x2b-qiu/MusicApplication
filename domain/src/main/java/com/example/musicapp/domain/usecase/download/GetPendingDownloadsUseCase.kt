package com.example.musicapp.domain.usecase.download

import com.example.musicapp.domain.model.PendingDownload
import com.example.musicapp.domain.repository.DownloadRepository
import javax.inject.Inject

// 读取全部未完成下载任务（启动恢复用）
class GetPendingDownloadsUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(): List<PendingDownload> {
        return downloadRepository.getPendingDownloads()
    }
}
