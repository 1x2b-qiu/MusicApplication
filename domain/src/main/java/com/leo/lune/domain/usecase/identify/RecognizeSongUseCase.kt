package com.leo.lune.domain.usecase.identify

import com.leo.lune.domain.model.IdentifyMatchResult
import com.leo.lune.domain.repository.IdentifyRepository
import com.leo.lune.domain.repository.MusicRepository
import javax.inject.Inject

// 听歌识曲：AudD 识别 → 用「歌名 歌手」在网易云搜索候选
class RecognizeSongUseCase @Inject constructor(
    private val identifyRepository: IdentifyRepository,
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke(
        audioPath: String,
        searchLimit: Int = 50,
    ): IdentifyMatchResult {
        val track = identifyRepository.recognize(audioPath)
            ?: return IdentifyMatchResult.NotRecognized

        val keywords = listOf(track.title, track.artist)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")

        val songs = if (keywords.isEmpty()) {
            emptyList()
        } else {
            musicRepository.searchSongs(keywords, searchLimit)
        }

        return IdentifyMatchResult.Matched(track = track, songs = songs)
    }
}
