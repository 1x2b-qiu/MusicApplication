package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.repository.MusicRepository
import javax.inject.Inject

class SearchSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(keywords: String, limit: Int = 20): List<Song> {
        val trimmed = keywords.trim()
        if (trimmed.isEmpty()) return emptyList()
        return musicRepository.searchSongs(trimmed, limit)
    }
}
