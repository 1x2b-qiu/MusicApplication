package com.leo.lune.domain.usecase.playback

import com.leo.lune.domain.repository.PlaybackSnapshotRepository
import javax.inject.Inject

// 清除播放快照（清空队列时调用）
class ClearPlaybackSnapshotUseCase @Inject constructor(
    private val playbackSnapshotRepository: PlaybackSnapshotRepository
) {
    suspend operator fun invoke() {
        playbackSnapshotRepository.clear()
    }
}
