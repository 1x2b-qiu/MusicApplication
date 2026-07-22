package com.example.musicapp.domain.repository

import com.example.musicapp.domain.model.LikeSongResult
import com.example.musicapp.domain.model.LyricLine
import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.model.SongUrl
import com.example.musicapp.domain.model.UserPlaylist

// 音乐数据仓储接口，封装搜索、播放、收藏等远端操作
interface MusicRepository {
    // 按关键词搜索歌曲
    suspend fun searchSongs(keywords: String, limit: Int = 20): List<Song>
    // 获取歌曲流媒体播放地址；bitrate 为可选目标码率（bps）
    suspend fun getSongUrl(songId: Long, bitrate: Int? = null): SongUrl
    // 获取歌曲 LRC 歌词
    suspend fun getSongLyrics(songId: Long): List<LyricLine>
    // 收藏或取消收藏歌曲
    suspend fun likeSong(songId: Long, like: Boolean = true): LikeSongResult
    // 获取用户收藏的全部歌曲 ID
    suspend fun getLikedSongIds(userId: Long): List<Long>
    // 批量获取歌曲详情
    suspend fun getSongDetails(songIds: List<Long>): List<Song>
    // 获取用户歌单列表
    suspend fun getUserPlaylists(
        userId: Long,
        limit: Int = 30,
        offset: Int = 0
    ): List<UserPlaylist>
    // 获取歌单内的歌曲列表
    suspend fun getPlaylistSongs(
        playlistId: Long,
        limit: Int? = null,
        offset: Int = 0
    ): List<Song>
}
