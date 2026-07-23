package com.leo.lune.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.leo.lune.data.local.dao.DownloadedSongDao
import com.leo.lune.data.local.dao.PendingDownloadDao
import com.leo.lune.data.local.dao.PlayStatsDao
import com.leo.lune.data.local.dao.PlaybackSnapshotDao
import com.leo.lune.data.local.dao.RecentPlayDao
import com.leo.lune.data.local.entity.DownloadedSongEntity
import com.leo.lune.data.local.entity.PendingDownloadEntity
import com.leo.lune.data.local.entity.PlayStatsEntity
import com.leo.lune.data.local.entity.PlaybackSnapshotEntity
import com.leo.lune.data.local.entity.RecentPlayEntity

// App 本地 Room 数据库
@Database(
    entities = [
        RecentPlayEntity::class,
        PlayStatsEntity::class,
        DownloadedSongEntity::class,
        PendingDownloadEntity::class,
        PlaybackSnapshotEntity::class
    ],
    version = 7,
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

    // 播放快照 DAO
    abstract fun playbackSnapshotDao(): PlaybackSnapshotDao
}
