package com.example.musicapp.di

import com.example.musicapp.data.remote.interceptor.CookieInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// Hilt OkHttp 模块：配置超时、Cookie 拦截器与日志
@Module
@InstallIn(SingletonComponent::class)
object OkHttpModule {

    @Provides
    @Singleton
    // 配置超时、Cookie 拦截器与请求日志
    fun provideOkHttpClient(
        cookieInterceptor: CookieInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(cookieInterceptor)
            .addInterceptor(logging)
            .build()
    }
}
