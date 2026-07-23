package com.leo.lune.data.mapper

import com.leo.lune.data.local.entity.RecentPlayEntity
import com.leo.lune.domain.model.Song

// Room 实体 → 领域模型
fun RecentPlayEntity.toSong(): Song = Song(
    id = songId,
    name = name,
    artists = artists,
    album = album,
    coverUrl = coverUrl,
    durationMs = durationMs
)

// 领域模型 → Room 实体
fun Song.toRecentPlayEntity(playedAt: Long): RecentPlayEntity = RecentPlayEntity(
    songId = id,
    name = name,
    artists = artists,
    album = album,
    coverUrl = coverUrl,
    durationMs = durationMs,
    playedAt = playedAt
)
