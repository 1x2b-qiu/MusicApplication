package com.example.musicapp.domain.repository

import com.example.musicapp.domain.model.PlayStats
import kotlinx.coroutines.flow.Flow

// 播放统计仓储：本周次数、累计听歌时长、播放模式
interface PlayStatsRepository {
    // 观察统计；跨周时 weekPlayCount 按 0 展示，直到本周再次写入
    fun observePlayStats(): Flow<PlayStats>

    // 记一次播放（本周次数 +1；跨周自动清零后从 1 起）
    suspend fun recordWeekPlay()

    // 累加一段实际听歌时长
    suspend fun addListenDuration(durationMs: Long)

    // 持久化当前播放模式（Shuffle / Loop / Single）
    suspend fun setPlayMode(playMode: String)
}
