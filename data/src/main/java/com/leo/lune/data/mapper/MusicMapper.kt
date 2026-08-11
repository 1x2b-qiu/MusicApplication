package com.leo.lune.data.mapper

import com.leo.lune.data.remote.response.PersonalizedPlaylistDto
import com.leo.lune.data.remote.response.PlaylistDetailDto
import com.leo.lune.data.remote.response.PlaylistDto
import com.leo.lune.data.remote.response.PlaylistHotTagDto
import com.leo.lune.data.remote.response.SongDto
import com.leo.lune.data.remote.response.SongUrlDto
import com.leo.lune.data.remote.response.TopPlaylistDto
import com.leo.lune.domain.model.LikeSongResult
import com.leo.lune.domain.model.PersonalizedPlaylist
import com.leo.lune.domain.model.PlaylistCategory
import com.leo.lune.domain.model.PlaylistDetail
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.model.SongUrl
import com.leo.lune.domain.model.SubscribePlaylistResult
import com.leo.lune.domain.model.UserPlaylist

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

// 补全协议相对路径的封面 URL（//xxx → https://xxx；yyimgs/ → CDN）
internal fun normalizeCoverUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return when {
        url.startsWith("//") -> "https:$url"
        // 曲风列表子标签常用相对路径 yyimgs/{hash}/{id}
        url.startsWith("yyimgs/") ->
            "https://p1.music.126.net/${url.removePrefix("yyimgs/")}"
        else -> url
    }
}

// 播放地址 DTO → 领域模型
fun SongUrlDto.toSongUrl(): SongUrl {
    return SongUrl(
        songId = id,
        url = url,
        bitrate = br,
        sizeBytes = size
    )
}

// 歌单 DTO → 领域模型
fun PlaylistDto.toUserPlaylist(): UserPlaylist {
    return UserPlaylist(
        id = id,
        name = name,
        trackCount = trackCount,
        coverUrl = coverImgUrl,
        specialType = specialType,
        subscribed = subscribed
    )
}

// 歌单详情 DTO → 领域模型
fun PlaylistDetailDto.toPlaylistDetail(): PlaylistDetail {
    return PlaylistDetail(
        id = id,
        name = name.orEmpty(),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        coverUrl = normalizeCoverUrl(coverImgUrl),
        trackCount = trackCount ?: 0,
        tags = tags.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
        creatorName = creator?.nickname?.trim()?.takeIf { it.isNotEmpty() },
        creatorAvatarUrl = normalizeCoverUrl(creator?.avatarUrl),
        subscribed = subscribed == true
    )
}

// 推荐歌单 DTO → 领域模型
fun PersonalizedPlaylistDto.toPersonalizedPlaylist(): PersonalizedPlaylist? {
    val playlistId = id ?: return null
    return PersonalizedPlaylist(
        id = playlistId,
        name = name.orEmpty(),
        copywriter = copywriter?.takeIf { it.isNotBlank() },
        coverUrl = normalizeCoverUrl(picUrl),
        trackCount = trackCount ?: 0,
        playCount = playCount?.toLong() ?: 0L
    )
}

// 热门歌单分类标签 DTO → 领域模型（不含封面）
fun PlaylistHotTagDto.toPlaylistCategory(): PlaylistCategory? {
    val tagId = id ?: return null
    val tagName = name?.takeIf { it.isNotBlank() } ?: return null
    return PlaylistCategory(id = tagId, name = tagName)
}

// 分类歌单 DTO → 领域模型（与推荐歌单共用 PersonalizedPlaylist）
fun TopPlaylistDto.toPersonalizedPlaylist(): PersonalizedPlaylist? {
    val playlistId = id ?: return null
    // 去掉 imageView/watermark 参数，避免封面上叠分类字样
    val cleanCover = coverImgUrl?.substringBefore('?')
    return PersonalizedPlaylist(
        id = playlistId,
        name = name.orEmpty(),
        copywriter = null,
        coverUrl = normalizeCoverUrl(cleanCover),
        trackCount = trackCount ?: 0,
        playCount = playCount?.toLong() ?: 0L
    )
}

// 收藏接口状态码 → 领域结果
fun Int.toLikeSongResult(): LikeSongResult {
    return LikeSongResult(
        success = this == 200,
        code = this
    )
}

// 歌单收藏接口状态码 → 领域结果
fun Int.toSubscribePlaylistResult(): SubscribePlaylistResult {
    return SubscribePlaylistResult(
        success = this == 200,
        code = this
    )
}
