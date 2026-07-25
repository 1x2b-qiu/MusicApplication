package com.leo.lune.domain.model

import androidx.compose.runtime.Immutable

// AudD 识别出的曲目元数据（尚未映射到网易云 Song）
@Immutable
data class RecognizedTrack(
    val title: String,
    val artist: String,
    val album: String? = null,
    val songLink: String? = null,
)
