package com.leo.lune.domain.usecase.music

import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.MusicRepository
import javax.inject.Inject

// 获取「我喜欢的音乐」歌单中的歌曲；首页轮播只需前若干首
class GetLikedMusicPlaylistSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(userId: Long, limit: Int? = null): List<Song> {
        val likedPlaylist = musicRepository.getUserPlaylists(userId)
            .firstOrNull { it.isLikedMusicPlaylist }
            ?: throw IllegalStateException("Liked music playlist not found for user $userId")
        return musicRepository.getPlaylistSongs(likedPlaylist.id, limit = limit)
    }
}
