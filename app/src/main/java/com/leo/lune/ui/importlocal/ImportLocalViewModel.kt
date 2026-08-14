package com.leo.lune.ui.importlocal

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.local.LocalFolderAudioScanner
import com.leo.lune.local.ScannedLocalTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 「导入本地歌曲」页 UI 状态（扫描结果仅内存，不落库）
data class ImportLocalUiState(
    // 当前选中的文件夹展示名
    val folderName: String? = null,
    // 是否正在扫描
    val isScanning: Boolean = false,
    // 扫描过程中已发现的曲目数（进度）
    val scannedCount: Int = 0,
    // 本次扫描结果（会话内有效；取消时保留已扫到的）
    val tracks: List<ScannedLocalTrack> = emptyList(),
    // 扫描失败信息
    val error: String? = null
)

// 「导入本地歌曲」ViewModel：自定义文件夹扫描，不做持久化
@HiltViewModel
class ImportLocalViewModel @Inject constructor(
    private val scanner: LocalFolderAudioScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportLocalUiState())
    // 对外只读，ImportLocalScreen 通过 collect 订阅
    val uiState: StateFlow<ImportLocalUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    // 协作取消：递归扫描在文件间隙检查，取消后保留已发现列表
    @Volatile
    private var cancelRequested: Boolean = false

    // 用户通过 SAF 选中文件夹后开始扫描
    fun onFolderPicked(treeUri: Uri, folderName: String) {
        cancelRequested = true
        scanJob?.cancel()
        cancelRequested = false
        scanJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    folderName = folderName,
                    isScanning = true,
                    scannedCount = 0,
                    tracks = emptyList(),
                    error = null
                )
            }
            runCatching {
                scanner.scan(
                    treeUri = treeUri,
                    isCancelled = { cancelRequested },
                    onTrackFound = { track ->
                        _uiState.update { state ->
                            if (!state.isScanning && cancelRequested) state
                            else state.copy(
                                tracks = state.tracks + track,
                                scannedCount = state.scannedCount + 1
                            )
                        }
                    }
                )
            }.onSuccess {
                _uiState.update { it.copy(isScanning = false, error = null) }
            }.onFailure { throwable ->
                if (cancelRequested) {
                    _uiState.update { it.copy(isScanning = false) }
                    return@onFailure
                }
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        error = throwable.message ?: "扫描失败，请重试"
                    )
                }
            }
        }
    }

    // 取消进行中的扫描，保留已扫到的结果
    fun cancelScan() {
        if (!_uiState.value.isScanning) return
        cancelRequested = true
        scanJob?.cancel()
        scanJob = null
        _uiState.update { it.copy(isScanning = false) }
    }

    // 清空本次扫描结果（不涉及落库）
    fun clearResults() {
        cancelRequested = true
        scanJob?.cancel()
        scanJob = null
        _uiState.value = ImportLocalUiState()
    }
}
