package com.leo.lune.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.SettingKeys
import com.leo.lune.domain.repository.SettingsRepository
import com.leo.lune.domain.usecase.settings.ObserveDownloadStorageLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// 下载设置页 UI 状态
data class DownloadSettingsUiState(
    val selectedQuality: DownloadQuality = DownloadQuality.Default,
    // 存储位置副标题：绝对路径；SAF 解析失败时为 content URI
    val storagePathHint: String = ""
)

// 下载设置：默认音质直连 SettingsRepository；存储位置仍走跨仓储 UseCase
@HiltViewModel
class DownloadSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    observeDownloadStorageLocationUseCase: ObserveDownloadStorageLocationUseCase
) : ViewModel() {

    val uiState: StateFlow<DownloadSettingsUiState> = combine(
        settingsRepository.observeValue(SettingKeys.DOWNLOAD_DEFAULT_QUALITY).map { raw ->
            val bitrate = raw?.toIntOrNull() ?: return@map DownloadQuality.Default
            DownloadQuality.fromBitrate(bitrate)
        },
        observeDownloadStorageLocationUseCase()
    ) { quality, storage ->
        DownloadSettingsUiState(
            selectedQuality = quality,
            storagePathHint = storage.pathHint
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DownloadSettingsUiState()
    )

    fun selectQuality(quality: DownloadQuality) {
        viewModelScope.launch {
            settingsRepository.setValue(
                SettingKeys.DOWNLOAD_DEFAULT_QUALITY,
                quality.bitrate.toString()
            )
        }
    }

    // 保存用户通过 SAF 选择的下载目录（tree URI + 展示名）
    fun setStorageLocation(treeUri: String, displayName: String) {
        viewModelScope.launch {
            settingsRepository.setValue(SettingKeys.DOWNLOAD_STORAGE_TREE_URI, treeUri)
            settingsRepository.setValue(
                SettingKeys.DOWNLOAD_STORAGE_DISPLAY_NAME,
                displayName.ifBlank { "已选择文件夹" }
            )
        }
    }
}
