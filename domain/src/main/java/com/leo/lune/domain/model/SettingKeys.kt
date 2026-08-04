package com.leo.lune.domain.model

// App 设置表的 key 常量（value 为字符串，由业务自行解析）
object SettingKeys {
    // 下载默认音质：存 DownloadQuality.bitrate 的十进制字符串
    const val DOWNLOAD_DEFAULT_QUALITY = "download_default_quality"

    // SAF 下载目录 tree URI（content://.../tree/...）
    const val DOWNLOAD_STORAGE_TREE_URI = "download_storage_tree_uri"

    // SAF 目录展示名（选目录时一并写入，便于设置页展示）
    const val DOWNLOAD_STORAGE_DISPLAY_NAME = "download_storage_display_name"

    // 流媒体默认音质：存 PlaybackQuality.bitrate 的十进制字符串
    const val PLAYBACK_DEFAULT_QUALITY = "playback_default_quality"

    // 与其他应用同时播放："true" / "false"；开启后不独占音频焦点
    const val PLAYBACK_MIX_WITH_OTHERS = "playback_mix_with_others"
}
