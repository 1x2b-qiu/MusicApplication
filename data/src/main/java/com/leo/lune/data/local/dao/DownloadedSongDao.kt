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

    @Query("SELECT * FROM downloaded_songs WHERE songId = :songId AND bitrate = :bitrate LIMIT 1")
    suspend fun getById(songId: Long, bitrate: Int): DownloadedSongEntity?

    // 同曲全部已下载档位，按码率降序（最高音质优先）
    @Query("SELECT * FROM downloaded_songs WHERE songId = :songId ORDER BY bitrate DESC")
    suspend fun getAllBySongId(songId: Long): List<DownloadedSongEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE songId = :songId)")
    fun observeIsDownloaded(songId: Long): Flow<Boolean>

    @Query("SELECT bitrate FROM downloaded_songs WHERE songId = :songId")
    fun observeDownloadedBitrates(songId: Long): Flow<List<Int>>

    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<DownloadedSongEntity>>

    @Query("DELETE FROM downloaded_songs WHERE songId = :songId AND bitrate = :bitrate")
    suspend fun deleteById(songId: Long, bitrate: Int)
}
