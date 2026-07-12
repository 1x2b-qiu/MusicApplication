package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.repository.MusicRepository
import javax.inject.Inject

// 获取「我喜欢的音乐」歌单中的全部歌曲
class GetLikedMusicPlaylistSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    // 定位「我喜欢的音乐」歌单并返回其中全部歌曲
    suspend operator fun invoke(userId: Long): List<Song> {
        // 从用户歌单中定位「我喜欢的音乐」特殊歌单
        val likedPlaylist = musicRepository.getUserPlaylists(userId)
            .firstOrNull { it.isLikedMusicPlaylist }
            ?: throw IllegalStateException("Liked music playlist not found for user $userId")
        return musicRepository.getPlaylistSongs(likedPlaylist.id)
    }
}
