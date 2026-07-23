package com.leo.lune.domain.usecase.playback

import com.leo.lune.domain.model.PlaybackSnapshot
import com.leo.lune.domain.repository.PlaybackSnapshotRepository
import javax.inject.Inject

// 保存播放快照（切歌 / 暂停 / 队列变更时调用）
class SavePlaybackSnapshotUseCase @Inject constructor(
    private val playbackSnapshotRepository: PlaybackSnapshotRepository
) {
    suspend operator fun invoke(snapshot: PlaybackSnapshot) {
        playbackSnapshotRepository.save(snapshot)
    }
}
