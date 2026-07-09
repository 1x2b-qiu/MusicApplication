package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.model.SongUrl
import com.example.musicapp.domain.repository.MusicRepository
import javax.inject.Inject

class GetSongUrlUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(songId: Long): SongUrl {
        return musicRepository.getSongUrl(songId)
    }
}
