package com.leo.lune.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

// 封面加载器（Hilt 单例）
// 职责：下载歌曲封面并压缩为适合系统通知栏/锁屏的 JPEG 字节；
// 内嵌 artworkData 代替远程 artworkUri，避免系统 UI 自行拉取失败导致封面不显示
@Singleton
class ArtworkLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imageLoader by lazy {
        ImageLoader.Builder(context)
            .crossfade(false)
            .build()
    }

    // 简单 LRU 内存缓存：coverUrl → 压缩后的 JPEG 字节
    private val cache = object : android.util.LruCache<String, ByteArray>(8) {}

    private companion object {
        // 输出尺寸上限：512px 足够通知栏/锁屏使用，体积可控
        const val MAX_DIMENSION = 512
        const val JPEG_QUALITY = 85
        // 下载超时：避免封面加载阻塞播放启动
        const val LOAD_TIMEOUT_MS = 4_000L
    }

    // 加载封面并压缩为 JPEG 字节；失败或超时返回 null（调用方回退到 artworkUri）
    suspend fun loadArtworkBytes(coverUrl: String?): ByteArray? {
        if (coverUrl.isNullOrBlank()) return null
        cache.get(coverUrl)?.let { return it }

        return withTimeoutOrNull(LOAD_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val request = ImageRequest.Builder(context)
                        .data(coverUrl)
                        .size(Size(MAX_DIMENSION, MAX_DIMENSION))
                        .allowHardware(false) // 需要 CPU 位图才能压缩为字节
                        .build()
                    val result = imageLoader.execute(request)
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                        ?: return@withContext null
                    val scaled = scaleBitmap(bitmap)
                    val bytes = compressToJpeg(scaled)
                    if (scaled !== bitmap) scaled.recycle()
                    cache.put(coverUrl, bytes)
                    bytes
                }.getOrNull()
            }
        }
    }

    // 等比缩放至 MAX_DIMENSION 以内；已足够小则原样返回
    private fun scaleBitmap(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) return source
        val ratio = minOf(MAX_DIMENSION.toFloat() / width, MAX_DIMENSION.toFloat() / height)
        val targetW = (width * ratio).toInt().coerceAtLeast(1)
        val targetH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    // 压缩为 JPEG 字节流
    private fun compressToJpeg(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        return stream.toByteArray()
    }
}
