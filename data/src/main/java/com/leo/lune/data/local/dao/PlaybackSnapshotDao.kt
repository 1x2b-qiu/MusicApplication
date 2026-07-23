package com.leo.lune.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leo.lune.data.local.entity.PlaybackSnapshotEntity

// 播放快照 DAO：单行读写
@Dao
interface PlaybackSnapshotDao {

    // 插入或替换快照（单行，整表只保留最新一次）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlaybackSnapshotEntity)

    // 读取快照；无快照时返回 null
    @Query("SELECT * FROM playback_snapshot WHERE id = ${PlaybackSnapshotEntity.SINGLETON_ID}")
    suspend fun get(): PlaybackSnapshotEntity?

    // 清除快照（清空队列时调用）
    @Query("DELETE FROM playback_snapshot WHERE id = ${PlaybackSnapshotEntity.SINGLETON_ID}")
    suspend fun delete()
}
