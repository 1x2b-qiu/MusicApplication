package com.leo.lune.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChartsSongPreview(
    val id: Long,
    val title: String,
    val artist: String,
    val coverUrl: String
)

data class ChartsHubItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val songs: List<ChartsSongPreview>
)

data class ChartsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val charts: List<ChartsHubItem> = emptyList()
)

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartsUiState())
    val uiState: StateFlow<ChartsUiState> = _uiState.asStateFlow()

    init {
        loadCharts()
    }

    fun onRetry() {
        loadCharts()
    }

    private fun loadCharts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, charts = emptyList()) }
            FixedCharts.map { spec ->
                async {
                    val songs = runCatching {
                        musicRepository.getPlaylistSongs(spec.id, limit = 3)
                    }.getOrElse { emptyList() }
                    val item = ChartsHubItem(
                        id = spec.id,
                        title = spec.title,
                        subtitle = spec.subtitle,
                        songs = songs.map { it.toPreview() }
                    )
                    // 谁先回来谁先展示，列表仍按 FixedCharts 顺序排列
                    _uiState.update { current ->
                        val byId = current.charts.associateBy { it.id } + (item.id to item)
                        current.copy(
                            charts = FixedCharts.mapNotNull { chart -> byId[chart.id] }
                        )
                    }
                }
            }.awaitAll()

            _uiState.update { current ->
                val allFailed = current.charts.isEmpty() ||
                    current.charts.all { it.songs.isEmpty() }
                current.copy(
                    isLoading = false,
                    error = "加载失败，请重试".takeIf { allFailed }
                )
            }
        }
    }
}

private fun Song.toPreview(): ChartsSongPreview = ChartsSongPreview(
    id = id,
    title = name,
    artist = artists,
    coverUrl = coverUrl.orEmpty()
)

// 与曲库预览同源的官方榜单
internal data class ChartSpec(
    val id: Long,
    val title: String,
    val subtitle: String
)

internal val FixedCharts = listOf(
    ChartSpec(3778678, "热歌榜", "实时热门"),
    ChartSpec(3779629, "新歌榜", "每周更新"),
    ChartSpec(6723173524, "网络热歌榜", "全网爆款"),
    ChartSpec(6688069460, "听歌识曲榜", "听歌识曲热榜"),
    ChartSpec(19723756, "飙升榜", "每日更新"),
    ChartSpec(2884035, "原创榜", "每周更新"),
    ChartSpec(991319590, "说唱榜", "中文说唱"),
    ChartSpec(1978921795, "电音榜", "电子音乐"),
    ChartSpec(71384707, "古典榜", "古典精选"),
    ChartSpec(71385702, "ACG榜", "二次元热歌"),
    ChartSpec(745956260, "韩语榜", "韩语热歌"),
    ChartSpec(505966151, "日语榜", "日语热歌")
)
