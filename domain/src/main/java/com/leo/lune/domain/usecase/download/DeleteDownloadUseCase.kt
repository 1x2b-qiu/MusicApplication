package com.leo.lune.domain.usecase.download

import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.repository.DownloadRepository
import javax.inject.Inject

// 删除指定音质的本地下载文件与元数据
class DeleteDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(songId: Long, quality: DownloadQuality) {
        downloadRepository.deleteDownload(songId, quality)
    }
}
