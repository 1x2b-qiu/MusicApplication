package com.leo.lune.ui.radio

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.controller.PlaybackPosition
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.AuthRepository
import com.leo.lune.domain.repository.MusicRepository
import com.leo.lune.manager.FavoriteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 私人 FM 电台页 UI 状态（由 FM 源数据 + 播放器状态合成，供界面订阅）
data class RadioUiState(
    // 已拉取的 FM 歌曲（含续拉追加）
    val songs: List<Song> = emptyList(),
    // 当前展示曲（会话中为播放器当前曲，否则为待播的首支）
    val songName: String = "",
    val artistName: String = "",
    val coverUrl: String? = null,
    val songId: Long = 0L,
    val durationMs: Long = 0L,
    // 仅 FM 会话中有值，来自播放器
    val qualityLabel: String = "",
    val isPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    // 是否已用私人 FM 队列开播
    val isFmSession: Boolean = false,
    // 是否正在请求 /personal_fm
    val isLoading: Boolean = false,
    // 优先展示列表加载错误；会话中可回落到播放器错误
    val error: String? = null
)

// 私人 FM 电台 ViewModel
// 拉 /personal_fm、续播追加队列；进入页/列表就绪后自动开播；播控委托 MusicPlayerController
@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class RadioViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val authRepository: AuthRepository,
    private val playerController: MusicPlayerController,
    private val favoriteManager: FavoriteManager
) : ViewModel() {

    // —— FM 列表源数据（本页自有，与播放器无关）——
    // 与 _uiState 分开：作为 combine 输入，避免「自己 combine 自己」和多处改同一份状态互相覆盖
    // 已拉取的私人 FM 列表（含续拉追加）
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    // 列表加载/不可用提示；null 表示无错误
    private val _loadError = MutableStateFlow<String?>(null)
    // 是否正在请求私人 FM（首拉或重置拉）
    private val _isLoading = MutableStateFlow(false)

    // —— 对外 UI 快照：combine(播放器 + 上方三源) 写入 ——
    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    // 高频进度；仅进度条区域订阅，避免整页随进度重绘
    val positionState: StateFlow<PlaybackPosition> = playerController.playbackPosition

    // 首拉 / 重置拉任务；登录态变化或重试时会取消旧任务
    private var loadJob: Job? = null
    // 后台预拉下一批；与「下一首」续拉共用 appendMoreAndAwait
    private var appendJob: Job? = null

    init {
        // 登录后拉 FM；退出则清空列表并提示登录
        viewModelScope.launch {
            authRepository.observeLoginState().collect { loginState ->
                if (loginState.isLoggedIn) {
                    loadPersonalFm(reset = true)
                } else {
                    loadJob?.cancel()
                    appendJob?.cancel()
                    _songs.value = emptyList()
                    _isLoading.value = false
                    _loadError.value = "登录后收听私人 FM"
                }
            }
        }
        // 合成展示态；FM 会话中接近队尾时触发预拉
        viewModelScope.launch {
            combine(
                playerController.playbackState,
                _songs,
                _isLoading,
                _loadError
            ) { playback, songs, isLoading, loadError ->
                val fmIds = songs.mapTo(HashSet()) { it.id }
                // 当前播放队列整段都属于本页 FM 列表，才算 FM 会话
                val isFmSession = songs.isNotEmpty() &&
                    playback.currentSong?.id in fmIds &&
                    playback.queue.isNotEmpty() &&
                    playback.queue.all { it.id in fmIds }
                val displaySong = if (isFmSession) playback.currentSong else songs.firstOrNull()
                val songId = displaySong?.id ?: 0L
                RadioUiState(
                    songs = songs,
                    songId = songId,
                    songName = displaySong?.name.orEmpty(),
                    artistName = displaySong?.artists.orEmpty(),
                    coverUrl = displaySong?.coverUrl,
                    durationMs = displaySong?.durationMs ?: 0L,
                    qualityLabel = if (isFmSession) playback.qualityLabel else "",
                    isPlaying = isFmSession && playback.isPlaying,
                    isFavorite = when {
                        songId == 0L -> false
                        isFmSession -> playback.isFavorite
                        else -> favoriteManager.isFavoriteSong(songId)
                    },
                    isFmSession = isFmSession,
                    isLoading = isLoading,
                    error = when {
                        loadError != null -> loadError
                        isFmSession -> playback.error
                        else -> null
                    }
                ) to (isFmSession to (playback.queueIndex to playback.queue.size))
            }
                .distinctUntilChanged()
                .collect { (ui, sessionMeta) ->
                    _uiState.value = ui
                    val (isFmSession, queueMeta) = sessionMeta
                    if (isFmSession) {
                        maybeAppendMore(queueMeta.first, queueMeta.second)
                    }
                }
        }
        // 未开播时收藏结果不走播放器，需单独刷新心形
        viewModelScope.launch {
            favoriteManager.results.collect {
                val current = _uiState.value
                if (current.songId != 0L && !current.isFmSession) {
                    _uiState.update {
                        it.copy(isFavorite = favoriteManager.isFavoriteSong(it.songId))
                    }
                }
            }
        }
    }

    // 进入电台页：有列表且尚未在 FM 会话中则开播；已在会话中不打断（含暂停）
    fun startFmPlaybackIfNeeded() {
        val songs = _songs.value
        if (songs.isEmpty()) return
        if (isCurrentFmSession(songs)) return
        playerController.playSong(songs.first(), songs)
    }

    // 未开播：用当前 FM 列表 playSong；已在会话中：委托播放器暂停/继续
    fun togglePlayPause() {
        val state = _uiState.value
        val songs = state.songs
        if (songs.isEmpty()) return
        if (state.isFmSession) {
            playerController.togglePlayPause()
            return
        }
        playerController.playSong(songs.first(), songs)
    }

    // 未开播：等同开播首曲；会话中：切下一首，队尾则续拉后再切
    fun skipToNext() {
        if (_uiState.value.songs.isEmpty()) return
        viewModelScope.launch {
            if (!_uiState.value.isFmSession) {
                val songs = _songs.value
                if (songs.isNotEmpty()) playerController.playSong(songs.first(), songs)
                return@launch
            }
            // 队尾时 personal_fm 需再拉一批；失败/重复时旧逻辑会静默 return
            val nextSong = resolveNextFmSong() ?: return@launch
            playerController.playSong(nextSong, _songs.value)
        }
    }

    // 仅在已拉到的队列内回退；队首则回到曲头，不环形跳到末尾
    fun skipToPrevious() {
        val state = _uiState.value
        if (state.songs.isEmpty() || !state.isFmSession) return
        val playback = playerController.playbackState.value
        val index = playback.queue.indexOfFirst { it.id == playback.currentSong?.id }
            .takeIf { it >= 0 } ?: playback.queueIndex
        if (index <= 0) {
            playerController.seekTo(0)
            return
        }
        val songs = state.songs
        if (index in songs.indices) {
            playerController.playSong(songs[index - 1], songs)
        }
    }

    // 仅 FM 会话中可拖进度
    fun seekTo(positionMs: Long) {
        if (!_uiState.value.isFmSession) return
        playerController.seekTo(positionMs)
    }

    // 会话中走播放器收藏；未开播走 FavoriteManager 并立刻改 UI
    fun toggleFavorite() {
        val songId = _uiState.value.songId.takeIf { it != 0L } ?: return
        if (_uiState.value.isFmSession) {
            playerController.toggleFavorite()
        } else {
            favoriteManager.toggleFavorite(songId)
            _uiState.update {
                it.copy(isFavorite = favoriteManager.isFavoriteSong(songId))
            }
        }
    }

    // 失败重试：清空后重新拉一批
    fun onRetry() {
        loadPersonalFm(reset = true)
    }

    // 请求 /personal_fm；reset=true 时先清空本地列表
    private fun loadPersonalFm(reset: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            if (reset) _songs.value = emptyList()
            runCatching {
                musicRepository.getPersonalFmSongs()
            }.onSuccess { songs ->
                _songs.value = songs
                _isLoading.value = false
                _loadError.value = if (songs.isEmpty()) "暂时没有可播放的歌曲" else null
                songs.firstOrNull()?.let { favoriteManager.syncForSong(it.id) }
                // 首拉/重试成功后自动开播
                if (songs.isNotEmpty()) startFmPlaybackIfNeeded()
            }.onFailure { throwable ->
                _isLoading.value = false
                _loadError.value = throwable.message ?: "加载私人 FM 失败"
            }
        }
    }

    // 播放器当前队列是否整段属于给定 FM 列表
    private fun isCurrentFmSession(songs: List<Song>): Boolean {
        val playback = playerController.playbackState.value
        if (songs.isEmpty() || playback.queue.isEmpty()) return false
        val fmIds = songs.mapTo(HashSet()) { it.id }
        return playback.currentSong?.id in fmIds &&
            playback.queue.all { it.id in fmIds }
    }

    // 距队尾不足 APPEND_THRESHOLD 时后台预拉
    private fun maybeAppendMore(queueIndex: Int, queueSize: Int) {
        if (queueSize - queueIndex > APPEND_THRESHOLD) return
        appendMore()
    }

    // 解析下一首：若已到本地队尾则续拉 personal_fm，直到拿到新歌或用尽重试
    private suspend fun resolveNextFmSong(): Song? {
        val currentId = playerController.playbackState.value.currentSong?.id
        repeat(APPEND_MAX_ATTEMPTS) { attempt ->
            val songs = _songs.value
            val index = songs.indexOfFirst { it.id == currentId }
                .takeIf { it >= 0 }
                ?: playerController.playbackState.value.queueIndex
            val nextIndex = index + 1
            if (nextIndex in songs.indices) return songs[nextIndex]

            val appended = appendMoreAndAwait()
            if (!appended && attempt == APPEND_MAX_ATTEMPTS - 1) {
                _loadError.value = "暂时没有更多推荐，请稍后再试"
            }
        }
        return null
    }

    // 非阻塞预拉入口；已有续拉或首拉进行中则跳过
    private fun appendMore() {
        if (appendJob?.isActive == true) return
        if (_isLoading.value) return
        appendJob = viewModelScope.launch {
            appendMoreAndAwait()
        }
    }

    // 拉取下一批并入队列；有新增返回 true，空/重复/失败返回 false
    private suspend fun appendMoreAndAwait(): Boolean {
        // 若已有续拉任务在跑，先等它结束再看结果，避免点下一首时空等
        appendJob?.takeIf { it.isActive && it != kotlinx.coroutines.currentCoroutineContext()[Job] }
            ?.join()
        val sizeBefore = _songs.value.size
        if (_songs.value.size > sizeBefore) return true

        return runCatching {
            musicRepository.getPersonalFmSongs()
        }.fold(
            onSuccess = { more ->
                if (more.isEmpty()) return@fold false
                val existingIds = _songs.value.mapTo(HashSet()) { it.id }
                val unique = more.filter { it.id !in existingIds }
                if (unique.isEmpty()) return@fold false
                _songs.value = _songs.value + unique
                if (_uiState.value.isFmSession) {
                    playerController.appendToQueue(unique)
                }
                true
            },
            onFailure = { false }
        )
    }

    companion object {
        // 距队尾剩余曲目少于此值时预拉下一批
        private const val APPEND_THRESHOLD = 2
        // 点下一首时最多连续请求个人 FM 次数
        private const val APPEND_MAX_ATTEMPTS = 3
    }
}
