package com.example.musicapp.domain.model

// 单行 LRC 歌词
data class LyricLine(
    // 该句歌词开始播放的时间点（毫秒）
    val timeMs: Long,
    // 歌词文本内容
    val text: String
)

// LRC 歌词匹配工具：根据播放进度定位当前应显示的歌词行
object LyricMatcher {

    /**
     * 查找当前播放位置对应的歌词文本。
     *
     * lines 必须按 timeMs 升序排列。算法从前往后扫描，持续记录
     * 「开始时间 <= 当前进度」的最后一句；一旦遇到未来的歌词行就停止。
     *
     * 例：positionMs = 20000，歌词分别在 12000 / 18500 / 25000 开始，
     * 则返回 18500 那句，因为 25000 还没到。
     */
    fun currentLine(lines: List<LyricLine>, positionMs: Long): String? {
        if (lines.isEmpty()) return null

        // 记录目前为止最后一个已经"到达"的歌词行
        var result: LyricLine? = null
        for (line in lines) {
            if (line.timeMs <= positionMs) {
                result = line
            } else {
                // 后续行的开始时间都大于当前进度，无需继续扫描
                break
            }
        }
        return result?.text
    }
}