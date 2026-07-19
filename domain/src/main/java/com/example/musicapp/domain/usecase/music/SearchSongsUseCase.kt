package com.example.musicapp.domain.usecase.music

import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.repository.MusicRepository
import javax.inject.Inject

// 按关键词搜索歌曲，空关键词直接返回空列表
class SearchSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    // 执行搜索，空关键词直接返回空列表
    suspend operator fun invoke(keywords: String, limit: Int = 20): List<Song> {
        val trimmed = keywords.trim()
        if (trimmed.isEmpty()) return emptyList()
        return musicRepository.searchSongs(trimmed, limit)
    }
}
