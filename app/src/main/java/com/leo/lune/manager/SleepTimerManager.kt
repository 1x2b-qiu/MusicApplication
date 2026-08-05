package com.leo.lune.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// 定时关闭管理器（Hilt 单例）
// 职责：倒计时 Job + 到期回调；不持有播放器，由调用方传入 onExpire（通常 pause）
@Singleton
class SleepTimerManager @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var timerJob: Job? = null

    // 到期时刻（epoch ms）；null 表示未启用
    private val _endsAtEpochMs = MutableStateFlow<Long?>(null)
    val endsAtEpochMs: StateFlow<Long?> = _endsAtEpochMs.asStateFlow()

    // 是否有进行中的定时关闭
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    /**
     * 启动（或覆盖）定时关闭。
     * durationMs ≤ 0 视为取消；到期后调用 onExpire，并清除进行中状态。
     */
    fun start(durationMs: Long, onExpire: () -> Unit) {
        if (durationMs <= 0L) {
            cancel()
            return
        }
        timerJob?.cancel()
        val endsAt = System.currentTimeMillis() + durationMs
        _endsAtEpochMs.value = endsAt
        _isActive.value = true
        timerJob = scope.launch {
            delay(durationMs)
            _endsAtEpochMs.value = null
            _isActive.value = false
            timerJob = null
            onExpire()
        }
    }

    // 取消当前定时；无任务时为空操作
    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _endsAtEpochMs.value = null
        _isActive.value = false
    }
}
