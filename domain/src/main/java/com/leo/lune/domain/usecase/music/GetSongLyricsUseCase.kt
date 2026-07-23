package com.leo.lune.domain.usecase.music

import com.leo.lune.domain.model.LyricLine
import com.leo.lune.domain.repository.MusicRepository
import javax.inject.Inject

// 获取指定歌曲的 LRC 歌词
class GetSongLyricsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    // 获取指定歌曲的 LRC 歌词行列表
    suspend operator fun invoke(songId: Long): List<LyricLine> {
        return musicRepository.getSongLyrics(songId)
    }
}
