package com.leo.lune.manager

import android.os.SystemClock
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.usecase.history.RecordRecentPlayUseCase
import com.leo.lune.domain.usecase.stats.AddListenDurationUseCase
import com.leo.lune.domain.usecase.stats.RecordWeekPlayUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// 听歌统计记录器（Hilt 单例）
// 职责：最近播放记录、周播放次数、听歌时长结算；全部失败静默忽略
@Singleton
class PlayStatsRecorderManager @Inject constructor(
    private val recordRecentPlayUseCase: RecordRecentPlayUseCase,
    private val recordWeekPlayUseCase: RecordWeekPlayUseCase,
    private val addListenDurationUseCase: AddListenDurationUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // 本次连续播放段起点（elapsedRealtime）；null 表示当前未在计时
    private var listeningSinceElapsedRealtime: Long? = null

    // 记录最近播放与周播放次数（失败静默忽略）
    fun recordPlayStats(song: Song) {
        scope.launch {
            runCatching { recordRecentPlayUseCase(song) }
            runCatching { recordWeekPlayUseCase() }
        }
    }

    // 开始一段听歌计时；已在计时则不重置，避免短暂抖动重复开段
    fun markListeningStarted() {
        if (listeningSinceElapsedRealtime == null) {
            listeningSinceElapsedRealtime = SystemClock.elapsedRealtime()
        }
    }

    // 结束当前听歌段并累加时长；切歌/暂停/清空队列时调用
    fun settleListenDuration() {
        val since = listeningSinceElapsedRealtime ?: return
        listeningSinceElapsedRealtime = null
        val elapsedMs = SystemClock.elapsedRealtime() - since
        if (elapsedMs <= 0L) return
        scope.launch {
            runCatching { addListenDurationUseCase(elapsedMs) }
        }
    }
}
