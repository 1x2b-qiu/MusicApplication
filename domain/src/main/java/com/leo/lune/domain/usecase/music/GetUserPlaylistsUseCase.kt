package com.leo.lune.domain.usecase.music

import com.leo.lune.domain.model.UserPlaylist
import com.leo.lune.domain.repository.MusicRepository
import javax.inject.Inject

// 获取指定用户的歌单列表
class GetUserPlaylistsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    // 分页获取用户歌单列表
    suspend operator fun invoke(
        userId: Long,
        limit: Int = 30,
        offset: Int = 0
    ): List<UserPlaylist> {
        return musicRepository.getUserPlaylists(userId, limit, offset)
    }
}
