package com.example.musicapp.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.controller.player.MusicPlayerController
import com.example.musicapp.controller.player.PlaybackState
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.usecase.ObserveLoginStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// 播放页 UI 状态
data class PlayerUiState(
    // 当前歌曲 ID
    val songId: Long = 0L,
    // 歌曲名
    val songName: String = "",
    // 艺人名
    val artistName: String = "",
    // 封面地址
    val coverUrl: String? = null,
    // 是否已登录，用于控制顶部登录入口
    val isLoggedIn: Boolean = false,
    // 是否正在拉取播放地址
    val isLoading: Boolean = false,
    // 当前可用的流媒体地址
    val playUrl: String? = null,
    // 播放或拉取地址时的错误信息
    val error: String? = null,
    // 是否正在播放
    val isPlaying: Boolean = false
)

// 播放页 ViewModel
// 将全局 MusicPlayerController 的播放状态映射为页面 UI 状态
// 若进入页面时还没有正在播放的歌曲，则根据导航参数自动发起播放
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: MusicPlayerController,
    observeLoginStateUseCase: ObserveLoginStateUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 从导航参数读取的歌曲信息，作为播放前的兜底展示数据
    private val navSongId: Long = savedStateHandle.get<Long>("songId") ?: 0L
    private val navSongName: String = savedStateHandle.get<String>("songName").orEmpty()
    private val navArtistName: String = savedStateHandle.get<String>("artistName").orEmpty()
    private val navCoverUrl: String? = savedStateHandle.get<String>("coverUrl")?.takeIf { it.isNotBlank() }

    // 进页时读取一次，不再持续观察登录态
    private val isLoggedIn = MutableStateFlow(false)

    // 直接暴露 ExoPlayer，供 PlayerScreen 挂载 PlayerView
    val exoPlayer get() = playerController.exoPlayer

    // 合并播放状态与一次性登录结果，生成页面 UI 状态
    val uiState: StateFlow<PlayerUiState> = combine(
        playerController.playbackState,
        isLoggedIn
    ) { playback, loggedIn ->
        playback.toPlayerUiState(loggedIn)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState()
    )

    init {
        viewModelScope.launch {
            isLoggedIn.value = observeLoginStateUseCase().first().isLoggedIn
        }
        // 若全局播放器尚未开始播歌，则使用导航参数触发首次播放
        val current = playerController.playbackState.value.currentSong
        if (current == null && navSongId > 0L) {
            playerController.playSong(
                Song(
                    id = navSongId,
                    name = navSongName,
                    artists = navArtistName,
                    album = "",
                    coverUrl = navCoverUrl,
                    durationMs = 0L
                )
            )
        }
    }

    // 将全局 PlaybackState 转换为播放页展示状态
    // 播放器尚未返回歌曲信息时，回退到导航参数
    private fun PlaybackState.toPlayerUiState(isLoggedIn: Boolean): PlayerUiState {
        val song = currentSong
        return PlayerUiState(
            songId = song?.id ?: navSongId,
            songName = song?.name ?: navSongName,
            artistName = song?.artists ?: navArtistName,
            coverUrl = song?.coverUrl ?: navCoverUrl,
            isLoggedIn = isLoggedIn,
            isLoading = isLoading,
            playUrl = playUrl,
            error = error,
            isPlaying = isPlaying
        )
    }
}
