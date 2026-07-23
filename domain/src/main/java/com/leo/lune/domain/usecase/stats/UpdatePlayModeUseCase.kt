package com.leo.lune.domain.usecase.stats

import com.leo.lune.domain.repository.PlayStatsRepository
import javax.inject.Inject

// 持久化当前播放模式
class UpdatePlayModeUseCase @Inject constructor(
    private val playStatsRepository: PlayStatsRepository
) {
    suspend operator fun invoke(playMode: String) {
        playStatsRepository.setPlayMode(playMode)
    }
}
