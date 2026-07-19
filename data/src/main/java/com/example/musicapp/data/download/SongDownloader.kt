package com.example.musicapp.data.download

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// 将远程音频流写入本地文件
// 基于全局 OkHttpClient 派生更长超时的下载客户端，复用 Cookie 等拦截器配置
@Singleton
class SongDownloader @Inject constructor(
    okHttpClient: OkHttpClient
) {
    // 音频文件较大，读/写超时放宽到 5 分钟；连接超时单独设为 20 秒
    private val client: OkHttpClient = okHttpClient.newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .build()

    // 同步 GET 下载到 destination，成功返回文件字节数
    // 调用方负责选用临时文件，并在失败时清理
    fun download(url: String, destination: File): Long {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("下载失败：HTTP ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("下载失败：空响应")
            destination.parentFile?.mkdirs()
            // 流式拷贝，避免整文件进内存
            body.byteStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return destination.length()
        }
    }

    // 从 URL 路径或 Content-Type 推断扩展名，供落盘文件名使用；无法识别时默认 mp3
    fun guessExtension(url: String, contentType: String?): String {
        // 去掉查询参数后再取后缀，避免 ?vuutv=... 干扰
        val pathExt = url.substringBefore('?')
            .substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
        if (pathExt in KNOWN_AUDIO_EXTS) return pathExt

        val type = contentType?.lowercase().orEmpty()
        return when {
            "flac" in type -> "flac"
            "mpeg" in type || "mp3" in type -> "mp3"
            "mp4" in type || "m4a" in type || "aac" in type -> "m4a"
            else -> "mp3"
        }
    }

    companion object {
        // URL 路径里可直接认作音频后缀的集合
        private val KNOWN_AUDIO_EXTS = setOf("mp3", "flac", "m4a", "aac", "ogg", "wav")
    }
}
