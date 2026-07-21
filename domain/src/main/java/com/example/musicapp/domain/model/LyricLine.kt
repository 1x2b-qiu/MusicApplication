package com.example.musicapp.domain.model

import androidx.compose.runtime.Immutable

// 单行 LRC 歌词
@Immutable
data class LyricLine(
    // 该句歌词开始播放的时间点（毫秒）
    val timeMs: Long,
    // 歌词文本内容
    val text: String
)

// LRC 歌词匹配工具：根据播放进度定位当前应显示的歌词行
object LyricMatcher {

    /**
     * 查找当前播放位置对应的歌词下标。
     * lines 须按 timeMs 升序；取「timeMs <= positionMs」的最后一句。
     * 无歌词，或进度尚未到达第一句时，返回 0。
     * 二分查找：播放中每 200ms 调用一次，避免长歌词线性扫描。
     */
    fun currentIndex(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return 0
        var low = 0
        var high = lines.lastIndex
        var result = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    /**
     * 查找当前播放位置对应的歌词文本；尚无到达的句子时返回 null。
     */
    fun currentLine(lines: List<LyricLine>, positionMs: Long): String? {
        if (lines.isEmpty()) return null
        val line = lines[currentIndex(lines, positionMs)]
        return if (line.timeMs <= positionMs) line.text else null
    }
}
