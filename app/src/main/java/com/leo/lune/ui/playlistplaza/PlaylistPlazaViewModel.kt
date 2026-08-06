package com.leo.lune.ui.playlistplaza

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.domain.model.PersonalizedPlaylist
import com.leo.lune.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 「推荐」Tab 的固定 id，与 playlist/hot 返回的数字 id 区分
const val RecommendCategoryId = "recommend"

// 歌单广场分类 Tab（推荐为本地项，其余来自 playlist/hot tags）
data class PlaylistPlazaCategory(
    // recommend 或热门标签 id 字符串
    val id: String,
    // Tab 展示名；非推荐项同时作为 top/playlist 的 cat 参数
    val name: String
)

// 歌单广场网格条目
data class PlaylistPlazaItem(
    val id: Long,
    val title: String,
    // 推荐文案 / 播放量 / 首数，见 plazaSubtitle
    val subtitle: String,
    val coverUrl: String = ""
)

// 歌单广场页 UI 状态
data class PlaylistPlazaUiState(
    // 首项固定为「推荐」，其后为热门分类标签
    val categories: List<PlaylistPlazaCategory> = listOf(
    ),
    // 当前选中的分类 id
    val selectedCategoryId: String = RecommendCategoryId,
    // 当前分类下的歌单网格数据
    val playlists: List<PlaylistPlazaItem> = emptyList(),
    // 是否正在拉取分类或歌单
    val isLoading: Boolean = false,
    // 加载失败时的错误信息；有列表数据时可不展示
    val error: String? = null
)

// 歌单广场 ViewModel
// 推荐 → getPersonalizedPlaylists；分类 Tab → getHotPlaylistCategories；
// 某分类歌单 → getTopPlaylists(cat = 分类名)
@HiltViewModel
class PlaylistPlazaViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistPlazaUiState(isLoading = true))
    // 对外只读，PlaylistPlazaScreen 通过 collect 订阅
    val uiState: StateFlow<PlaylistPlazaUiState> = _uiState.asStateFlow()

    // 当前进行中的加载任务；切 Tab / 重试时取消旧任务，避免乱序回写
    private var loadPlaylistsJob: Job? = null

    init {
        loadInitial()
    }

    // 切换分类 Tab：先清空列表再拉取，避免短暂展示上一分类数据
    fun onCategorySelect(categoryId: String) {
        if (categoryId == _uiState.value.selectedCategoryId) return
        _uiState.update {
            it.copy(
                selectedCategoryId = categoryId,
                playlists = emptyList(),
                error = null
            )
        }
        loadPlaylists(categoryId)
    }

    // 加载失败后由 UI 触发重试
    // 仅有默认「推荐」项时说明分类也未拉到，走完整进页加载；否则只重拉当前分类歌单
    fun onRetry() {
        val state = _uiState.value
        if (state.categories.size <= 1) {
            loadInitial()
        } else {
            loadPlaylists(state.selectedCategoryId)
        }
    }

    // 进入歌单详情（暂留空）
    fun onPlaylistClick(playlistId: Long) = Unit

    // 播放歌单（暂留空）
    fun onPlaylistPlayClick(playlistId: Long) = Unit

    // 进页：并行拉分类标签 + 推荐歌单
    // 一侧失败仍尽量展示另一侧；两侧都失败才整页错误
    private fun loadInitial() {
        loadPlaylistsJob?.cancel()
        loadPlaylistsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            coroutineScope {
                val categoriesDeferred = async {
                    runCatching { musicRepository.getHotPlaylistCategories() }
                }
                val playlistsDeferred = async {
                    runCatching {
                        musicRepository.getPersonalizedPlaylists(limit = RecommendPlaylistLimit)
                    }
                }
                val tagsResult = categoriesDeferred.await()
                val playlistsResult = playlistsDeferred.await()

                // 分类与推荐均失败：无法展示有效内容
                if (tagsResult.isFailure && playlistsResult.isFailure) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = playlistsResult.exceptionOrNull()?.message
                                ?: tagsResult.exceptionOrNull()?.message
                                ?: "加载失败，请稍后重试"
                        )
                    }
                    return@coroutineScope
                }

                // Tab 列表：本地「推荐」+ 热门标签（标签失败时至少保留推荐）
                val categories = buildList {
                    add(PlaylistPlazaCategory(RecommendCategoryId, "推荐"))
                    tagsResult.getOrDefault(emptyList()).forEach { tag ->
                        add(PlaylistPlazaCategory(id = tag.id.toString(), name = tag.name))
                    }
                }
                val playlists = playlistsResult.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        categories = categories,
                        selectedCategoryId = RecommendCategoryId,
                        playlists = playlists.map { playlist -> playlist.toPlazaItem() },
                        isLoading = false,
                        // 推荐失败且列表为空时带上错误，便于空态展示重试
                        error = playlistsResult.exceptionOrNull()?.message
                            ?.takeIf { playlists.isEmpty() }
                    )
                }
            }
        }
    }

    // 按当前分类拉取歌单：推荐走 personalized，其它用分类名调 top/playlist
    private fun loadPlaylists(categoryId: String) {
        loadPlaylistsJob?.cancel()
        loadPlaylistsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                if (categoryId == RecommendCategoryId) {
                    musicRepository.getPersonalizedPlaylists(limit = RecommendPlaylistLimit)
                } else {
                    // top/playlist 的 cat 使用展示名（如「华语」），不是数字 id
                    val catName = _uiState.value.categories
                        .firstOrNull { it.id == categoryId }
                        ?.name
                        ?: error("未知分类")
                    musicRepository.getTopPlaylists(cat = catName, limit = CategoryPlaylistLimit)
                }
            }.onSuccess { playlists ->
                _uiState.update {
                    it.copy(
                        playlists = playlists.map { playlist -> playlist.toPlazaItem() },
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        playlists = emptyList(),
                        error = throwable.message ?: "加载失败，请稍后重试"
                    )
                }
            }
        }
    }

    companion object {
        // 推荐 Tab 拉取条数
        private const val RecommendPlaylistLimit = 50
        // 风格分类下歌单拉取条数
        private const val CategoryPlaylistLimit = 50
    }
}

// 领域歌单 → 广场网格 UI 模型
private fun PersonalizedPlaylist.toPlazaItem(): PlaylistPlazaItem = PlaylistPlazaItem(
    id = id,
    title = name,
    subtitle = plazaSubtitle(copywriter, trackCount, playCount),
    coverUrl = coverUrl.orEmpty()
)

// 副标题：优先推荐文案，其次播放量，再次首数
private fun plazaSubtitle(
    copywriter: String?,
    trackCount: Int,
    playCount: Long
): String {
    copywriter?.takeIf { it.isNotBlank() }?.let { return it }
    if (playCount > 0L) return formatPlayCount(playCount)
    if (trackCount > 0) return "$trackCount 首"
    return ""
}

// 播放量展示：亿 / 万 / 原次数
private fun formatPlayCount(count: Long): String = when {
    count >= 100_000_000L -> String.format("%.1f亿次", count / 100_000_000.0)
    count >= 10_000L -> String.format("%.1f万次", count / 10_000.0)
    else -> "${count}次"
}
