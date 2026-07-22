package com.example.musicapp.ui.component.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.model.DownloadQuality
import com.example.musicapp.domain.usecase.download.GetDownloadQualitySizesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 下载音质弹窗：按歌拉取各档真实体积
@HiltViewModel
class DownloadQualitySheetViewModel @Inject constructor(
    private val getDownloadQualitySizesUseCase: GetDownloadQualitySizesUseCase
) : ViewModel() {

    private val _sizeByQuality = MutableStateFlow<Map<DownloadQuality, Long>>(emptyMap())
    val sizeByQuality: StateFlow<Map<DownloadQuality, Long>> = _sizeByQuality.asStateFlow()

    private var loadJob: Job? = null

    // 打开弹窗或切歌时调用；并行请求各音质 size
    fun loadSizes(songId: Long) {
        loadJob?.cancel()
        _sizeByQuality.value = emptyMap()
        loadJob = viewModelScope.launch {
            val sizes = runCatching { getDownloadQualitySizesUseCase(songId) }
                .getOrDefault(emptyMap())
            _sizeByQuality.value = sizes
        }
    }
}
