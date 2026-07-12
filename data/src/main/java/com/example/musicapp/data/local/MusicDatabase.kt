package com.example.musicapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.musicapp.data.local.dao.RecentPlayDao
import com.example.musicapp.data.local.entity.RecentPlayEntity

// App 本地 Room 数据库，当前仅包含最近播放表
@Database(
    entities = [RecentPlayEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {

    // 最近播放记录 DAO
    abstract fun recentPlayDao(): RecentPlayDao
}
