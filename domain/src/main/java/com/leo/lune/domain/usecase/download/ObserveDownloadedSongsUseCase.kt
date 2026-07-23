package com.leo.lune.domain.usecase.download

import com.leo.lune.domain.model.DownloadedSong
import com.leo.lune.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// 观察全部已下载歌曲（按下载时间倒序）
class ObserveDownloadedSongsUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    operator fun invoke(): Flow<List<DownloadedSong>> {
        return downloadRepository.observeDownloadedSongs()
    }
}
