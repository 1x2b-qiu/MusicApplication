package com.example.musicapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.musicapp.data.local.entity.PlayStatsEntity
import kotlinx.coroutines.flow.Flow

// 播放统计 DAO：单行读写
@Dao
interface PlayStatsDao {

    @Query("SELECT * FROM play_stats WHERE id = ${PlayStatsEntity.SINGLETON_ID}")
    fun observe(): Flow<PlayStatsEntity?>

    @Query("SELECT * FROM play_stats WHERE id = ${PlayStatsEntity.SINGLETON_ID}")
    suspend fun get(): PlayStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlayStatsEntity)
}
