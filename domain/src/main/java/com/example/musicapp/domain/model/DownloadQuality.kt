package com.example.musicapp.domain.model

// 本地下载音质档位（对应网易云 song/url 的 br 参数）
enum class DownloadQuality(
    // 请求码率（bps）；999000 表示尽量最高
    val bitrate: Int,
    val label: String,
    // 弹窗右侧说明前缀，如「128 kbps」或「FLAC」
    val detailPrefix: String
) {
    Standard(128_000, "标准", "128 kbps"),
    High(320_000, "高品质", "320 kbps"),
    Lossless(999_000, "无损", "FLAC");

    companion object {
        val Default: DownloadQuality = High
    }
}
