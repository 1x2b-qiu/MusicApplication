package com.example.musicapp.controller

import com.example.musicapp.domain.model.DownloadQuality
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.usecase.download.DownloadSongUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// 进行中的下载任务快照（本地下载页「正在下载」、播放页下载按钮共用）
data class ActiveDownloadTask(
    val songId: Long,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    // 0f..1f；Content-Length 未知时不更新，保留上次值
    val progress: Float = 0f,
    // 非空表示失败；成功或取消会从列表移除，不会长期停留在此
    val error: String? = null
)

// 全局下载任务管理（Hilt 单例）
// 统一入队 / 进度 / 取消，避免播放页与下载页各维护一份状态
@Singleton
class SongDownloadManager @Inject constructor(
    private val downloadSongUseCase: DownloadSongUseCase
) {
    // 对外只读；UI 通过 collect 订阅进行中任务
    private val _tasks = MutableStateFlow<List<ActiveDownloadTask>>(emptyList())
    val tasks: StateFlow<List<ActiveDownloadTask>> = _tasks.asStateFlow()

    // SupervisorJob：单个任务失败不影响同 scope 里其他下载
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    // songId → 协程 Job，用于判断是否在下、以及 cancel
    private val jobs = mutableMapOf<Long, Job>()

    // 入队下载；同曲已有活跃 Job 则忽略，避免重复请求
    fun enqueue(song: Song, quality: DownloadQuality = DownloadQuality.Default) {
        if (jobs[song.id]?.isActive == true) return

        // 先写入 UI，再启动协程，保证列表立刻出现进度行
        upsertTask(
            ActiveDownloadTask(
                songId = song.id,
                title = song.name,
                artist = song.artists,
                coverUrl = song.coverUrl,
                progress = 0f,
                error = null
            )
        )

        val job = scope.launch {
            runCatching {
                downloadSongUseCase(
                    song = song,
                    quality = quality,
                    onProgress = { bytesRead, totalBytes ->
                        // 总长未知时无法算百分比，跳过本次回调
                        if (totalBytes <= 0L) return@downloadSongUseCase
                        val fraction = (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f)
                        updateProgress(song.id, fraction)
                    },
                    // 与协程取消联动：Job.cancel 后底层 OkHttp 循环会中断
                    isCancelled = { !isActive }
                )
            }.onSuccess {
                // 落盘成功后由 ObserveDownloadedSongs 刷新「已下载」，此处只清进行中项
                removeTask(song.id)
            }.onFailure { error ->
                if (error is CancellationException) {
                    // 用户取消：直接移除，不当作失败展示
                    removeTask(song.id)
                } else {
                    // 失败保留一条带 error 的任务，供播放页展示文案
                    updateError(song.id, error.message ?: "下载失败")
                }
            }
            jobs.remove(song.id)
        }
        jobs[song.id] = job
    }

    // 取消指定歌曲下载：先停协程，再从 UI 列表移除
    fun cancel(songId: Long) {
        jobs.remove(songId)?.cancel()
        removeTask(songId)
    }

    // 有则替换、无则追加，保证同一 songId 在列表中最多一条
    private fun upsertTask(task: ActiveDownloadTask) {
        _tasks.update { list ->
            val without = list.filterNot { it.songId == task.songId }
            without + task
        }
    }

    // 仅更新进度；顺带清掉旧 error（若曾失败后重试）
    private fun updateProgress(songId: Long, progress: Float) {
        _tasks.update { list ->
            list.map { task ->
                if (task.songId == songId) task.copy(progress = progress, error = null) else task
            }
        }
    }

    private fun updateError(songId: Long, message: String) {
        _tasks.update { list ->
            list.map { task ->
                if (task.songId == songId) task.copy(error = message) else task
            }
        }
    }

    private fun removeTask(songId: Long) {
        _tasks.update { list -> list.filterNot { it.songId == songId } }
    }
}
