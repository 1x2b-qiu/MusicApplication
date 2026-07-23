package com.leo.lune.domain.usecase.music

import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.MusicRepository
import javax.inject.Inject

// 批量获取歌曲详情
class GetSongDetailsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    // 批量查询歌曲详情，顺序与传入 ID 一致
    suspend operator fun invoke(songIds: List<Long>): List<Song> {
        return musicRepository.getSongDetails(songIds)
    }
}
