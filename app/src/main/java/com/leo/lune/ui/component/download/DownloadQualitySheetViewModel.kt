package com.leo.lune.ui.component.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.SettingKeys
import com.leo.lune.domain.repository.DownloadRepository
import com.leo.lune.domain.repository.SettingsRepository
import com.leo.lune.domain.usecase.download.GetDownloadQualitySizesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 下载音质弹窗 UI 状态
data class DownloadQualitySheetUiState(
    // 当前状态所属歌曲；与弹层 song.id 不一致时 UI 应忽略已下载集合
    val songId: Long = 0L,
    val sizeByQuality: Map<DownloadQuality, Long> = emptyMap(),
    // 该曲已落盘的音质档位
    val downloadedQualities: Set<DownloadQuality> = emptySet(),
    // 设置页配置的默认音质（打开弹层时预选）
    val defaultQuality: DownloadQuality = DownloadQuality.Default,
    // 已成功拉取 size 的歌；同 id 再次打开复用
    val loadedSongId: Long? = null
)

// 下载音质弹窗：按歌拉取各档真实体积，观察已下载档位与默认音质
@HiltViewModel
class DownloadQualitySheetViewModel @Inject constructor(
    private val getDownloadQualitySizesUseCase: GetDownloadQualitySizesUseCase,
    private val downloadRepository: DownloadRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadQualitySheetUiState())

    // 默认音质来自设置表，与本地 UIState 合并对外暴露
    val uiState: StateFlow<DownloadQualitySheetUiState> = combine(
        _uiState,
        settingsRepository.observeValue(SettingKeys.DOWNLOAD_DEFAULT_QUALITY).map { raw ->
            val bitrate = raw?.toIntOrNull() ?: return@map DownloadQuality.Default
            DownloadQuality.fromBitrate(bitrate)
        }
    ) { state, defaultQuality ->
        state.copy(defaultQuality = defaultQuality)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DownloadQualitySheetUiState()
    )

    private var loadJob: Job? = null
    private var observeJob: Job? = null

    // 打开弹窗或切歌时调用；订阅已下载档位，并按需拉取各音质 size
    fun load(songId: Long) {
        observeJob?.cancel()
        loadJob?.cancel()
        // 立刻绑定到新歌并清空上一首状态，避免 stateIn 回放旧值导致按钮误灰
        _uiState.update {
            it.copy(
                songId = songId,
                downloadedQualities = emptySet(),
                sizeByQuality = emptyMap(),
                loadedSongId = null
            )
        }
        observeJob = viewModelScope.launch {
            downloadRepository.observeDownloadedQualities(songId).collect { qualities ->
                _uiState.update { state ->
                    // 过期协程回调：已切到别的歌则丢弃
                    if (state.songId != songId) state
                    else state.copy(downloadedQualities = qualities)
                }
            }
        }
        loadJob = viewModelScope.launch {
            val sizes = runCatching { getDownloadQualitySizesUseCase(songId) }
                .getOrDefault(emptyMap())
            _uiState.update { state ->
                if (state.songId != songId) state
                else state.copy(
                    sizeByQuality = sizes,
                    loadedSongId = songId.takeIf { sizes.isNotEmpty() }
                )
            }
        }
    }
}
