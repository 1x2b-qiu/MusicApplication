package com.example.musicapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.musicapp.data.local.dao.DownloadedSongDao
import com.example.musicapp.data.local.dao.PendingDownloadDao
import com.example.musicapp.data.local.dao.PlayStatsDao
import com.example.musicapp.data.local.dao.RecentPlayDao
import com.example.musicapp.data.local.entity.DownloadedSongEntity
import com.example.musicapp.data.local.entity.PendingDownloadEntity
import com.example.musicapp.data.local.entity.PlayStatsEntity
import com.example.musicapp.data.local.entity.RecentPlayEntity

// App 本地 Room 数据库
@Database(
    entities = [
        RecentPlayEntity::class,
        PlayStatsEntity::class,
        DownloadedSongEntity::class,
        PendingDownloadEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {

    // 最近播放记录 DAO
    abstract fun recentPlayDao(): RecentPlayDao

    // 播放统计 DAO
    abstract fun playStatsDao(): PlayStatsDao

    // 本地下载记录 DAO
    abstract fun downloadedSongDao(): DownloadedSongDao

    // 未完成下载任务 DAO
    abstract fun pendingDownloadDao(): PendingDownloadDao
}
