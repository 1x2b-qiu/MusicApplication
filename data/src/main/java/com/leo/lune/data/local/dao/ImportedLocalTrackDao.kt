package com.leo.lune.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leo.lune.data.local.entity.ImportedLocalTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportedLocalTrackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ImportedLocalTrackEntity>)

    @Query("SELECT * FROM imported_local_tracks WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): ImportedLocalTrackEntity?

    @Query("SELECT * FROM imported_local_tracks WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: Long): ImportedLocalTrackEntity?

    @Query("SELECT MIN(localId) FROM imported_local_tracks")
    suspend fun getMinLocalId(): Long?

    @Query("SELECT * FROM imported_local_tracks ORDER BY importedAt DESC")
    fun observeAll(): Flow<List<ImportedLocalTrackEntity>>

    @Query("DELETE FROM imported_local_tracks WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: Long)
}
