package com.leo.lune.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.usecase.settings.ObserveDefaultDownloadQualityUseCase
import com.leo.lune.domain.usecase.settings.ObserveDownloadStorageLocationUseCase
import com.leo.lune.domain.usecase.settings.SetDefaultDownloadQualityUseCase
import com.leo.lune.domain.usecase.settings.SetDownloadStorageLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// 下载设置页 UI 状态
data class DownloadSettingsUiState(
    val selectedQuality: DownloadQuality = DownloadQuality.Default,
    // 存储位置副标题：绝对路径；SAF 解析失败时为 content URI
    val storagePathHint: String = ""
)

// 下载设置：默认音质 + SAF 存储目录（仅依赖 settings UseCase，不直连 data）
@HiltViewModel
class DownloadSettingsViewModel @Inject constructor(
    observeDefaultDownloadQualityUseCase: ObserveDefaultDownloadQualityUseCase,
    observeDownloadStorageLocationUseCase: ObserveDownloadStorageLocationUseCase,
    private val setDefaultDownloadQualityUseCase: SetDefaultDownloadQualityUseCase,
    private val setDownloadStorageLocationUseCase: SetDownloadStorageLocationUseCase
) : ViewModel() {

    val uiState: StateFlow<DownloadSettingsUiState> = combine(
        observeDefaultDownloadQualityUseCase(),
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
            setDefaultDownloadQualityUseCase(quality)
        }
    }

    // 保存用户通过 SAF 选择的下载目录
    fun setStorageLocation(treeUri: String, displayName: String) {
        viewModelScope.launch {
            setDownloadStorageLocationUseCase(treeUri, displayName)
        }
    }
}
