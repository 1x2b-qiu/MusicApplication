package com.leo.lune.ui.component.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.usecase.download.GetDownloadQualitySizesUseCase
import com.leo.lune.domain.usecase.download.ObserveDownloadedQualitiesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 下载音质弹窗 UI 状态
data class DownloadQualitySheetUiState(
    val sizeByQuality: Map<DownloadQuality, Long> = emptyMap(),
    // 该曲已落盘的音质档位
    val downloadedQualities: Set<DownloadQuality> = emptySet(),
    // 已成功拉取 size 的歌；同 id 再次打开复用
    val loadedSongId: Long? = null
)

// 下载音质弹窗：按歌拉取各档真实体积，并观察已下载档位
@HiltViewModel
class DownloadQualitySheetViewModel @Inject constructor(
    private val getDownloadQualitySizesUseCase: GetDownloadQualitySizesUseCase,
    private val observeDownloadedQualitiesUseCase: ObserveDownloadedQualitiesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadQualitySheetUiState())
    val uiState: StateFlow<DownloadQualitySheetUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var observeJob: Job? = null

    // 打开弹窗或切歌时调用；订阅已下载档位，并按需拉取各音质 size
    fun load(songId: Long) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            observeDownloadedQualitiesUseCase(songId).collect { qualities ->
                _uiState.update { it.copy(downloadedQualities = qualities) }
            }
        }

        val current = _uiState.value
        if (songId == current.loadedSongId && current.sizeByQuality.isNotEmpty()) return
        loadJob?.cancel()
        _uiState.update {
            it.copy(sizeByQuality = emptyMap(), loadedSongId = null)
        }
        loadJob = viewModelScope.launch {
            val sizes = runCatching { getDownloadQualitySizesUseCase(songId) }
                .getOrDefault(emptyMap())
            _uiState.update {
                it.copy(
                    sizeByQuality = sizes,
                    loadedSongId = songId.takeIf { sizes.isNotEmpty() }
                )
            }
        }
    }
}
