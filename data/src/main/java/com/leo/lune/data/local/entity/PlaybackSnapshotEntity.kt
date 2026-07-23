package com.leo.lune.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// 播放快照单行表：进程被杀后恢复播放状态
// 当前歌曲平铺存储（与 RecentPlayEntity 风格一致），队列序列化为 JSON
@Entity(tableName = "playback_snapshot")
data class PlaybackSnapshotEntity(
    // 固定为 1，整表只保留一行
    @PrimaryKey val id: Int = SINGLETON_ID,
    // 当前歌曲
    val currentSongId: Long,
    val currentSongName: String,
    val currentSongArtists: String,
    val currentSongAlbum: String,
    val currentSongCoverUrl: String?,
    val currentSongDurationMs: Long,
    // 队列（Gson 序列化的 Song 数组 JSON）
    val queueJson: String,
    // 当前歌曲在队列中的下标
    val queueIndex: Int
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
