package com.example.musicapp.domain.usecase.download

import com.example.musicapp.domain.repository.DownloadRepository
import javax.inject.Inject

// 删除本地下载文件与元数据
class DeleteDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(songId: Long) {
        downloadRepository.deleteDownload(songId)
    }
}
