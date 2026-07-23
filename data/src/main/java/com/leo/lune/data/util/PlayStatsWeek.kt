package com.leo.lune.data.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

// 播放统计用的自然周工具：以周一为一周起始
internal object PlayStatsWeek {
    // 当前时区下、本周一的 epochDay
    @RequiresApi(Build.VERSION_CODES.O)
    fun currentWeekStartEpochDay(): Long {
        return LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .toEpochDay()
    }
}
