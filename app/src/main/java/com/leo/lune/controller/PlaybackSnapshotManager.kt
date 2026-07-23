package com.leo.lune.controller

import com.leo.lune.domain.model.PlaybackSnapshot
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.usecase.playback.ClearPlaybackSnapshotUseCase
import com.leo.lune.domain.usecase.playback.GetPlaybackSnapshotUseCase
import com.leo.lune.domain.usecase.playback.SavePlaybackSnapshotUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// 播放快照持久化管理器（Hilt 单例）
// 职责：将队列/当前曲/下标写入 Room，供进程被杀后恢复；失败静默忽略
@Singleton
class PlaybackSnapshotManager @Inject constructor(
    private val savePlaybackSnapshotUseCase: SavePlaybackSnapshotUseCase,
    private val getPlaybackSnapshotUseCase: GetPlaybackSnapshotUseCase,
    private val clearPlaybackSnapshotUseCase: ClearPlaybackSnapshotUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // 异步保存快照；仅在有当前曲且队列非空时写入，否则静默跳过
    fun save(currentSong: Song?, queue: List<Song>, queueIndex: Int) {
        if (currentSong == null || queue.isEmpty()) return
        scope.launch {
            runCatching {
                savePlaybackSnapshotUseCase(
                    PlaybackSnapshot(
                        currentSong = currentSong,
                        queue = queue,
                        queueIndex = queueIndex
                    )
                )
            }
        }
    }

    // 挂起读取上次快照；失败或无数据返回 null
    suspend fun restore(): PlaybackSnapshot? {
        return runCatching { getPlaybackSnapshotUseCase() }.getOrNull()
    }

    // 异步清除快照（清空队列时调用）
    fun clear() {
        scope.launch {
            runCatching { clearPlaybackSnapshotUseCase() }
        }
    }
}
