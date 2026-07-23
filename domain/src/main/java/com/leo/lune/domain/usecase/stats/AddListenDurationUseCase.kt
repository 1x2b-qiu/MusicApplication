package com.leo.lune.domain.usecase.stats

import com.leo.lune.domain.repository.PlayStatsRepository
import javax.inject.Inject

// 累加一段实际听歌时长
class AddListenDurationUseCase @Inject constructor(
    private val playStatsRepository: PlayStatsRepository
) {
    suspend operator fun invoke(durationMs: Long) {
        playStatsRepository.addListenDuration(durationMs)
    }
}
