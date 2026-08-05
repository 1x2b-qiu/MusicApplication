package com.leo.lune.ui.playlistplaza

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// 歌单广场分类 Chip
data class PlaylistPlazaCategory(
    val id: String,
    val name: String
)

// 歌单广场网格条目（UI 暂用假数据）
data class PlaylistPlazaItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val coverUrl: String = ""
)

data class PlaylistPlazaUiState(
    val categories: List<PlaylistPlazaCategory> = emptyList(),
    val selectedCategoryId: String = RecommendCategoryId,
    val playlists: List<PlaylistPlazaItem> = emptyList()
)

// 歌单广场：先搭 UI，分类与歌单均为本地假数据
@HiltViewModel
class PlaylistPlazaViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(sampleUiState())
    val uiState: StateFlow<PlaylistPlazaUiState> = _uiState.asStateFlow()

    fun onCategorySelect(categoryId: String) {
        if (categoryId == _uiState.value.selectedCategoryId) return
        _uiState.update {
            it.copy(
                selectedCategoryId = categoryId,
                playlists = mockPlaylistsFor(categoryId)
            )
        }
    }

    fun onPlaylistClick(playlistId: Long) = Unit

    fun onPlaylistPlayClick(playlistId: Long) = Unit
}

const val RecommendCategoryId = "recommend"

private fun sampleUiState(): PlaylistPlazaUiState = PlaylistPlazaUiState(
    categories = mockCategories(),
    selectedCategoryId = RecommendCategoryId,
    playlists = mockPlaylistsFor(RecommendCategoryId)
)

private fun mockCategories(): List<PlaylistPlazaCategory> = listOf(
    PlaylistPlazaCategory(RecommendCategoryId, "推荐"),
    PlaylistPlazaCategory("chinese", "华语"),
    PlaylistPlazaCategory("pop", "流行"),
    PlaylistPlazaCategory("rock", "摇滚"),
    PlaylistPlazaCategory("folk", "民谣"),
    PlaylistPlazaCategory("electronic", "电子"),
    PlaylistPlazaCategory("light", "轻音乐"),
    PlaylistPlazaCategory("rnb", "R&B"),
    PlaylistPlazaCategory("jazz", "爵士"),
    PlaylistPlazaCategory("classical", "古典")
)

private fun mockPlaylistsFor(categoryId: String): List<PlaylistPlazaItem> {
    val categoryName = mockCategories().firstOrNull { it.id == categoryId }?.name ?: "歌单"
    return (1..12).map { index ->
        PlaylistPlazaItem(
            id = categoryId.hashCode().toLong() * 100 + index,
            title = "$categoryName 精选 · Vol.$index",
            subtitle = "${80 + index * 17} 首"
        )
    }
}
