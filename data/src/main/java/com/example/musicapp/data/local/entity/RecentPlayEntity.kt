package com.example.musicapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// 最近播放记录表：缓存歌曲快照，避免首页展示时再请求详情
@Entity(tableName = "recent_plays")
data class RecentPlayEntity(
    // 歌曲 ID，作为主键；同一首歌重复播放会覆盖并刷新 playedAt
    @PrimaryKey val songId: Long,
    val name: String,
    val artists: String,
    val album: String,
    val coverUrl: String?,
    val durationMs: Long,
    // 最近一次在本 App 内成功开始播放的时间戳
    val playedAt: Long
)
