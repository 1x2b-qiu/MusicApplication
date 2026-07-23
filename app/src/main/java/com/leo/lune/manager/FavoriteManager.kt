package com.leo.lune.manager

import com.leo.lune.domain.usecase.auth.ObserveLoginStateUseCase
import com.leo.lune.domain.usecase.music.GetLikedSongIdsUseCase
import com.leo.lune.domain.usecase.music.LikeSongUseCase
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

// 收藏操作结果；UI 层据此展示 Toast / Snackbar
sealed interface FavoriteResult {
    data object Success : FavoriteResult
    data class Failure(val message: String) : FavoriteResult
}

// 红心收藏管理器（Hilt 单例）
// 职责：维护本地红心 id 缓存、乐观更新 + 失败回滚、对外暴露当前曲收藏态
@Singleton
class FavoriteManager @Inject constructor(
    private val likeSongUseCase: LikeSongUseCase,
    private val getLikedSongIdsUseCase: GetLikedSongIdsUseCase,
    private val observeLoginStateUseCase: ObserveLoginStateUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)


    // 当前展示曲是否已收藏；UI 通过 PlaybackState.isFavorite 间接订阅
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()


    // 最近一次收藏操作结果（成功/失败），供 UI 展示提示；null 表示无事件
    private val _lastResult = MutableStateFlow<FavoriteResult?>(null)
    val lastResult: StateFlow<FavoriteResult?> = _lastResult.asStateFlow()

    // 本地缓存的红心歌单 id 集合，用于即时判断收藏态
    private var likedSongIds: Set<Long> = emptySet()
    // 最近一次 sync 的歌曲 id；红心列表刷新后用来补同步 UI
    private var currentSongId: Long? = null
    // 当前登录用户 id；null 表示未登录
    private var currentUserId: Long? = null

    init {
        // 启动时根据登录态预热红心列表，避免首屏收藏图标闪错
        scope.launch {
            val loginState = observeLoginStateUseCase().first()
            currentUserId = loginState.userId?.takeIf { loginState.isLoggedIn }
            if (currentUserId != null) {
                refreshFromServer(currentUserId!!)
            } else {
                likedSongIds = emptySet()
                _isFavorite.value = false
            }
        }
    }

    // 切换收藏（乐观更新）；未登录时返回失败，请求失败则回滚
    fun toggleFavorite(songId: Long) {
        if (currentUserId == null) {
            _lastResult.value = FavoriteResult.Failure("请先登录后收藏")
            return
        }

        val targetFavorite = !_isFavorite.value
        // 乐观更新
        _isFavorite.value = targetFavorite
        likedSongIds = if (targetFavorite) likedSongIds + songId else likedSongIds - songId

        scope.launch {
            runCatching {
                likeSongUseCase(songId, like = targetFavorite)
            }.onSuccess { result ->
                if (!result.success) {
                    revert(songId, targetFavorite, "收藏操作失败")
                } else {
                    _lastResult.value = FavoriteResult.Success
                }
            }.onFailure { throwable ->
                revert(songId, targetFavorite, throwable.message ?: "收藏操作失败")
            }
        }
    }

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
    fun refreshFromServer(userId: Long) {
        scope.launch {
            runCatching {
                getLikedSongIdsUseCase(userId)
            }.onSuccess { ids ->
                likedSongIds = ids.toSet()
                // 列表可能晚于快照恢复到达，补一次当前曲红心
                syncForSong(currentSongId)
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
        _isFavorite.value = !attemptedFavorite
        _lastResult.value = FavoriteResult.Failure(message)
    }
}
