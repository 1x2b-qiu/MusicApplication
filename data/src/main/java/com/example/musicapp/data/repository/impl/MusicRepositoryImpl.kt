package com.example.musicapp.data.repository.impl

import com.example.musicapp.data.mapper.toLikeSongResult
import com.example.musicapp.data.mapper.toSong
import com.example.musicapp.data.mapper.toSongUrl
import com.example.musicapp.data.mapper.toUserPlaylist
import com.example.musicapp.data.remote.api.NeteaseApi
import com.example.musicapp.data.util.LrcParser
import com.example.musicapp.domain.model.LikeSongResult
import com.example.musicapp.domain.model.LyricLine
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.SongUrl
import com.example.musicapp.domain.model.UserPlaylist
import com.example.musicapp.domain.repository.MusicRepository
import javax.inject.Inject
import javax.inject.Singleton

// 音乐数据仓储实现
// 封装网易云 API 调用，将 DTO 映射为领域模型
@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val neteaseApi: NeteaseApi
) : MusicRepository {

    // 按关键词搜索歌曲
    override suspend fun searchSongs(keywords: String, limit: Int): List<Song> {
        val response = neteaseApi.search(keywords, limit)
        if (response.code != 200) {
            throw IllegalStateException("Search failed with code ${response.code}")
        }
        return response.result?.songs.orEmpty().map { it.toSong() }
    }

    // 获取歌曲可播放的音频 URL；bitrate 指定目标码率时按该档返回
    override suspend fun getSongUrl(songId: Long, bitrate: Int?): SongUrl {
        val response = neteaseApi.getSongUrl(songId = songId, bitrate = bitrate)
        if (response.code != 200) {
            throw IllegalStateException("Get song url failed with code ${response.code}")
        }
        val item = response.data?.firstOrNull()
            ?: throw IllegalStateException("No playable url for song $songId")
        return item.toSongUrl()
    }

    // 获取歌曲 LRC 歌词并解析为按时间排序的歌词行
    override suspend fun getSongLyrics(songId: Long): List<LyricLine> {
        val response = neteaseApi.getLyric(songId)
        val lrcText = response.lrc?.lyric
        if (lrcText.isNullOrBlank()) return emptyList()
        return LrcParser.parse(lrcText)
    }

    // 收藏或取消收藏歌曲
    override suspend fun likeSong(songId: Long, like: Boolean): LikeSongResult {
        val response = neteaseApi.likeSong(songId = songId, like = like)
        return response.code.toLikeSongResult()
    }

    // 获取用户红心歌单中的歌曲 ID 列表
    override suspend fun getLikedSongIds(userId: Long): List<Long> {
        val response = neteaseApi.getLikelist(userId = userId)
        if (response.code != 200) {
            throw IllegalStateException("Get likelist failed with code ${response.code}")
        }
        return response.ids.orEmpty()
    }

    // 批量获取歌曲详情
    override suspend fun getSongDetails(songIds: List<Long>): List<Song> {
        if (songIds.isEmpty()) return emptyList()
        val response = neteaseApi.getSongDetail(songIds = songIds.joinToString(","))
        if (response.code != 200) {
            throw IllegalStateException("Get song detail failed with code ${response.code}")
        }
        return response.songs.orEmpty().map { it.toSong() }
    }

    // 获取用户创建的歌单列表
    override suspend fun getUserPlaylists(
        userId: Long,
        limit: Int,
        offset: Int
    ): List<UserPlaylist> {
        val response = neteaseApi.getUserPlaylists(
            userId = userId,
            limit = limit,
            offset = offset
        )
        if (response.code != 200) {
            throw IllegalStateException("Get user playlists failed with code ${response.code}")
        }
        return response.playlist.orEmpty().map { it.toUserPlaylist() }
    }

    // 获取歌单内全部歌曲
    override suspend fun getPlaylistSongs(
        playlistId: Long,
        limit: Int?,
        offset: Int
    ): List<Song> {
        val response = neteaseApi.getPlaylistTrackAll(
            playlistId = playlistId,
            limit = limit,
            offset = offset
        )
        if (response.code != 200) {
            throw IllegalStateException("Get playlist songs failed with code ${response.code}")
        }
        return response.songs.orEmpty().map { it.toSong() }
    }
}
