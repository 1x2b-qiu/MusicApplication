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
    // true 表示用户暂停；任务仍留在列表，可继续
    val paused: Boolean = false,
    // 非空表示失败；成功或取消会从列表移除，不会长期停留在此
    val error: String? = null
)

// 全局下载任务管理（Hilt 单例）
// 统一入队 / 进度 / 暂停 / 取消，避免播放页与下载页各维护一份状态
@Singleton
class SongDownloadManager @Inject constructor(
    private val downloadSongUseCase: DownloadSongUseCase
) {
    // 对外只读；UI 通过 collect 订阅进行中任务
    private val _tasks = MutableStateFlow<List<ActiveDownloadTask>>(emptyList())
    val tasks: StateFlow<List<ActiveDownloadTask>> = _tasks.asStateFlow()

    // SupervisorJob：单个任务失败不影响同 scope 里其他下载
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    // songId → 协程 Job，用于判断是否在下、以及 cancel / pause
    private val jobs = mutableMapOf<Long, Job>()
    // 暂停后继续需要原 Song / 音质，故单独缓存请求参数
    private val requests = mutableMapOf<Long, DownloadRequest>()

    // 入队下载；同曲已在列表（含暂停）则忽略或恢复，避免重复请求
    fun enqueue(song: Song, quality: DownloadQuality = DownloadQuality.Default) {
        val existing = _tasks.value.find { it.songId == song.id }
        if (existing != null && existing.error == null) {
            if (existing.paused) resume(song.id)
            return
        }
        if (jobs[song.id]?.isActive == true) return

        requests[song.id] = DownloadRequest(song, quality)
        upsertTask(
            ActiveDownloadTask(
                songId = song.id,
                title = song.name,
                artist = song.artists,
                coverUrl = song.coverUrl,
                progress = 0f,
                paused = false,
                error = null
            )
        )
        startJob(song, quality)
    }

    // 暂停：停协程但保留任务与进度，封面区可展示暂停态
    fun pause(songId: Long) {
        val task = _tasks.value.find { it.songId == songId } ?: return
        if (task.paused || task.error != null) return
        _tasks.update { list ->
            list.map { if (it.songId == songId) it.copy(paused = true) else it }
        }
        jobs.remove(songId)?.cancel()
    }

    // 继续：用缓存的请求参数重新拉流（当前下载器不支持断点，进度会重新累计）
    fun resume(songId: Long) {
        val request = requests[songId] ?: return
        val task = _tasks.value.find { it.songId == songId } ?: return
        if (!task.paused) return
        if (jobs[songId]?.isActive == true) return

        upsertTask(task.copy(paused = false, progress = 0f, error = null))
        startJob(request.song, request.quality)
    }

    fun togglePause(songId: Long) {
        val task = _tasks.value.find { it.songId == songId } ?: return
        if (task.paused) resume(songId) else pause(songId)
    }

    // 取消指定歌曲下载：先停协程，再从 UI 列表移除
    fun cancel(songId: Long) {
        jobs.remove(songId)?.cancel()
        requests.remove(songId)
        removeTask(songId)
    }

    private fun startJob(song: Song, quality: DownloadQuality) {
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
                requests.remove(song.id)
                removeTask(song.id)
            }.onFailure { error ->
                if (error is CancellationException) {
                    // pause 会先标 paused 再 cancel，需保留任务；cancel() 已先 removeTask
                    val stillPaused = _tasks.value.any { it.songId == song.id && it.paused }
                    if (!stillPaused) {
                        requests.remove(song.id)
                        removeTask(song.id)
                    }
                } else {
                    // 失败保留一条带 error 的任务，供播放页展示文案
                    updateError(song.id, error.message ?: "下载失败")
                }
            }
            jobs.remove(song.id)
        }
        jobs[song.id] = job
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
                if (task.songId == songId) {
                    task.copy(progress = progress, paused = false, error = null)
                } else {
                    task
                }
            }
        }
    }

    private fun updateError(songId: Long, message: String) {
        _tasks.update { list ->
            list.map { task ->
                if (task.songId == songId) task.copy(error = message, paused = false) else task
            }
        }
    }

    private fun removeTask(songId: Long) {
        _tasks.update { list -> list.filterNot { it.songId == songId } }
    }

    private data class DownloadRequest(
        val song: Song,
        val quality: DownloadQuality
    )
}
