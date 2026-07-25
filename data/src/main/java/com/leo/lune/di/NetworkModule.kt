package com.leo.lune.di

import com.leo.lune.data.BuildConfig
import com.leo.lune.data.remote.api.AuddApi
import com.leo.lune.data.remote.api.NeteaseApi
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// Hilt 网络模块：Gson、网易云 Retrofit、AudD Retrofit
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    // Gson 实例，供 Retrofit 序列化/反序列化 JSON
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    // 构建网易云 Retrofit，baseUrl 来自 BuildConfig
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

    @Provides
    @Singleton
    @Audd
    // AudD 专用客户端：无 Cookie，识别上传可能稍慢故超时放宽
    fun provideAuddOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }
            .build()
    }

    @Provides
    @Singleton
    @Audd
    // AudD Retrofit（与网易云隔离，避免 Cookie 串扰）
    fun provideAuddRetrofit(
        @Audd okHttpClient: OkHttpClient,
        gson: Gson,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.AUDD_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    // AudD 听歌识曲接口代理
    fun provideAuddApi(@Audd retrofit: Retrofit): AuddApi =
        retrofit.create(AuddApi::class.java)
}
