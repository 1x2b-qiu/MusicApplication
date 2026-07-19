package com.example.musicapp.domain.usecase.download

import com.example.musicapp.domain.model.DownloadedSong
import com.example.musicapp.domain.repository.DownloadRepository
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
