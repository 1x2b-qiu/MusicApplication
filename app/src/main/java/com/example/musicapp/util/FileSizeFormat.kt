package com.example.musicapp.util

/** 将字节数格式化为 MB 文案（≥10 MB 取整，否则保留一位小数）。 */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 10.0) {
        "${mb.toInt()} MB"
    } else {
        String.format("%.1f MB", mb)
    }
}
