package com.leo.lune.ui.importlocal

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.domain.model.LocalTrackImportDraft
import com.leo.lune.domain.repository.ImportedLocalTrackRepository
import com.leo.lune.local.LocalFolderAudioScanner
import com.leo.lune.local.MediaStoreAudioScanner
import com.leo.lune.local.ScannedLocalTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 「导入本地歌曲」页 UI 状态（扫描结果先在内存预览，确认后再写入 Room）
data class ImportLocalUiState(
    // 当前扫描来源的展示名（文件夹名 / 「全盘」）
    val folderName: String? = null,
    // 是否正在扫描
    val isScanning: Boolean = false,
    // 是否正在写入曲库
    val isImporting: Boolean = false,
    // 扫描过程中已发现的曲目数（进度）
    val scannedCount: Int = 0,
    // 本次扫描结果（会话内有效；取消时保留已扫到的）
    val tracks: List<ScannedLocalTrack> = emptyList(),
    // 扫描失败信息
    val error: String? = null,
    // 导入成功提示
    val importMessage: String? = null
)

// 「导入本地歌曲」ViewModel：全盘 / 自定义文件夹扫描，确认后写入导入表
@HiltViewModel
class ImportLocalViewModel @Inject constructor(
    private val scanner: LocalFolderAudioScanner,
    private val mediaStoreScanner: MediaStoreAudioScanner,
    private val importedLocalTrackRepository: ImportedLocalTrackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportLocalUiState())
    // 对外只读，ImportLocalScreen 通过 collect 订阅
    val uiState: StateFlow<ImportLocalUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var importJob: Job? = null
    private var currentTreeUri: Uri? = null
    // 协作取消：递归扫描在文件间隙检查，取消后保留已发现列表
    @Volatile
    private var cancelRequested: Boolean = false

    // 全盘扫描（MediaStore）；须已授予音频读取权限，由页面在申请通过后调用
    // 已入库 / 已下载的曲目不跳过，用户可自行决定是否重复导入（同 URI 导入只做元数据覆盖）
    fun startDeviceScan() {
        launchScan(folderLabel = "全盘", treeUri = Uri.parse(MediaStoreAudioScanner.MediaStoreTreeUri)) {
            // 全设备曲目量大，按批刷新预览，避免每首一次 StateFlow 发射
            val buffer = ArrayList<ScannedLocalTrack>(FlushBatchSize)
            mediaStoreScanner.scan(
                isCancelled = { cancelRequested },
                onTrackFound = { track ->
                    buffer += track
                    if (buffer.size >= FlushBatchSize) {
                        appendScannedTracks(buffer.toList())
                        buffer.clear()
                    }
                }
            )
            if (buffer.isNotEmpty()) appendScannedTracks(buffer.toList())
        }
    }

    // 用户通过 SAF 选中文件夹后开始扫描
    fun onFolderPicked(treeUri: Uri, folderName: String) {
        launchScan(folderLabel = folderName, treeUri = treeUri) {
            scanner.scan(
                treeUri = treeUri,
                isCancelled = { cancelRequested },
                onTrackFound = { track -> appendScannedTracks(track) }
            )
        }
    }

    // 将当前预览列表写入导入表；同一文档 URI 覆盖元数据并保留 localId
    fun importScannedTracks() {
        val treeUri = currentTreeUri ?: return
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty() || _uiState.value.isScanning || _uiState.value.isImporting) return
        importJob?.cancel()
        importJob = viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importMessage = null, error = null) }
            runCatching {
                importedLocalTrackRepository.importTracks(
                    tracks.map { it.toDraft(treeUri.toString()) }
                )
            }.onSuccess { count ->
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importMessage = "已写入本地曲库 · $count 首"
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        error = throwable.message ?: "导入失败，请重试"
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

    // 清空本次扫描结果（不删除已写入曲库的记录）
    fun clearResults() {
        cancelRequested = true
        scanJob?.cancel()
        scanJob = null
        importJob?.cancel()
        importJob = null
        currentTreeUri = null
        _uiState.value = ImportLocalUiState()
    }

    // 用户拒绝音频读取权限，无法全盘扫描
    fun onAudioPermissionDenied() {
        _uiState.update { it.copy(error = "未授予音频读取权限，无法扫描全部歌曲") }
    }

    // 启动一次扫描任务：重置状态后执行 scanBlock，异常与取消统一收口
    private fun launchScan(
        folderLabel: String,
        treeUri: Uri,
        scanBlock: suspend () -> Unit
    ) {
        cancelRequested = true
        scanJob?.cancel()
        importJob?.cancel()
        cancelRequested = false
        currentTreeUri = treeUri
        scanJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    folderName = folderLabel,
                    isScanning = true,
                    isImporting = false,
                    scannedCount = 0,
                    tracks = emptyList(),
                    error = null,
                    importMessage = null
                )
            }
            runCatching { scanBlock() }
                .onSuccess { _uiState.update { it.copy(isScanning = false, error = null) } }
                .onFailure { throwable ->
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

    private fun appendScannedTracks(track: ScannedLocalTrack) {
        appendScannedTracks(listOf(track))
    }

    private fun appendScannedTracks(newTracks: List<ScannedLocalTrack>) {
        _uiState.update { state ->
            if (!state.isScanning && cancelRequested) state
            else state.copy(
                tracks = state.tracks + newTracks,
                scannedCount = state.scannedCount + newTracks.size
            )
        }
    }

    companion object {
        // 全盘扫描预览刷新批大小
        private const val FlushBatchSize = 64
    }
}

private fun ScannedLocalTrack.toDraft(treeUri: String) = LocalTrackImportDraft(
    uri = uri,
    displayName = displayName,
    title = title,
    artists = artists,
    album = album,
    durationMs = durationMs,
    fileSizeBytes = fileSizeBytes,
    treeUri = treeUri
)
