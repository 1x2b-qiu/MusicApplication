package com.leo.lune.manager

import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.PendingDownload
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.usecase.download.DeletePendingDownloadUseCase
import com.leo.lune.domain.usecase.download.DiscardPartialDownloadUseCase
import com.leo.lune.domain.usecase.download.DownloadSongUseCase
import com.leo.lune.domain.usecase.download.GetPartialDownloadBytesUseCase
import com.leo.lune.domain.usecase.download.GetPendingDownloadsUseCase
import com.leo.lune.domain.usecase.download.UpdatePendingDownloadPausedUseCase
import com.leo.lune.domain.usecase.download.UpdatePendingDownloadTotalBytesUseCase
import com.leo.lune.domain.usecase.download.UpsertPendingDownloadUseCase
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
    val durationMs: Long = 0L,
    // 0f..1f；Content-Length 未知时不更新，保留上次值
    val progress: Float = 0f,
    // true 表示用户暂停；任务仍留在列表，可继续
    val paused: Boolean = false,
    // 非空表示失败；成功或取消会从列表移除，不会长期停留在此
    val error: String? = null
)

// 全局下载任务管理（Hilt 单例）
// 统一入队 / 进度 / 暂停 / 取消；Room 持久化进行中任务，启动后恢复为暂停态
@Singleton
class SongDownloadManager @Inject constructor(
    private val downloadSongUseCase: DownloadSongUseCase,
    private val discardPartialDownloadUseCase: DiscardPartialDownloadUseCase,
    private val upsertPendingDownloadUseCase: UpsertPendingDownloadUseCase,
    private val updatePendingDownloadPausedUseCase: UpdatePendingDownloadPausedUseCase,
    private val updatePendingDownloadTotalBytesUseCase: UpdatePendingDownloadTotalBytesUseCase,
    private val deletePendingDownloadUseCase: DeletePendingDownloadUseCase,
    private val getPendingDownloadsUseCase: GetPendingDownloadsUseCase,
    private val getPartialDownloadBytesUseCase: GetPartialDownloadBytesUseCase
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
    // 已把真实总长写入 Room 的 songId，避免每次进度回调都打 IO
    private val persistedTotalBytesSongIds = mutableSetOf<Long>()

    init {
        // 进程重启后从 Room 恢复列表；一律暂停，等用户手动继续
        restorePendingTasks()
    }

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
                durationMs = song.durationMs,
                progress = 0f,
                paused = false,
                error = null
            )
        )
        persistPending(song, quality, paused = false)
        startJob(song, quality)
    }

    // 暂停：停协程但保留任务、进度与临时文件，供断点续传
    fun pause(songId: Long) {
        val task = _tasks.value.find { it.songId == songId } ?: return
        if (task.paused || task.error != null) return
        _tasks.update { list ->
            list.map { if (it.songId == songId) it.copy(paused = true) else it }
        }
        persistPaused(songId, paused = true)
        jobs.remove(songId)?.cancel()
    }

    // 继续：保留当前进度，底层对已有临时文件发 Range 续传
    fun resume(songId: Long) {
        val request = requests[songId] ?: return
        val task = _tasks.value.find { it.songId == songId } ?: return
        if (!task.paused) return
        if (jobs[songId]?.isActive == true) return

        upsertTask(task.copy(paused = false, error = null))
        persistPaused(songId, paused = false)
        startJob(request.song, request.quality)
    }

    fun togglePause(songId: Long) {
        val task = _tasks.value.find { it.songId == songId } ?: return
        if (task.paused) resume(songId) else pause(songId)
    }

    // 取消指定歌曲下载：先停协程，再清理临时文件、Room 记录与 UI 列表
    fun cancel(songId: Long) {
        val job = jobs.remove(songId)
        requests.remove(songId)
        persistedTotalBytesSongIds.remove(songId)
        removeTask(songId)
        if (job == null || !job.isActive) {
            // 已暂停或无活跃 Job：此处直接清临时文件与 pending
            discardPartial(songId)
        }
        // 仍在下载：等协程取消回调里再清，避免与写盘竞态
        job?.cancel()
    }

    // 冷启动：从 Room 拉未完成任务灌回内存列表；不自动开下，等用户点继续
    private fun restorePendingTasks() {
        scope.launch {
            val pendingList = runCatching { getPendingDownloadsUseCase() }.getOrElse { emptyList() }
            if (pendingList.isEmpty()) return@launch

            val restored = mutableListOf<ActiveDownloadTask>()
            for (pending in pendingList) {
                val song = pending.song
                val quality = pending.quality
                // 已下字节用临时文件；总长优先 Room 里的真实值，没有再估算
                val bytes = runCatching { getPartialDownloadBytesUseCase(song.id) }.getOrDefault(0L)
                val progress = progressFromPartialBytes(
                    bytes = bytes,
                    durationMs = song.durationMs,
                    quality = quality,
                    knownTotalBytes = pending.totalBytes
                )
                if (pending.totalBytes > 0L) {
                    persistedTotalBytesSongIds.add(song.id)
                }

                // 续传需要 Song / 音质，先缓存到 requests
                requests[song.id] = DownloadRequest(song, quality)
                restored += ActiveDownloadTask(
                    songId = song.id,
                    title = song.name,
                    artist = song.artists,
                    coverUrl = song.coverUrl,
                    durationMs = song.durationMs,
                    progress = progress,
                    // 进程被杀后一律暂停，避免静默耗流量
                    paused = true,
                    error = null
                )
                // 与 UI 一致，把 paused=true 写回 Room
                runCatching { updatePendingDownloadPausedUseCase(song.id, paused = true) }
            }
            _tasks.value = restored
        }
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
                        // 首次拿到真实总长时写入 Room，供杀进程后恢复进度
                        persistTotalBytesOnce(song.id, totalBytes)
                        val fraction = (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f)
                        updateProgress(song.id, fraction)
                    },
                    // 与协程取消联动：Job.cancel 后底层 OkHttp 循环会中断
                    isCancelled = { !isActive }
                )
            }.onSuccess {
                // 落盘成功后由 ObserveDownloadedSongs 刷新「已下载」，此处只清进行中项
                requests.remove(song.id)
                persistedTotalBytesSongIds.remove(song.id)
                removeTask(song.id)
                // downloadSong 成功路径已删 pending；此处再删一次保证兜底
                clearPending(song.id)
            }.onFailure { error ->
                if (error is CancellationException) {
                    // pause 会先标 paused 再 cancel，需保留任务与临时文件
                    // 仅当列表里已没有该任务（cancel 已 remove）或不存在暂停标记时，才当取消清理
                    val task = _tasks.value.find { it.songId == song.id }
                    if (task == null) {
                        requests.remove(song.id)
                        persistedTotalBytesSongIds.remove(song.id)
                        discardPartial(song.id)
                    } else if (!task.paused) {
                        requests.remove(song.id)
                        persistedTotalBytesSongIds.remove(song.id)
                        removeTask(song.id)
                        discardPartial(song.id)
                    }
                } else {
                    // 失败保留一条带 error 的任务，供播放页展示文案；写回暂停便于杀进程后恢复
                    updateError(song.id, error.message ?: "下载失败")
                    persistPaused(song.id, paused = true)
                }
            }
            jobs.remove(song.id)
        }
        jobs[song.id] = job
    }

    private fun persistPending(song: Song, quality: DownloadQuality, paused: Boolean) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                upsertPendingDownloadUseCase(
                    PendingDownload(
                        song = song,
                        quality = quality,
                        paused = paused,
                        totalBytes = 0L
                    )
                )
            }
        }
    }

    private fun persistPaused(songId: Long, paused: Boolean) {
        scope.launch(Dispatchers.IO) {
            runCatching { updatePendingDownloadPausedUseCase(songId, paused) }
        }
    }

    // 每个任务只写一次真实总长，避免进度回调打爆 Room
    private fun persistTotalBytesOnce(songId: Long, totalBytes: Long) {
        if (totalBytes <= 0L) return
        if (!persistedTotalBytesSongIds.add(songId)) return
        scope.launch(Dispatchers.IO) {
            runCatching { updatePendingDownloadTotalBytesUseCase(songId, totalBytes) }
                .onFailure { persistedTotalBytesSongIds.remove(songId) }
        }
    }

    private fun clearPending(songId: Long) {
        scope.launch(Dispatchers.IO) {
            runCatching { deletePendingDownloadUseCase(songId) }
        }
    }

    private fun discardPartial(songId: Long) {
        scope.launch(Dispatchers.IO) {
            runCatching { discardPartialDownloadUseCase(songId) }
            runCatching { deletePendingDownloadUseCase(songId) }
        }
    }

    // 有则原地替换（保持队列位置）、无则追加
    private fun upsertTask(task: ActiveDownloadTask) {
        _tasks.update { list ->
            val index = list.indexOfFirst { it.songId == task.songId }
            if (index < 0) {
                list + task
            } else {
                list.toMutableList().also { it[index] = task }
            }
        }
    }

    // 仅更新进度；不改 paused，避免暂停后迟到的进度回调把标记清掉，进而被当成取消清理
    private fun updateProgress(songId: Long, progress: Float) {
        _tasks.update { list ->
            list.map { task ->
                if (task.songId == songId) {
                    if (task.paused) {
                        // 已暂停：忽略迟到进度，保留暂停态与当前进度
                        task
                    } else {
                        task.copy(progress = progress, error = null)
                    }
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

    companion object {
        // 用临时文件已下字节 / 真实或估算总长，得到恢复时的进度
        fun progressFromPartialBytes(
            bytes: Long,
            durationMs: Long,
            quality: DownloadQuality,
            knownTotalBytes: Long = 0L
        ): Float {
            if (bytes <= 0L) return 0f
            val total = if (knownTotalBytes > 0L) {
                knownTotalBytes
            } else {
                estimateTotalBytes(durationMs, quality)
            }
            if (total <= 0L) return 0f
            return (bytes.toFloat() / total.toFloat()).coerceIn(0f, 0.99f)
        }

        // 与音质弹窗体积估算一致：无损按约 1000kbps
        private fun estimateTotalBytes(durationMs: Long, quality: DownloadQuality): Long {
            if (durationMs <= 0L) return 0L
            val estimateBps = when (quality) {
                DownloadQuality.Lossless -> 1_000_000L
                else -> quality.bitrate.toLong()
            }
            return (durationMs / 1000.0 * (estimateBps / 8.0)).toLong().coerceAtLeast(1L)
        }
    }
}
