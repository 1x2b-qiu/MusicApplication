package com.leo.lune.di

import com.leo.lune.data.repository.impl.AuthRepositoryImpl
import com.leo.lune.data.repository.impl.DownloadRepositoryImpl
import com.leo.lune.data.repository.impl.MusicRepositoryImpl
import com.leo.lune.data.repository.impl.PlayHistoryRepositoryImpl
import com.leo.lune.data.repository.impl.PlayStatsRepositoryImpl
import com.leo.lune.data.repository.impl.PlaybackSnapshotRepositoryImpl
import com.leo.lune.data.repository.impl.SearchHistoryRepositoryImpl
import com.leo.lune.data.repository.impl.ThemeRepositoryImpl
import com.leo.lune.domain.repository.AuthRepository
import com.leo.lune.domain.repository.DownloadRepository
import com.leo.lune.domain.repository.MusicRepository
import com.leo.lune.domain.repository.PlayHistoryRepository
import com.leo.lune.domain.repository.PlayStatsRepository
import com.leo.lune.domain.repository.PlaybackSnapshotRepository
import com.leo.lune.domain.repository.SearchHistoryRepository
import com.leo.lune.domain.repository.ThemeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Hilt 仓储绑定模块：将 data 层实现绑定到 domain 接口
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    // 音乐数据仓储（搜索、播放、收藏等）
    abstract fun bindMusicRepository(
        musicRepositoryImpl: MusicRepositoryImpl
    ): MusicRepository

    @Binds
    @Singleton
    // 认证仓储
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    // 最近播放本地仓储
    abstract fun bindPlayHistoryRepository(
        playHistoryRepositoryImpl: PlayHistoryRepositoryImpl
    ): PlayHistoryRepository

    @Binds
    @Singleton
    // 播放统计本地仓储
    abstract fun bindPlayStatsRepository(
        playStatsRepositoryImpl: PlayStatsRepositoryImpl
    ): PlayStatsRepository

    @Binds
    @Singleton
    // 主题偏好仓储
    abstract fun bindThemeRepository(
        themeRepositoryImpl: ThemeRepositoryImpl
    ): ThemeRepository

    @Binds
    @Singleton
    // 搜索历史本地仓储
    abstract fun bindSearchHistoryRepository(
        searchHistoryRepositoryImpl: SearchHistoryRepositoryImpl
    ): SearchHistoryRepository

    @Binds
    @Singleton
    // 本地下载仓储
    abstract fun bindDownloadRepository(
        downloadRepositoryImpl: DownloadRepositoryImpl
    ): DownloadRepository

    @Binds
    @Singleton
    // 播放快照仓储
    abstract fun bindPlaybackSnapshotRepository(
        playbackSnapshotRepositoryImpl: PlaybackSnapshotRepositoryImpl
    ): PlaybackSnapshotRepository
}
