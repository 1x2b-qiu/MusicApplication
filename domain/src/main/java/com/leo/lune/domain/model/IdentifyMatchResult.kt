package com.leo.lune.domain.model

import androidx.compose.runtime.Immutable

// 听歌识曲：识别结果 + 网易云曲库匹配
@Immutable
sealed interface IdentifyMatchResult {
    // 识别成功，并附带按「歌名 歌手」搜索到的候选曲目
    data class Matched(
        val track: RecognizedTrack,
        val songs: List<Song>,
    ) : IdentifyMatchResult

    // 音频已处理，但 AudD 未匹配到曲目
    data object NotRecognized : IdentifyMatchResult
}
