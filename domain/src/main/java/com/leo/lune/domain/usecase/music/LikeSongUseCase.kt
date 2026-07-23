package com.leo.lune.domain.usecase.music

import com.leo.lune.domain.model.LikeSongResult
import com.leo.lune.domain.repository.MusicRepository
import javax.inject.Inject

// 收藏或取消收藏歌曲
class LikeSongUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    // like=true 收藏，false 取消收藏
    suspend operator fun invoke(songId: Long, like: Boolean = true): LikeSongResult {
        return musicRepository.likeSong(songId, like)
    }
}
