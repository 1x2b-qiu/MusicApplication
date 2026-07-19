package com.example.musicapp.domain.usecase.music

import com.example.musicapp.domain.model.SongUrl
import com.example.musicapp.domain.repository.MusicRepository
import javax.inject.Inject

// 获取歌曲流媒体播放地址
class GetSongUrlUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    // 获取指定歌曲的流媒体播放地址
    suspend operator fun invoke(songId: Long): SongUrl {
        return musicRepository.getSongUrl(songId)
    }
}
