package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.repository.PlayStatsRepository
import javax.inject.Inject

// 累加一段实际听歌时长
class AddListenDurationUseCase @Inject constructor(
    private val playStatsRepository: PlayStatsRepository
) {
    suspend operator fun invoke(durationMs: Long) {
        playStatsRepository.addListenDuration(durationMs)
    }
}
