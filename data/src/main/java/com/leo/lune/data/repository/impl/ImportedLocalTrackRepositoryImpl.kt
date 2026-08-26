package com.leo.lune.data.repository.impl

import androidx.room.withTransaction
import com.leo.lune.data.local.MusicDatabase
import com.leo.lune.data.local.dao.ImportedLocalTrackDao
import com.leo.lune.data.mapper.toEntity
import com.leo.lune.data.mapper.toImportedLocalTrack
import com.leo.lune.domain.model.ImportedLocalTrack
import com.leo.lune.domain.model.LocalTrackImportDraft
import com.leo.lune.domain.repository.ImportedLocalTrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// 导入本地曲目：Room 索引 + 负数 localId；不触碰用户原文件
@Singleton
class ImportedLocalTrackRepositoryImpl @Inject constructor(
    private val database: MusicDatabase,
    private val dao: ImportedLocalTrackDao
) : ImportedLocalTrackRepository {

    override suspend fun importTracks(drafts: List<LocalTrackImportDraft>): Int =
        withContext(Dispatchers.IO) {
            if (drafts.isEmpty()) return@withContext 0
            database.withTransaction {
                val uniqueDrafts = drafts.distinctBy { it.uri }
                val existingByUri = uniqueDrafts
                    .mapNotNull { dao.getByUri(it.uri) }
                    .associateBy { it.uri }
                var nextId = (dao.getMinLocalId() ?: 0L) - 1L
                val now = System.currentTimeMillis()
                val entities = uniqueDrafts.map { draft ->
                    val existing = existingByUri[draft.uri]
                    if (existing != null) {
                        draft.toEntity(localId = existing.localId, importedAt = now)
                    } else {
                        val id = nextId
                        nextId -= 1L
                        draft.toEntity(localId = id, importedAt = now)
                    }
                }
                dao.upsertAll(entities)
                entities.size
            }
        }

    override fun observeImportedTracks(): Flow<List<ImportedLocalTrack>> {
        return dao.observeAll().map { list -> list.map { it.toImportedLocalTrack() } }
    }

    override suspend fun getByLocalId(localId: Long): ImportedLocalTrack? =
        withContext(Dispatchers.IO) {
            dao.getByLocalId(localId)?.toImportedLocalTrack()
        }
}
