package com.leo.lune.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject


// 设置页展示状态（暂不接真实业务，仅驱动 UI）
data class SettingsUiState(
    // 首页顶栏是否展示实时歌词
    val statusBarLyricsEnabled: Boolean = true,
    // 是否仅允许 Wi-Fi 下载
    val wifiOnlyDownload: Boolean = true,
    // 缓存体积展示文案（占位）
    val cacheSizeLabel: String = "286 MB",
    // 当前应用版本号
    val appVersion: String = "1.0",
    // 是否正在检查更新（用于右侧文案切换）
    val checkingUpdate: Boolean = false
)

// 设置页 ViewModel：先只维护界面状态，功能后续再接
@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    // 对外只读，SettingsScreen 通过 collect 订阅
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // 切换状态栏歌词开关（仅改 UI 状态，暂不持久化）
    fun toggleStatusBarLyrics() {
        _uiState.update { it.copy(statusBarLyricsEnabled = !it.statusBarLyricsEnabled) }
    }


    // 检查更新：占位，后续再接真实检查逻辑
    fun checkUpdate() {
        // 占位：后续再接更新检查
    }
}
