package com.leo.lune.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.domain.model.PlaybackQuality
import com.leo.lune.domain.model.SettingKeys
import com.leo.lune.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaybackSettingsUiState(
    val selectedQuality: PlaybackQuality = PlaybackQuality.Default,
    // 与其他应用同时播放（不独占音频焦点）
    val mixWithOthers: Boolean = false
)

// 播放设置：默认流媒体音质 + 音频混合开关（直连 SettingsRepository）
@HiltViewModel
class PlaybackSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<PlaybackSettingsUiState> = combine(
        settingsRepository.observeValue(SettingKeys.PLAYBACK_DEFAULT_QUALITY).map { raw ->
            val bitrate = raw?.toIntOrNull() ?: return@map PlaybackQuality.Default
            PlaybackQuality.fromBitrate(bitrate)
        },
        settingsRepository.observeValue(SettingKeys.PLAYBACK_MIX_WITH_OTHERS).map { raw ->
            raw == "true"
        }
    ) { quality, mixWithOthers ->
        PlaybackSettingsUiState(
            selectedQuality = quality,
            mixWithOthers = mixWithOthers
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlaybackSettingsUiState()
    )

    fun selectQuality(quality: PlaybackQuality) {
        viewModelScope.launch {
            settingsRepository.setValue(
                SettingKeys.PLAYBACK_DEFAULT_QUALITY,
                quality.bitrate.toString()
            )
        }
    }

    fun setMixWithOthers(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setValue(
                SettingKeys.PLAYBACK_MIX_WITH_OTHERS,
                if (enabled) "true" else "false"
            )
        }
    }
}
