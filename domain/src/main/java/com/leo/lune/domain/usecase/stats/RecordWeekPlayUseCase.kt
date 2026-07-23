package com.leo.lune.domain.usecase.stats

import com.leo.lune.domain.repository.PlayStatsRepository
import javax.inject.Inject

// 记录一次播放，本周次数 +1（跨周自动清零）
class RecordWeekPlayUseCase @Inject constructor(
    private val playStatsRepository: PlayStatsRepository
) {
    suspend operator fun invoke() {
        playStatsRepository.recordWeekPlay()
    }
}
