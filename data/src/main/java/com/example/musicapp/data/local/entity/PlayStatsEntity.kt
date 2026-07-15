package com.example.musicapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// 播放统计单行表：本周播放次数 + 累计实际听歌时长
@Entity(tableName = "play_stats")
data class PlayStatsEntity(
    // 固定为 1，整表只保留一行汇总
    @PrimaryKey val id: Int = SINGLETON_ID,
    // 当前自然周内的播放次数（不去重；跨周后重置）
    val weekPlayCount: Int = 0,
    // 当前计数所属周的周一 epochDay，用于跨周判定
    val weekStartEpochDay: Long = 0L,
    // 累计实际听歌时长（毫秒）
    val totalListenDurationMs: Long = 0L
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
