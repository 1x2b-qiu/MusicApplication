package com.leo.lune.data.download

import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// 将远程音频流写入本地文件；已有临时文件时通过 Range 断点续传
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
    // 若 destination 已有内容，请求 Range 并从末尾追加；服务端不支持时回退为整文件重下
    // onProgress：累计已写字节 / 完整文件总长（未知时为 -1）
    // isCancelled：为 true 时中断 OkHttp 并抛 CancellationException
    fun download(
        url: String,
        destination: File,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null,
        isCancelled: () -> Boolean = { false }
    ): Long {
        destination.parentFile?.mkdirs()
        // 临时文件已有长度，作为断点偏移；0 表示全新下载
        val existingBytes = if (destination.isFile) destination.length() else 0L

        val requestBuilder = Request.Builder()
            .url(url)
            .get()
        // 有半成品时告诉服务端从 existingBytes 起只返回剩余部分
        if (existingBytes > 0L) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }
        val request = requestBuilder.build()

        val call = client.newCall(request)
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("下载失败：HTTP ${response.code}")
                }
                val body = response.body ?: throw IllegalStateException("下载失败：空响应")

                // 206：断点续传追加；200：整文件（或服务端忽略 Range）则覆盖写入
                val resumeAppend = response.code == HTTP_PARTIAL_CONTENT && existingBytes > 0L
                if (existingBytes > 0L && !resumeAppend) {
                    // 服务端未按 Range 返回，半成品作废，改为从头写
                    destination.delete()
                }

                val totalBytes = resolveTotalBytes(response, existingBytes, resumeAppend)
                // 续传从已有字节起算进度；整文件重下从 0 起算
                var bytesWritten = if (resumeAppend) existingBytes else 0L
                var lastReported = -1L
                onProgress?.invoke(bytesWritten, totalBytes)

                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                body.byteStream().use { input ->
                    // resumeAppend=true 时追加到文件末尾，否则覆盖创建
                    FileOutputStream(destination, resumeAppend).use { output ->
                        while (true) {
                            // 上层 pause/cancel 会置位，中断读循环并取消 OkHttp Call
                            if (isCancelled()) {
                                call.cancel()
                                throw CancellationException("下载已取消")
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            bytesWritten += read
                            // 约每 64KB 或结束时回调，避免 UI 被过密进度打爆
                            if (onProgress != null &&
                                (bytesWritten - lastReported >= PROGRESS_REPORT_INTERVAL_BYTES ||
                                    (totalBytes > 0L && bytesWritten >= totalBytes))
                            ) {
                                lastReported = bytesWritten
                                onProgress(bytesWritten, totalBytes)
                            }
                        }
                    }
                }
                // 循环可能在不足 64KB 处结束，补发最后一次进度
                if (onProgress != null && lastReported != bytesWritten) {
                    onProgress(bytesWritten, if (totalBytes > 0L) totalBytes else bytesWritten)
                }
                return destination.length()
            }
        } catch (error: CancellationException) {
            // 确保取消时也关掉 Call，避免连接空挂
            call.cancel()
            throw error
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

    // 解析完整文件总长（供进度条用），未知时返回 -1
    // 优先用 Content-Range 的 total；206 续传时 Content-Length 只是剩余长度，需加上已下字节
    private fun resolveTotalBytes(
        response: Response,
        existingBytes: Long,
        resumeAppend: Boolean
    ): Long {
        // 例：Content-Range: bytes 1024-9999/10000 → 完整总长 10000
        parseContentRangeTotal(response.header("Content-Range"))?.let { return it }
        val contentLength = response.body?.contentLength() ?: -1L
        if (contentLength <= 0L) return -1L
        // 续传追加：本段 Length 是剩余字节；整文件下载：Length 即总长
        return if (resumeAppend) existingBytes + contentLength else contentLength
    }

    // 从 Content-Range 取出完整文件总长；格式 bytes start-end/total，total 为 * 表示未知
    private fun parseContentRangeTotal(header: String?): Long? {
        if (header.isNullOrBlank()) return null
        // 取最后一个 '/' 之后的 total 部分
        val totalPart = header.substringAfterLast('/', missingDelimiterValue = "").trim()
        if (totalPart.isEmpty() || totalPart == "*") return null
        return totalPart.toLongOrNull()?.takeIf { it > 0L }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8 * 1024
        private const val PROGRESS_REPORT_INTERVAL_BYTES = 64L * 1024L
        private const val HTTP_PARTIAL_CONTENT = 206
        // URL 路径里可直接认作音频后缀的集合
        private val KNOWN_AUDIO_EXTS = setOf("mp3", "flac", "m4a", "aac", "ogg", "wav")
    }
}
