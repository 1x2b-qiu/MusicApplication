package com.example.musicapp.di

import com.example.musicapp.data.repository.impl.AuthRepositoryImpl
import com.example.musicapp.data.repository.impl.MusicRepositoryImpl
import com.example.musicapp.data.repository.impl.PlayHistoryRepositoryImpl
import com.example.musicapp.data.repository.impl.ThemeRepositoryImpl
import com.example.musicapp.domain.repository.AuthRepository
import com.example.musicapp.domain.repository.MusicRepository
import com.example.musicapp.domain.repository.PlayHistoryRepository
import com.example.musicapp.domain.repository.ThemeRepository
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
    // 主题偏好仓储
    abstract fun bindThemeRepository(
        themeRepositoryImpl: ThemeRepositoryImpl
    ): ThemeRepository
}
