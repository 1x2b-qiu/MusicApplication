package com.example.musicapp.di

import com.example.musicapp.data.repository.impl.AuthRepositoryImpl
import com.example.musicapp.data.repository.impl.MusicRepositoryImpl
import com.example.musicapp.domain.repository.AuthRepository
import com.example.musicapp.domain.repository.MusicRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(
        musicRepositoryImpl: MusicRepositoryImpl
    ): MusicRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}
