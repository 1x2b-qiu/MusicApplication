package com.leo.lune.domain.model

// 流媒体默认音质档位（对应网易云 song/url 的 br 参数；不影响已下载文件）
enum class PlaybackQuality(
    // 请求码率（bps）；999000 表示尽量最高
    val bitrate: Int,
    val label: String,
    // 设置页副标题，如「128 kbps · 省流量」
    val detail: String,
    // 可选角标：推荐 / HiFi
    val badge: String? = null
) {
    Standard(128_000, "标准", "128 kbps · 省流量"),
    High(320_000, "高品质", "320 kbps · 推荐", badge = "推荐"),
    Lossless(999_000, "无损", "FLAC · 最高音质", badge = "HiFi");

    companion object {
        val Default: PlaybackQuality = Lossless

        // 按码率还原档位；未知码率回退默认
        fun fromBitrate(bitrate: Int): PlaybackQuality {
            return entries.find { it.bitrate == bitrate } ?: Default
        }
    }
}
