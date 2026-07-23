package com.leo.lune.domain.repository

import com.leo.lune.domain.model.PlaybackSnapshot

// 播放快照仓储：进程级恢复播放状态
interface PlaybackSnapshotRepository {

    // 保存当前播放快照（切歌 / 暂停 / 队列变更时调用）
    suspend fun save(snapshot: PlaybackSnapshot)

    // 读取上次保存的快照；无快照或数据损坏时返回 null
    suspend fun get(): PlaybackSnapshot?

    // 清除快照（清空队列时调用）
    suspend fun clear()
}
