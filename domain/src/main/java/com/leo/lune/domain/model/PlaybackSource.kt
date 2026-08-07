package com.leo.lune.domain.model

// 播放源：描述一首歌曲的可播放来源及音质标签
// Controller 持有 PlaybackSource 后，再进行 Android 特有的 URI 转换（Uri.fromFile 等），
// 保持 domain 层零 Android 依赖。
sealed class PlaybackSource {

    abstract val qualityLabel: String
    // URI 原始字符串：本地路径或远端 URL，供 Controller 转换或展示用
    abstract val rawUri: String

    // 本地已下载文件：path 为私有目录绝对路径，或 SAF content URI 字符串
    data class LocalFile(
        val path: String,
        override val qualityLabel: String
    ) : PlaybackSource() {
        override val rawUri: String get() = path
    }

    // 远端流媒体地址
    data class RemoteStream(
        val url: String,
        override val qualityLabel: String
    ) : PlaybackSource() {
        override val rawUri: String get() = url
    }
}
