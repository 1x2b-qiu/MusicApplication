package com.leo.lune.domain.model

import androidx.compose.runtime.Immutable

// 歌曲领域模型，表示一首可播放的音乐
// @Immutable：向 Compose 编译器承诺实例不可变，使以其为参数的 composable 可跳过
@Immutable
data class Song(
    // 歌曲唯一 ID
    val id: Long,
    // 歌曲名
    val name: String,
    // 歌手（多个歌手以分隔符拼接）
    val artists: String,
    // 专辑名
    val album: String,
    // 封面图 URL，可能为空
    val coverUrl: String?,
    // 时长（毫秒）
    val durationMs: Long,
    // 队列项绑定的本地下载音质码率；非本地多档队列为 null（播时取最高本地 / 流媒体默认）
    val preferredDownloadBitrate: Int? = null,
    // 导入本地曲的可播放 URI（content://）；网易云曲为 null
    val localPlaybackUri: String? = null
) {
    // 自定义文件夹导入：负 id，或不经过网易云下载表
    val isImportedLocal: Boolean
        get() = id < 0L || !localPlaybackUri.isNullOrBlank()
}
