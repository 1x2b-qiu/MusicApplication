package com.leo.lune.manager

import com.leo.lune.domain.repository.AuthRepository
import com.leo.lune.domain.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// 收藏操作结果；UI 层据此展示 Toast
sealed interface FavoriteResult {
    // liked=true 表示已喜欢，false 表示已取消喜欢
    data class Success(val liked: Boolean) : FavoriteResult
    data class Failure(val message: String) : FavoriteResult
}

// 红心收藏管理器（Hilt 单例）
// 职责：维护本地红心 id 缓存、乐观更新 + 失败回滚、对外暴露当前曲收藏态
@Singleton
class FavoriteManager @Inject constructor(
    private val musicRepository: MusicRepository,
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)


    // 当前展示曲是否已收藏；UI 通过 PlaybackState.isFavorite 间接订阅
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()


    // 收藏操作结果事件（成功/失败），供 UI 展示 Toast；SharedFlow 保证连续同结果也能收到
    private val _results = MutableSharedFlow<FavoriteResult>(extraBufferCapacity = 1)
    val results: SharedFlow<FavoriteResult> = _results.asSharedFlow()

    // 本地缓存的红心歌单 id 集合，用于即时判断收藏态
    private var likedSongIds: Set<Long> = emptySet()
    // 最近一次 sync 的歌曲 id；红心列表刷新后用来补同步 UI
    private var currentSongId: Long? = null
    // 当前登录用户 id；null 表示未登录
    private var currentUserId: Long? = null

    init {
        // 持续观察登录态：登录（含会话恢复）后预热红心列表，登出后清空缓存
        scope.launch {
            authRepository.observeLoginState().collect { loginState ->
                val userId = loginState.userId?.takeIf { loginState.isLoggedIn }
                if (userId != currentUserId) {
                    currentUserId = userId
                    if (userId != null) {
                        refreshFromServer(userId)
                    } else {
                        likedSongIds = emptySet()
                        _isFavorite.value = false
                    }
                }
            }
        }
    }

    // 切换收藏（乐观更新）；未登录时返回失败，请求失败则回滚
    fun toggleFavorite(songId: Long) {
        if (currentUserId == null) {
            _results.tryEmit(FavoriteResult.Failure("请先登录后收藏"))
            return
        }

        // 以本地红心集合为准，避免预览曲与播放器当前曲不一致时取反错误
        val targetFavorite = songId !in likedSongIds
        likedSongIds = if (targetFavorite) likedSongIds + songId else likedSongIds - songId
        if (currentSongId == null || currentSongId == songId) {
            _isFavorite.value = targetFavorite
        }

        scope.launch {
            runCatching {
                musicRepository.likeSong(songId, like = targetFavorite)
            }.onSuccess { result ->
                if (!result.success) {
                    revert(songId, targetFavorite, "收藏操作失败")
                } else {
                    _results.tryEmit(FavoriteResult.Success(liked = targetFavorite))
                }
            }.onFailure { throwable ->
                revert(songId, targetFavorite, throwable.message ?: "收藏操作失败")
            }
        }
    }

    // 查询某首歌是否在本地红心缓存中（不改动当前展示态）
    fun isFavoriteSong(songId: Long): Boolean = songId in likedSongIds

    // 切歌 / 预览曲变化时调用：用本地缓存即时同步收藏态
    fun syncForSong(songId: Long?) {
        if (songId == null) return
        currentSongId = songId
        val favorite = songId in likedSongIds
        if (_isFavorite.value != favorite) {
            _isFavorite.value = favorite
        }
    }

    // 从服务端刷新红心 id 集合，并同步当前曲收藏态
    // 失败时延迟重试：服务先于 Activity 重建时 Cookie 尚未恢复，首次请求可能失败
    fun refreshFromServer(userId: Long) {
        scope.launch {
            repeat(REFRESH_ATTEMPTS) { attempt ->
                // 用户已切换 / 登出，放弃本次（过期的）刷新
                if (currentUserId != userId) return@launch
                val succeeded = runCatching { musicRepository.getLikedSongIds(userId) }
                    .onSuccess { ids ->
                        likedSongIds = ids.toSet()
                        // 列表可能晚于快照恢复到达，补一次当前曲红心
                        syncForSong(currentSongId)
                    }
                    .isSuccess
                if (succeeded) return@launch
                if (attempt < REFRESH_ATTEMPTS - 1) delay(REFRESH_RETRY_DELAY_MS)
            }
        }
    }

    // 登录成功后外部调用：刷新缓存
    fun onLoginSuccess(userId: Long) {
        currentUserId = userId
        refreshFromServer(userId)
    }

    // 登出后外部调用：清空缓存
    fun onLogout() {
        currentUserId = null
        likedSongIds = emptySet()
        _isFavorite.value = false
    }

    // 收藏请求失败时回滚本地缓存与状态
    private fun revert(songId: Long, attemptedFavorite: Boolean, message: String) {
        likedSongIds = if (attemptedFavorite) likedSongIds - songId else likedSongIds + songId
        if (currentSongId == songId) {
            _isFavorite.value = !attemptedFavorite
        }
        _results.tryEmit(FavoriteResult.Failure(message))
    }

    private companion object {
        // 红心列表刷新总尝试次数
        const val REFRESH_ATTEMPTS = 4
        // 相邻两次刷新之间的间隔；覆盖「划掉后台→重新进入」的会话恢复窗口
        const val REFRESH_RETRY_DELAY_MS = 2_000L
    }
}
