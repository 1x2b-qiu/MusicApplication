package com.leo.lune.domain.repository

// 应用缓存仓储：统计 / 清理 cacheDir 等临时文件，不触及已下载歌曲与用户数据
interface CacheRepository {
    // 当前可清理缓存总字节数
    suspend fun getCacheSizeBytes(): Long

    // 清理缓存目录内容；已下载歌曲、登录态、设置不受影响
    suspend fun clearCache()
}
