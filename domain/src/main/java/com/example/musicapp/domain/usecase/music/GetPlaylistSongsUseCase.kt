package com.example.musicapp.domain.usecase.music

import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.repository.MusicRepository
import javax.inject.Inject

// 获取指定歌单内的歌曲列表
class GetPlaylistSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    // 分页获取歌单内歌曲，limit 为 null 时返回全部
    suspend operator fun invoke(
        playlistId: Long,
        limit: Int? = null,
        offset: Int = 0
    ): List<Song> {
        return musicRepository.getPlaylistSongs(playlistId, limit, offset)
    }
}
