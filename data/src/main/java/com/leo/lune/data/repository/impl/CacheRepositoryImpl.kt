package com.leo.lune.data.repository.impl

import android.content.Context
import com.leo.lune.domain.repository.CacheRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// 扫描并清理系统缓存目录（含 Coil 磁盘缓存、识曲临时文件等）
@Singleton
class CacheRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CacheRepository {

    override suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        cacheRoots().sumOf { it.directorySizeBytes() }
    }

    override suspend fun clearCache() = withContext(Dispatchers.IO) {
        cacheRoots().forEach { root ->
            root.listFiles()?.forEach { child ->
                runCatching { child.deleteRecursively() }
            }
        }
    }

    // 内部缓存 + 外部缓存；不含 filesDir / 下载目录
    private fun cacheRoots(): List<File> = buildList {
        add(context.cacheDir)
        context.externalCacheDir?.let { add(it) }
    }

    private fun File.directorySizeBytes(): Long {
        if (!exists()) return 0L
        return walkTopDown()
            .filter { it.isFile }
            .sumOf { runCatching { it.length() }.getOrDefault(0L) }
    }
}
