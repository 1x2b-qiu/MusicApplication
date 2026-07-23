package com.leo.lune.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leo.lune.data.local.entity.DownloadedSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedSongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadedSongEntity)

    @Query("SELECT * FROM downloaded_songs WHERE songId = :songId LIMIT 1")
    suspend fun getById(songId: Long): DownloadedSongEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE songId = :songId)")
    fun observeIsDownloaded(songId: Long): Flow<Boolean>

    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<DownloadedSongEntity>>

    @Query("DELETE FROM downloaded_songs WHERE songId = :songId")
    suspend fun deleteById(songId: Long)
}
