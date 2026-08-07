package com.leo.lune.domain.repository

// 封面加载仓储接口
// 把 Android 特有的图片加载实现（Coil、Bitmap 压缩）封装在 data/app 层，
// domain 层只依赖此纯接口，UseCase 可以脱离 Android 框架进行单测。
interface ArtworkRepository {

    // 加载封面并压缩为通知栏/锁屏适用的 JPEG 字节；失败或超时返回 null
    suspend fun loadArtworkBytes(coverUrl: String?): ByteArray?
}
