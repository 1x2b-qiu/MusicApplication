package com.example.musicapp.data.util

import com.example.musicapp.domain.model.LyricLine

// LRC 歌词解析器：将原始 LRC 文本转为按时间排序的歌词行
object LrcParser {
    // 匹配时间标签，如 [01:23.45] 或 [01:23]
    private val timeTagPattern = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?]""")
    // 匹配元数据行，如 [ti:歌名]、[ar:歌手]，解析时跳过
    private val metadataPattern = Regex(
        """\[(?:ti|ar|al|by|offset|id|hash|sign|qq|total|language):.*]""",
        RegexOption.IGNORE_CASE
    )

    // 将 LRC 原始文本解析为按时间排序的歌词行列表
    fun parse(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        lrcText.lineSequence().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || metadataPattern.matches(trimmed)) return@forEach

            val matches = timeTagPattern.findAll(trimmed).toList()
            if (matches.isEmpty()) return@forEach

            // 去掉所有时间标签后，剩余部分就是歌词文本
            val text = timeTagPattern.replace(trimmed, "").trim()
            if (text.isEmpty()) return@forEach

            // 同一行可能有多个时间标签，如 [00:12.00][00:25.00]重复歌词
            matches.forEach { match ->
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val fraction = match.groupValues[3]
                val fractionMs = parseFractionMs(fraction)
                val timeMs = minutes * 60_000 + seconds * 1_000 + fractionMs
                lines += LyricLine(timeMs = timeMs, text = text)
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    // 解析时间标签中小数部分：2 位为百分秒（×10），3 位为毫秒
    private fun parseFractionMs(fraction: String): Long {
        if (fraction.isEmpty()) return 0L
        return when (fraction.length) {
            1 -> fraction.toLong() * 100L
            2 -> fraction.toLong() * 10L
            else -> fraction.take(3).toLong()
        }
    }
}
