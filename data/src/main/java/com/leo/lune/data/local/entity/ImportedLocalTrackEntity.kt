package com.leo.lune.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// 用户从自定义文件夹导入的本地曲目索引（不拷贝文件，不删原文件）
// uri 为 SAF 文档 URI；localId 为负数，避免与网易云 songId 冲突
@Entity(
    tableName = "imported_local_tracks",
    indices = [Index(value = ["localId"], unique = true)]
)
data class ImportedLocalTrackEntity(
    @PrimaryKey val uri: String,
    val localId: Long,
    val displayName: String,
    val title: String,
    val artists: String,
    val album: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    // 授权来源文件夹 tree URI，便于按目录失效
    val treeUri: String,
    val importedAt: Long
)
