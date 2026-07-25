package com.leo.lune.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.usecase.settings.ObserveDefaultDownloadQualityUseCase
import com.leo.lune.domain.usecase.settings.SetDefaultDownloadQualityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 存储位置选项（仅 UI 占位，后续再接真实路径）
enum class DownloadStorageLocation(
    val label: String,
    val pathHint: String
) {
    Internal("内部存储", "我的音乐 / Downloads"),
    SdCard("SD 卡", "SD Card / Downloads")
}

// 下载设置页 UI 状态
data class DownloadSettingsUiState(
    val selectedQuality: DownloadQuality = DownloadQuality.Default,
    val storageLocation: DownloadStorageLocation = DownloadStorageLocation.Internal
)

// 下载设置：默认音质读写 Room；存储位置仍仅 UI 占位
@HiltViewModel
class DownloadSettingsViewModel @Inject constructor(
    observeDefaultDownloadQualityUseCase: ObserveDefaultDownloadQualityUseCase,
    private val setDefaultDownloadQualityUseCase: SetDefaultDownloadQualityUseCase
) : ViewModel() {

    private val storageLocation = MutableStateFlow(DownloadStorageLocation.Internal)

    val uiState: StateFlow<DownloadSettingsUiState> = combine(
        observeDefaultDownloadQualityUseCase(),
        storageLocation
    ) { quality, location ->
        DownloadSettingsUiState(
            selectedQuality = quality,
            storageLocation = location
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DownloadSettingsUiState()
    )

    fun selectQuality(quality: DownloadQuality) {
        viewModelScope.launch {
            setDefaultDownloadQualityUseCase(quality)
        }
    }

    // 设计稿交互：点击在内部存储 / SD 卡之间切换（暂不持久化）
    fun toggleStorageLocation() {
        storageLocation.update { current ->
            when (current) {
                DownloadStorageLocation.Internal -> DownloadStorageLocation.SdCard
                DownloadStorageLocation.SdCard -> DownloadStorageLocation.Internal
            }
        }
    }
}
