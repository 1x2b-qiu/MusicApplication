package com.leo.lune.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leo.lune.data.local.entity.RecentPlayEntity
import kotlinx.coroutines.flow.Flow

// 最近播放本地数据访问
@Dao
interface RecentPlayDao {

    // 插入或更新；同 songId 冲突时替换，实现「最近听过排到最前」
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentPlayEntity)

    // 按播放时间倒序观察最近记录，供首页实时刷新
    @Query(
        """
        SELECT * FROM recent_plays
        ORDER BY playedAt DESC
        LIMIT :limit
        """
    )
    fun observeRecent(limit: Int): Flow<List<RecentPlayEntity>>

    // 删除超出保留上限的旧记录
    @Query(
        """
        DELETE FROM recent_plays
        WHERE songId NOT IN (
            SELECT songId FROM recent_plays
            ORDER BY playedAt DESC
            LIMIT :maxCount
        )
        """
    )
    suspend fun trimExcess(maxCount: Int)
}
