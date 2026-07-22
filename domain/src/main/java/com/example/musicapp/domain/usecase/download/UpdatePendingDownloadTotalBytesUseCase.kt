package com.example.musicapp.domain.usecase.download

import com.example.musicapp.domain.repository.DownloadRepository
import javax.inject.Inject

// 将下载响应中的真实总长写入 pending（仅首次有效写入）
class UpdatePendingDownloadTotalBytesUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(songId: Long, totalBytes: Long) {
        downloadRepository.updatePendingTotalBytes(songId, totalBytes)
    }
}
