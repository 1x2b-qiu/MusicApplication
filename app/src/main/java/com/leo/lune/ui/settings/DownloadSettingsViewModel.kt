package com.leo.lune.ui.settings

import androidx.lifecycle.ViewModel
import com.leo.lune.domain.model.DownloadQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

// 下载设置：仅维护 UI 选中态，持久化与读写逻辑后续再接
@HiltViewModel
class DownloadSettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadSettingsUiState())
    val uiState: StateFlow<DownloadSettingsUiState> = _uiState.asStateFlow()

    fun selectQuality(quality: DownloadQuality) {
        _uiState.update { it.copy(selectedQuality = quality) }
    }

    // 设计稿交互：点击在内部存储 / SD 卡之间切换
    fun toggleStorageLocation() {
        _uiState.update { state ->
            val next = when (state.storageLocation) {
                DownloadStorageLocation.Internal -> DownloadStorageLocation.SdCard
                DownloadStorageLocation.SdCard -> DownloadStorageLocation.Internal
            }
            state.copy(storageLocation = next)
        }
    }
}
