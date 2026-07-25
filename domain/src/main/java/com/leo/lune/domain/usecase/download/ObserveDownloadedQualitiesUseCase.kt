package com.leo.lune.domain.usecase.download

import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// 观察某首歌已下载的音质档位
class ObserveDownloadedQualitiesUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    operator fun invoke(songId: Long): Flow<Set<DownloadQuality>> {
        return downloadRepository.observeDownloadedQualities(songId)
    }
}
