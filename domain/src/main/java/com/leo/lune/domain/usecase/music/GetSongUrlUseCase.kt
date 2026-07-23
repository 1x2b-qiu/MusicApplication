package com.leo.lune.domain.usecase.music

import com.leo.lune.domain.model.SongUrl
import com.leo.lune.domain.repository.MusicRepository
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
