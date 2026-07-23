package com.leo.lune.controller

import com.leo.lune.domain.model.Song
import com.leo.lune.domain.usecase.stats.ObservePlayStatsUseCase
import com.leo.lune.domain.usecase.stats.UpdatePlayModeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

// 队列导航器（Hilt 单例）
// 职责：播放模式状态 + 持久化、解析上/下一首下标（纯逻辑，不操作播放器）
@Singleton
class QueueManager @Inject constructor(
    private val observePlayStatsUseCase: ObservePlayStatsUseCase,
    private val updatePlayModeUseCase: UpdatePlayModeUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // 当前播放模式；UI 通过 PlaybackState.playMode 间接订阅
    private val _playMode = MutableStateFlow(PlayerPlayMode.Loop)
    val playMode: StateFlow<PlayerPlayMode> = _playMode.asStateFlow()

    init {
        // 启动时恢复本地持久化的播放模式
        scope.launch {
            val saved = observePlayStatsUseCase().first().playMode
            _playMode.value = runCatching { PlayerPlayMode.valueOf(saved) }
                .getOrDefault(PlayerPlayMode.Loop)
        }
    }

    // 循环切换播放模式：随机 → 列表循环 → 单曲循环 → 随机；并写入本地
    fun cyclePlayMode() {
        val next = when (_playMode.value) {
            PlayerPlayMode.Shuffle -> PlayerPlayMode.Loop
            PlayerPlayMode.Loop -> PlayerPlayMode.Single
            PlayerPlayMode.Single -> PlayerPlayMode.Shuffle
        }
        _playMode.value = next
        scope.launch {
            updatePlayModeUseCase(next.name)
        }
    }

    // 解析「下一首」下标；单曲循环在手动/自动下一首时仍按列表前进
    fun resolveNextIndex(queue: List<Song>, currentIndex: Int): Int {
        return when (_playMode.value) {
            PlayerPlayMode.Shuffle -> resolveRandomIndex(queue, currentIndex)
            PlayerPlayMode.Loop, PlayerPlayMode.Single ->
                (currentIndex + 1) % queue.size
        }
    }

    // 解析「上一首」下标
    fun resolvePreviousIndex(queue: List<Song>, currentIndex: Int): Int {
        return when (_playMode.value) {
            PlayerPlayMode.Shuffle -> resolveRandomIndex(queue, currentIndex)
            PlayerPlayMode.Loop -> if (currentIndex <= 0) queue.lastIndex else currentIndex - 1
            PlayerPlayMode.Single -> if (currentIndex <= 0) 0 else currentIndex - 1
        }
    }

    // 随机选一首，尽量避开当前下标（队列长度 > 1）
    fun resolveRandomIndex(queue: List<Song>, currentIndex: Int): Int {
        if (queue.size <= 1) return 0
        var next = Random.nextInt(queue.size)
        while (next == currentIndex) {
            next = Random.nextInt(queue.size)
        }
        return next
    }
}
