package com.leo.lune.domain.usecase.download

import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.repository.DownloadRepository
import javax.inject.Inject

// 查询歌曲本地文件路径（未下载则为 null）
// quality 非空取该档；为空取最高音质
class GetLocalSongPathUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(
        songId: Long,
        quality: DownloadQuality? = null
    ): String? {
        return downloadRepository.getLocalPath(songId, quality)
    }
}
