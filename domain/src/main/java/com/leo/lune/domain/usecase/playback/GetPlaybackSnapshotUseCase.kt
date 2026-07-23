package com.leo.lune.domain.usecase.playback

import com.leo.lune.domain.model.PlaybackSnapshot
import com.leo.lune.domain.repository.PlaybackSnapshotRepository
import javax.inject.Inject

// 读取上次保存的播放快照；无快照或数据损坏时返回 null
class GetPlaybackSnapshotUseCase @Inject constructor(
    private val playbackSnapshotRepository: PlaybackSnapshotRepository
) {
    suspend operator fun invoke(): PlaybackSnapshot? {
        return playbackSnapshotRepository.get()
    }
}
