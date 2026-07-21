package com.example.musicapp.data.repository.impl

import com.example.musicapp.data.util.PlayStatsWeek
import com.example.musicapp.data.local.dao.PlayStatsDao
import com.example.musicapp.data.local.entity.PlayStatsEntity
import com.example.musicapp.domain.model.PlayStats
import com.example.musicapp.domain.repository.PlayStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// 播放统计仓储实现：Room 单行表读写
@Singleton
class PlayStatsRepositoryImpl @Inject constructor(
    private val playStatsDao: PlayStatsDao
) : PlayStatsRepository {

    override fun observePlayStats(): Flow<PlayStats> {
        return playStatsDao.observe().map { entity ->
            val stats = entity ?: PlayStatsEntity()
            val weekStart = PlayStatsWeek.currentWeekStartEpochDay()
            // 跨周且尚未写入新周：对外展示本周次数为 0
            val weekCount = if (stats.weekStartEpochDay == weekStart) {
                stats.weekPlayCount
            } else {
                0
            }
            PlayStats(
                weekPlayCount = weekCount,
                totalListenDurationMs = stats.totalListenDurationMs,
                playMode = stats.playMode
            )
        }
    }

    override suspend fun recordWeekPlay() {
        val weekStart = PlayStatsWeek.currentWeekStartEpochDay()
        val current = playStatsDao.get() ?: PlayStatsEntity()
        val weekCount = if (current.weekStartEpochDay == weekStart) {
            current.weekPlayCount + 1
        } else {
            1
        }
        playStatsDao.upsert(
            current.copy(
                id = PlayStatsEntity.SINGLETON_ID,
                weekPlayCount = weekCount,
                weekStartEpochDay = weekStart
            )
        )
    }

    override suspend fun addListenDuration(durationMs: Long) {
        if (durationMs <= 0L) return
        val current = playStatsDao.get() ?: PlayStatsEntity()
        playStatsDao.upsert(
            current.copy(
                id = PlayStatsEntity.SINGLETON_ID,
                totalListenDurationMs = current.totalListenDurationMs + durationMs
            )
        )
    }

    override suspend fun setPlayMode(playMode: String) {
        val current = playStatsDao.get() ?: PlayStatsEntity()
        playStatsDao.upsert(
            current.copy(
                id = PlayStatsEntity.SINGLETON_ID,
                playMode = playMode
            )
        )
    }
}
