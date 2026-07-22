package com.example.musicapp.domain.usecase.download

import com.example.musicapp.domain.repository.DownloadRepository
import javax.inject.Inject

// 删除未完成下载的 Room 记录（成功落盘或用户取消）
class DeletePendingDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(songId: Long) {
        downloadRepository.deletePendingDownload(songId)
    }
}
