package com.leo.lune.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.domain.repository.CacheRepository
import com.leo.lune.util.formatFileSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    // 可清理缓存字节数
    val cacheSizeBytes: Long = 0L,
    // 是否正在清理，防连点
    val isClearingCache: Boolean = false
) {
    // 设置行 / 弹窗共用的体积文案
    val cacheSizeLabel: String get() = formatFileSize(cacheSizeBytes)
}

// 设置页：缓存体积统计与清理
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val cacheRepository: CacheRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshCacheSize()
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val bytes = runCatching { cacheRepository.getCacheSizeBytes() }.getOrDefault(0L)
            _uiState.update { it.copy(cacheSizeBytes = bytes) }
        }
    }

    // 清理缓存后刷新体积；onComplete 用于关弹窗
    fun clearCache(onComplete: () -> Unit = {}) {
        if (_uiState.value.isClearingCache) return
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingCache = true) }
            runCatching { cacheRepository.clearCache() }
            val bytes = runCatching { cacheRepository.getCacheSizeBytes() }.getOrDefault(0L)
            _uiState.update {
                it.copy(cacheSizeBytes = bytes, isClearingCache = false)
            }
            onComplete()
        }
    }
}
