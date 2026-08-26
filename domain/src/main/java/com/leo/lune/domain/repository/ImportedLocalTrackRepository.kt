package com.leo.lune.domain.repository

import com.leo.lune.domain.model.ImportedLocalTrack
import com.leo.lune.domain.model.LocalTrackImportDraft
import kotlinx.coroutines.flow.Flow

// 自定义文件夹导入的本地曲目索引；只写 Room，不复制/删除用户文件
interface ImportedLocalTrackRepository {

    // 按文档 URI 去重写入；已存在的保留 localId，只更新标签与导入时间
    suspend fun importTracks(drafts: List<LocalTrackImportDraft>): Int

    fun observeImportedTracks(): Flow<List<ImportedLocalTrack>>

    suspend fun getByLocalId(localId: Long): ImportedLocalTrack?
}
