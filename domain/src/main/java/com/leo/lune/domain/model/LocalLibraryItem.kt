package com.leo.lune.domain.model

import androidx.compose.runtime.Immutable

// 本地曲库一行：本地下载或自定义文件夹导入
@Immutable
data class LocalLibraryItem(
    val song: Song,
    val addedAt: Long,
    // 下载档位码率；导入曲为 null
    val bitrate: Int? = null
) {
    val rowKey: String
        get() = if (bitrate != null) "${song.id}_$bitrate" else "imported_${song.id}"

    val qualityLabel: String
        get() = bitrate?.let { DownloadQuality.fromBitrate(it).label } ?: "本地"
}
