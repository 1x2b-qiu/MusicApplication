package com.example.musicapp.domain.usecase.download

import com.example.musicapp.domain.repository.DownloadRepository
import javax.inject.Inject

// 读取临时下载文件已写入字节数
class GetPartialDownloadBytesUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(songId: Long): Long {
        return downloadRepository.getPartialDownloadBytes(songId)
    }
}
