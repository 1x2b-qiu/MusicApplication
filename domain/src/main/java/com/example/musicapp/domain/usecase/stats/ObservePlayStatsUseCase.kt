package com.example.musicapp.domain.usecase.stats

import com.example.musicapp.domain.model.PlayStats
import com.example.musicapp.domain.repository.PlayStatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// 观察本地播放统计（本周次数 + 累计听歌时长）
class ObservePlayStatsUseCase @Inject constructor(
    private val playStatsRepository: PlayStatsRepository
) {
    operator fun invoke(): Flow<PlayStats> = playStatsRepository.observePlayStats()
}
