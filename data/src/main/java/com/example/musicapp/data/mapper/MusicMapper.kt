package com.example.musicapp.data.mapper

import com.example.musicapp.data.remote.response.PlaylistDto
import com.example.musicapp.data.remote.response.SongDto
import com.example.musicapp.data.remote.response.SongUrlDto
import com.example.musicapp.domain.model.LikeSongResult
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.SongUrl
import com.example.musicapp.domain.model.UserPlaylist

// 网易云 API DTO → 领域模型映射

// 将歌曲 DTO 转为领域 Song，兼容 artists/ar、album/al 等字段差异
fun SongDto.toSong(): Song {
    val albumInfo = album ?: al
    val artistList = artists ?: ar
    return Song(
        id = id,
        name = name,
        artists = artistList?.joinToString(" / ") { it.name }.orEmpty(),
        album = albumInfo?.name.orEmpty(),
        coverUrl = normalizeCoverUrl(albumInfo?.picUrl),
        durationMs = duration.takeIf { it > 0 } ?: dt
    )
}

// 补全协议相对路径的封面 URL（//xxx → https://xxx）
private fun normalizeCoverUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return when {
        url.startsWith("//") -> "https:$url"
        else -> url
    }
}

// 播放地址 DTO → 领域模型
fun SongUrlDto.toSongUrl(): SongUrl {
    return SongUrl(
        songId = id,
        url = url,
        bitrate = br
    )
}

// 歌单 DTO → 领域模型
fun PlaylistDto.toUserPlaylist(): UserPlaylist {
    return UserPlaylist(
        id = id,
        name = name,
        trackCount = trackCount,
        coverUrl = coverImgUrl,
        specialType = specialType
    )
}

// 收藏接口状态码 → 领域结果
fun Int.toLikeSongResult(): LikeSongResult {
    return LikeSongResult(
        success = this == 200,
        code = this
    )
}
