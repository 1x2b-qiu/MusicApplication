package com.example.musicapp.domain.repository

import com.example.musicapp.domain.model.PlayStats
import kotlinx.coroutines.flow.Flow

// 播放统计仓储：本周次数与累计实际听歌时长
interface PlayStatsRepository {
    // 观察统计；跨周时 weekPlayCount 按 0 展示，直到本周再次写入
    fun observePlayStats(): Flow<PlayStats>

    // 记一次播放（本周次数 +1；跨周自动清零后从 1 起）
    suspend fun recordWeekPlay()

    // 累加一段实际听歌时长
    suspend fun addListenDuration(durationMs: Long)
}
