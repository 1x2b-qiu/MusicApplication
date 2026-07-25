package com.leo.lune.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.usecase.settings.ObserveDefaultDownloadQualityUseCase
import com.leo.lune.domain.usecase.settings.SetDefaultDownloadQualityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// 下载设置页 UI 状态
data class DownloadSettingsUiState(
    val selectedQuality: DownloadQuality = DownloadQuality.Default
)

// 下载设置：默认音质读写 Room；存储位置展示后续再接选目录
@HiltViewModel
class DownloadSettingsViewModel @Inject constructor(
    observeDefaultDownloadQualityUseCase: ObserveDefaultDownloadQualityUseCase,
    private val setDefaultDownloadQualityUseCase: SetDefaultDownloadQualityUseCase
) : ViewModel() {

    val uiState: StateFlow<DownloadSettingsUiState> =
        observeDefaultDownloadQualityUseCase()
            .map { DownloadSettingsUiState(selectedQuality = it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DownloadSettingsUiState()
            )

    fun selectQuality(quality: DownloadQuality) {
        viewModelScope.launch {
            setDefaultDownloadQualityUseCase(quality)
        }
    }
}
