package com.example.musicapp.domain.usecase.download

import com.example.musicapp.domain.repository.DownloadRepository
import javax.inject.Inject

// 丢弃未完成的临时下载文件（取消下载时清理；暂停续传不要调用）
class DiscardPartialDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(songId: Long) {
        downloadRepository.discardPartialDownload(songId)
    }
}
