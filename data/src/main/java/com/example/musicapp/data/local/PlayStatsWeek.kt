package com.example.musicapp.data.local

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

// 播放统计用的自然周工具：以周一为一周起始
internal object PlayStatsWeek {
    // 当前时区下、本周一的 epochDay
    fun currentWeekStartEpochDay(): Long {
        return LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .toEpochDay()
    }
}
