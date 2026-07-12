package com.example.musicapp.di

import com.example.musicapp.data.BuildConfig
import com.example.musicapp.data.remote.api.NeteaseApi
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

// Hilt 网络模块：提供 Gson、Retrofit 与网易云 API 接口
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    // Gson 实例，供 Retrofit 序列化/反序列化 JSON
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    // 构建 Retrofit 实例，baseUrl 来自 BuildConfig
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.NETEASE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    // 网易云 OpenAPI 接口代理
    fun provideNeteaseApi(retrofit: Retrofit): NeteaseApi =
        retrofit.create(NeteaseApi::class.java)
}
