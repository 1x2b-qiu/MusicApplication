package com.leo.lune.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// 流媒体默认音质档位（仅 UI 骨架，暂不持久化）
enum class PlaybackQuality(
    val label: String,
    val detail: String,
    val badge: String? = null
) {
    Standard("标准", "128 kbps · 省流量"),
    High("高品质", "320 kbps · 推荐", badge = "推荐"),
    Lossless("无损", "FLAC · 最高音质", badge = "HiFi");

    companion object {
        val Default: PlaybackQuality = High
    }
}

data class PlaybackSettingsUiState(
    val selectedQuality: PlaybackQuality = PlaybackQuality.Default,
    // 与其他应用同时播放（不独占音频焦点）
    val mixWithOthers: Boolean = false
)

// 播放设置页：默认音质 + 音频混合开关（暂无持久化 / 播放器副作用）
@HiltViewModel
class PlaybackSettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PlaybackSettingsUiState())
    val uiState: StateFlow<PlaybackSettingsUiState> = _uiState.asStateFlow()

    fun selectQuality(quality: PlaybackQuality) {
        _uiState.update { it.copy(selectedQuality = quality) }
    }

    fun setMixWithOthers(enabled: Boolean) {
        _uiState.update { it.copy(mixWithOthers = enabled) }
    }
}
