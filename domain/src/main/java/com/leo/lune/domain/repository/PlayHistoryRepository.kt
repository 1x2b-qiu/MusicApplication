package com.leo.lune.domain.repository

import com.leo.lune.domain.model.Song
import kotlinx.coroutines.flow.Flow

// 最近播放仓储：记录本 App 内的真实播放历史
interface PlayHistoryRepository {
    // 写入一条播放记录
    suspend fun recordPlay(song: Song)

    // 观察最近播放列表，按时间倒序
    fun observeRecentPlays(limit: Int): Flow<List<Song>>
}
