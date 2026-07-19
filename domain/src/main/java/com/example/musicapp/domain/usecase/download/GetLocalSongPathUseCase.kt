package com.example.musicapp.domain.usecase.download

import com.example.musicapp.domain.repository.DownloadRepository
import javax.inject.Inject

// 查询歌曲本地文件路径（未下载则为 null）
class GetLocalSongPathUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(songId: Long): String? {
        return downloadRepository.getLocalPath(songId)
    }
}
