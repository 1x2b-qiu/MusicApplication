package com.leo.lune.data.mapper

import com.leo.lune.data.local.entity.ImportedLocalTrackEntity
import com.leo.lune.domain.model.ImportedLocalTrack
import com.leo.lune.domain.model.LocalTrackImportDraft

fun ImportedLocalTrackEntity.toImportedLocalTrack(): ImportedLocalTrack = ImportedLocalTrack(
    localId = localId,
    uri = uri,
    displayName = displayName,
    title = title,
    artists = artists,
    album = album,
    durationMs = durationMs,
    fileSizeBytes = fileSizeBytes,
    treeUri = treeUri,
    importedAt = importedAt
)

fun LocalTrackImportDraft.toEntity(
    localId: Long,
    importedAt: Long
): ImportedLocalTrackEntity = ImportedLocalTrackEntity(
    uri = uri,
    localId = localId,
    displayName = displayName,
    title = title,
    artists = artists,
    album = album,
    durationMs = durationMs,
    fileSizeBytes = fileSizeBytes,
    treeUri = treeUri,
    importedAt = importedAt
)
