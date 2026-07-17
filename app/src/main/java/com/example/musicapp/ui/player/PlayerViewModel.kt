package com.example.musicapp.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.controller.player.MusicPlayerController
import com.example.musicapp.controller.player.PlayerPlayMode
import com.example.musicapp.domain.model.LyricLine
import com.example.musicapp.domain.model.LyricMatcher
import com.example.musicapp.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// 全屏播放页 UI 状态（数据来自全局 MusicPlayerController）
data class PlayerUiState(
    val songId: Long = 0L,
    val songName: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val coverUrl: String? = null,
    // 是否正在解析播放地址
    val isLoading: Boolean = false,
    val playUrl: String? = null,
    val error: String? = null,
    val isPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    // 播放队列与当前下标
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = 0,
    val lyrics: List<LyricLine> = emptyList(),
    val playMode: PlayerPlayMode = PlayerPlayMode.Shuffle,
    // 进度（毫秒），来自 PlaybackState
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L
) {
    // 当前高亮歌词行（与 Controller / LyricMatcher 同一套匹配算法）
    val activeLyricIndex: Int
        get() = LyricMatcher.currentIndex(lyrics, currentPositionMs)

    // 进度条比例 0f..1f
    val progressFraction: Float
        get() = if (durationMs <= 0L) 0f else (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

// 全屏播放页 ViewModel：只读全局播放状态，播控委托 MusicPlayerController
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: MusicPlayerController
) : ViewModel() {

    // 映射本页需要的字段并过滤无关更新
    val uiState: StateFlow<PlayerUiState> = playerController.playbackState
        .map { state ->
            val song = state.displaySong
            PlayerUiState(
                songId = song?.id ?: 0L,
                songName = song?.name.orEmpty(),
                artistName = song?.artists.orEmpty(),
                albumName = song?.album.orEmpty(),
                coverUrl = song?.coverUrl,
                isLoading = state.isLoading,
                playUrl = state.playUrl,
                error = state.error,
                isPlaying = state.isPlaying,
                isFavorite = state.isFavorite,
                queue = state.queue,
                queueIndex = state.queueIndex,
                lyrics = state.lyrics,
                playMode = state.playMode,
                currentPositionMs = state.currentPositionMs,
                durationMs = song?.durationMs ?: 0L
            )
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlayerUiState()
        )

    fun togglePlayPause() = playerController.togglePlayPause()

    fun toggleFavorite() = playerController.toggleFavorite()

    fun skipToNext() = playerController.skipToNext()

    fun skipToPrevious() = playerController.skipToPrevious()

    // 进度条拖动结束后定位
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    // 点击歌词跳转到对应时间
    fun seekToLyric(index: Int) {
        val lyric = uiState.value.lyrics.getOrNull(index) ?: return
        playerController.seekTo(lyric.timeMs)
    }

    // 播放队列中指定项
    fun playQueueItemAt(index: Int) = playerController.playQueueItemAt(index)

    // 切换播放模式，逻辑在 Controller
    fun cyclePlayMode() = playerController.cyclePlayMode()
}
