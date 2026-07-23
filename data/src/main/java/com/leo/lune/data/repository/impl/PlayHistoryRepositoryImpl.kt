package com.leo.lune.data.repository.impl

import com.leo.lune.data.local.dao.RecentPlayDao
import com.leo.lune.data.mapper.toRecentPlayEntity
import com.leo.lune.data.mapper.toSong
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.PlayHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// 最近播放仓储实现：写入 Room 并映射为领域模型
@Singleton
class PlayHistoryRepositoryImpl @Inject constructor(
    private val recentPlayDao: RecentPlayDao
) : PlayHistoryRepository {

    // 记录一次播放并裁剪超出上限的旧记录
    override suspend fun recordPlay(song: Song) {
        recentPlayDao.upsert(song.toRecentPlayEntity(playedAt = System.currentTimeMillis()))
        recentPlayDao.trimExcess(MAX_RECENT_PLAY_COUNT)
    }

    // 按播放时间倒序观察最近播放列表
    override fun observeRecentPlays(limit: Int): Flow<List<Song>> {
        return recentPlayDao.observeRecent(limit).map { entities ->
            entities.map { it.toSong() }
        }
    }

    companion object {
        // 本地最多保留的播放记录条数
        private const val MAX_RECENT_PLAY_COUNT = 50
    }
}
