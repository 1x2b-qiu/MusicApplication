package com.example.musicapp.domain.usecase.music

import com.example.musicapp.domain.model.SongUrl
import com.example.musicapp.domain.repository.MusicRepository
import javax.inject.Inject

// 获取歌曲流媒体播放地址
class GetSongUrlUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    // bitrate：可选目标码率（bps）；不传则由服务端默认
    suspend operator fun invoke(songId: Long, bitrate: Int? = null): SongUrl {
        return musicRepository.getSongUrl(songId, bitrate)
    }
}
