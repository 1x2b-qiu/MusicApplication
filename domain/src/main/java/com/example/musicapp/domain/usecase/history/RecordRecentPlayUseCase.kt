package com.example.musicapp.domain.usecase.history

import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.repository.PlayHistoryRepository
import javax.inject.Inject

// 记录一次成功开始播放的歌曲到本地最近播放
class RecordRecentPlayUseCase @Inject constructor(
    private val playHistoryRepository: PlayHistoryRepository
) {
    // 将歌曲写入本地最近播放记录
    suspend operator fun invoke(song: Song) {
        playHistoryRepository.recordPlay(song)
    }
}
