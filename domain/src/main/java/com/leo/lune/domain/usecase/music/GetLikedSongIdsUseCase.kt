package com.leo.lune.domain.usecase.music

import com.leo.lune.domain.repository.MusicRepository
import javax.inject.Inject

// 获取用户收藏的全部歌曲 ID
class GetLikedSongIdsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    // 获取指定用户收藏的全部歌曲 ID
    suspend operator fun invoke(userId: Long): List<Long> {
        return musicRepository.getLikedSongIds(userId)
    }
}
