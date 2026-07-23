package com.leo.lune.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leo.lune.data.local.entity.PendingDownloadEntity

@Dao
interface PendingDownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingDownloadEntity)

    @Query("SELECT * FROM pending_downloads")
    suspend fun getAll(): List<PendingDownloadEntity>

    @Query("UPDATE pending_downloads SET paused = :paused WHERE songId = :songId")
    suspend fun updatePaused(songId: Long, paused: Boolean)

    // 仅写入真实总长；已有有效值则不覆盖，避免重复写库
    @Query(
        """
        UPDATE pending_downloads
        SET totalBytes = :totalBytes
        WHERE songId = :songId AND totalBytes <= 0 AND :totalBytes > 0
        """
    )
    suspend fun updateTotalBytesIfAbsent(songId: Long, totalBytes: Long)

    @Query("DELETE FROM pending_downloads WHERE songId = :songId")
    suspend fun deleteById(songId: Long)
}
