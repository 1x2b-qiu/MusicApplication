package com.leo.lune

import android.app.Application
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.leo.lune.data.BuildConfig
import com.leo.lune.domain.repository.AuthRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class MusicApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 配置 Coil 图片加载缓存，防止封面大图导致 OOM
        configureImageLoader()

        // 进程启动即恢复会话 Cookie，使其与 Activity 生命周期解耦
        // 无论进程为 Activity / Service / 广播拉起，都先于业务组件执行
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SessionRestoreEntryPoint::class.java
        )
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            entryPoint.authRepository().restoreSession()
        }
    }

    private fun configureImageLoader() {
        val imageLoader = ImageLoader.Builder(this)
            // 内存缓存：使用应用可用内存的 25%，音乐App封面数量多但单张不大
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            // 磁盘缓存：50MB，封面图压缩后通常几十KB，可缓存上千张
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            // 默认缓存策略：优先读缓存，减少网络请求
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // Debug 构建时启用日志，便于排查图片加载问题
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()

        // 设置为全局单例，所有 AsyncImage / rememberAsyncImagePainter 自动使用
        coil.Coil.setImageLoader(imageLoader)
    }
}

// 供 Application 在 onCreate 中访问 Hilt 单例的入口
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SessionRestoreEntryPoint {
    fun authRepository(): AuthRepository
}
