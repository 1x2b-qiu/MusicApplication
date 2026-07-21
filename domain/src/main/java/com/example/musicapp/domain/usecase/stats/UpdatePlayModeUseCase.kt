package com.example.musicapp.domain.usecase.stats

import com.example.musicapp.domain.repository.PlayStatsRepository
import javax.inject.Inject

// 持久化当前播放模式
class UpdatePlayModeUseCase @Inject constructor(
    private val playStatsRepository: PlayStatsRepository
) {
    suspend operator fun invoke(playMode: String) {
        playStatsRepository.setPlayMode(playMode)
    }
}
