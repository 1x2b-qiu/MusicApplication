package com.leo.lune.domain.model

import androidx.compose.runtime.Immutable

// 已导入曲库的本地文件索引（文件仍在用户目录，仅 Room 存元数据）
@Immutable
data class ImportedLocalTrack(
    val localId: Long,
    val uri: String,
    val displayName: String,
    val title: String,
    val artists: String,
    val album: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val treeUri: String,
    val importedAt: Long
) {
    fun toSong(): Song = Song(
        id = localId,
        name = title,
        artists = artists.ifBlank { "未知艺术家" },
        album = album,
        coverUrl = null,
        durationMs = durationMs,
        localPlaybackUri = uri
    )

    fun toLocalLibraryItem(): LocalLibraryItem = LocalLibraryItem(
        song = toSong(),
        addedAt = importedAt,
        bitrate = null
    )
}

// 扫描预览写入曲库时的入参（尚无 localId）
data class LocalTrackImportDraft(
    val uri: String,
    val displayName: String,
    val title: String,
    val artists: String,
    val album: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val treeUri: String
)
