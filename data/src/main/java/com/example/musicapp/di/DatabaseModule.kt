package com.example.musicapp.di

import android.content.Context
import androidx.room.Room
import com.example.musicapp.data.local.MusicDatabase
import com.example.musicapp.data.local.dao.RecentPlayDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Room 数据库与 DAO 的 Hilt 提供模块
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    // 构建 Room 数据库单例
    fun provideMusicDatabase(
        @ApplicationContext context: Context
    ): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "music_database"
        ).build()
    }

    @Provides
    // 提供最近播放 DAO
    fun provideRecentPlayDao(database: MusicDatabase): RecentPlayDao {
        return database.recentPlayDao()
    }
}
