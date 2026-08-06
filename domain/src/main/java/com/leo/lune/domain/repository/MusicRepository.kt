package com.leo.lune.domain.repository

import com.leo.lune.domain.model.LikeSongResult
import com.leo.lune.domain.model.LyricLine
import com.leo.lune.domain.model.PersonalizedPlaylist
import com.leo.lune.domain.model.PlaylistCategory
import com.leo.lune.domain.model.PlaylistGenre
import com.leo.lune.domain.model.SearchSuggestion
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.model.SongUrl
import com.leo.lune.domain.model.UserPlaylist

// 音乐数据仓储接口，封装搜索、播放、收藏等远端操作
interface MusicRepository {
    // 按关键词搜索歌曲
    suspend fun searchSongs(keywords: String, limit: Int = 20): List<Song>
    // 获取热搜关键词列表
    suspend fun getHotSearchTerms(): List<String>
    // 按输入获取搜索联想建议
    suspend fun getSearchSuggestions(keywords: String): List<SearchSuggestion>
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
    // 获取每日推荐歌曲（需登录）；afresh 为 true 时刷新当日推荐
    suspend fun getDailyRecommendSongs(afresh: Boolean = false): List<Song>
    // 获取推荐新音乐（猜你喜欢）
    suspend fun getPersonalizedNewsongs(limit: Int = 15): List<Song>
    // 获取推荐歌单（甄选歌单）
    suspend fun getPersonalizedPlaylists(limit: Int = 10): List<PersonalizedPlaylist>
    // 获取热门歌单分类标签（不含封面，供歌单广场 Tab）
    suspend fun getHotPlaylistCategories(): List<PlaylistCategory>
    // 按分类名获取网友精选碟歌单
    suspend fun getTopPlaylists(cat: String, limit: Int = 50): List<PersonalizedPlaylist>
    // 获取热门风格分类（含封面）
    suspend fun getHotPlaylistGenres(): List<PlaylistGenre>
}
