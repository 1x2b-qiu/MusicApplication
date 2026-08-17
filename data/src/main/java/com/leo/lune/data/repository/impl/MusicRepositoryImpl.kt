package com.leo.lune.data.repository.impl

import com.leo.lune.data.mapper.normalizeCoverUrl
import com.leo.lune.data.mapper.toLikeSongResult
import com.leo.lune.data.mapper.toPersonalizedPlaylist
import com.leo.lune.data.mapper.toPlaylistCategory
import com.leo.lune.data.mapper.toPlaylistDetail
import com.leo.lune.data.mapper.toSong
import com.leo.lune.data.mapper.toSongUrl
import com.leo.lune.data.mapper.toSubscribePlaylistResult
import com.leo.lune.data.mapper.toUserPlaylist
import com.leo.lune.data.remote.api.NeteaseApi
import com.leo.lune.data.remote.response.SuggestAlbumDto
import com.leo.lune.data.remote.response.SuggestArtistDto
import com.leo.lune.data.remote.response.SuggestSongDto
import com.leo.lune.data.util.LrcParser
import com.leo.lune.domain.model.LikeSongResult
import com.leo.lune.domain.model.LyricLine
import com.leo.lune.domain.model.MusicStyle
import com.leo.lune.domain.model.MusicStyleDetail
import com.leo.lune.domain.model.MusicStyleSongsPage
import com.leo.lune.domain.model.PersonalizedPlaylist
import com.leo.lune.domain.model.PlaylistCategory
import com.leo.lune.domain.model.PlaylistDetail
import com.leo.lune.domain.model.SearchSuggestion
import com.leo.lune.domain.model.SearchSuggestionType
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.model.SongUrl
import com.leo.lune.domain.model.SubscribePlaylistResult
import com.leo.lune.domain.model.UserPlaylist
import com.leo.lune.domain.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    // 获取热搜关键词（失败时向上抛，由 ViewModel 静默处理）
    override suspend fun getHotSearchTerms(): List<String> {
        val response = neteaseApi.getSearchHot()
        if (response.code != 200) {
            throw IllegalStateException("Get hot search failed with code ${response.code}")
        }
        return response.result?.hots.orEmpty()
            .mapNotNull { it.first?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
    }

    // 获取搜索联想：优先歌曲 / 歌手 / 专辑；若为空则兜底 allMatch 关键词
    override suspend fun getSearchSuggestions(keywords: String): List<SearchSuggestion> {
        val response = neteaseApi.getSearchSuggest(keywords)
        if (response.code != 200) {
            throw IllegalStateException("Get search suggest failed with code ${response.code}")
        }
        val result = response.result ?: return emptyList()
        val typed = buildList {
            result.songs.orEmpty().mapNotNullTo(this) { it.toSuggestion() }
            result.artists.orEmpty().mapNotNullTo(this) { it.toSuggestion() }
            result.albums.orEmpty().mapNotNullTo(this) { it.toSuggestion() }
        }
        if (typed.isNotEmpty()) return typed.distinctBy { it.text to it.type }

        return result.allMatch.orEmpty()
            .mapNotNull { match ->
                val keyword = match.keyword?.trim()?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
                SearchSuggestion(
                    text = keyword,
                    keyword = keyword,
                    type = SearchSuggestionType.Song
                )
            }
            .distinctBy { it.text }
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
        // 解析属 CPU 工作，切到 Default 避免占主线程
        return withContext(Dispatchers.Default) { LrcParser.parse(lrcText) }
    }

    // 收藏或取消收藏歌曲
    override suspend fun likeSong(songId: Long, like: Boolean): LikeSongResult {
        val response = neteaseApi.likeSong(songId = songId, like = like)
        return response.code.toLikeSongResult()
    }

    // 收藏或取消收藏歌单；t=1 收藏，t=2 取消
    override suspend fun subscribePlaylist(
        playlistId: Long,
        subscribe: Boolean
    ): SubscribePlaylistResult {
        val response = neteaseApi.subscribePlaylist(
            action = if (subscribe) 1 else 2,
            playlistId = playlistId
        )
        return response.code.toSubscribePlaylistResult()
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

    // 获取歌单详情元数据（带 timestamp，打穿代理缓存以保证 subscribed 回显）
    override suspend fun getPlaylistDetail(playlistId: Long): PlaylistDetail {
        val response = neteaseApi.getPlaylistDetail(playlistId)
        if (response.code != 200) {
            throw IllegalStateException("Get playlist detail failed with code ${response.code}")
        }
        val playlist = response.playlist
            ?: throw IllegalStateException("Playlist detail missing for id $playlistId")
        return playlist.toPlaylistDetail()
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

    // 获取私人 FM 一批歌曲
    override suspend fun getPersonalFmSongs(): List<Song> {
        val response = neteaseApi.getPersonalFm()
        if (response.code != 200) {
            throw IllegalStateException("Get personal FM failed with code ${response.code}")
        }
        return response.data.orEmpty().map { it.toSong() }
    }

    // 获取每日推荐歌曲列表
    override suspend fun getDailyRecommendSongs(afresh: Boolean): List<Song> {
        val response = neteaseApi.getRecommendSongs(afresh = afresh)
        if (response.code != 200) {
            throw IllegalStateException("Get daily recommend songs failed with code ${response.code}")
        }
        return response.data?.dailySongs.orEmpty().map { it.toSong() }
    }

    // 获取推荐新音乐列表（猜你喜欢）
    override suspend fun getPersonalizedNewsongs(limit: Int): List<Song> {
        val response = neteaseApi.getPersonalizedNewsong(limit = limit)
        if (response.code != 200) {
            throw IllegalStateException("Get personalized newsong failed with code ${response.code}")
        }
        return response.result.orEmpty().mapNotNull { it.song?.toSong() }
    }

    // 获取推荐歌单列表（甄选歌单）
    override suspend fun getPersonalizedPlaylists(limit: Int): List<PersonalizedPlaylist> {
        val response = neteaseApi.getPersonalized(limit = limit)
        if (response.code != 200) {
            throw IllegalStateException("Get personalized playlists failed with code ${response.code}")
        }
        return response.result.orEmpty().mapNotNull { it.toPersonalizedPlaylist() }
    }

    // 获取热门歌单分类标签（不含封面）
    override suspend fun getHotPlaylistCategories(): List<PlaylistCategory> {
        val response = neteaseApi.getPlaylistHot()
        if (response.code != 200) {
            throw IllegalStateException("Get playlist hot tags failed with code ${response.code}")
        }
        return response.tags.orEmpty().mapNotNull { it.toPlaylistCategory() }
    }

    // 按分类名获取网友精选碟歌单
    override suspend fun getTopPlaylists(cat: String, limit: Int): List<PersonalizedPlaylist> {
        val response = neteaseApi.getTopPlaylist(cat = cat, limit = limit)
        if (response.code != 200) {
            throw IllegalStateException("Get top playlists failed with code ${response.code}")
        }
        return response.playlists.orEmpty().mapNotNull { it.toPersonalizedPlaylist() }
    }

    // 获取顶级曲风列表（含封面）；子风格不在曲库入口展示
    override suspend fun getMusicStyles(): List<MusicStyle> {
        val response = neteaseApi.getStyleList()
        if (response.code != 200) {
            throw IllegalStateException("Get style list failed with code ${response.code}")
        }
        return response.data.orEmpty().mapNotNull { tag ->
            val id = tag.tagId ?: return@mapNotNull null
            val name = tag.tagName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            MusicStyle(
                id = id,
                name = name,
                coverUrl = normalizeCoverUrl(tag.picUrl)
            )
        }
    }

    // 获取曲风详情
    override suspend fun getMusicStyleDetail(styleId: Long): MusicStyleDetail {
        val response = neteaseApi.getStyleDetail(tagId = styleId)
        if (response.code != 200) {
            throw IllegalStateException("Get style detail failed with code ${response.code}")
        }
        val detail = response.data
            ?: throw IllegalStateException("Style detail is empty")
        return MusicStyleDetail(
            id = detail.tagId ?: styleId,
            name = detail.name.orEmpty(),
            description = detail.desc?.trim()?.takeIf { it.isNotEmpty() },
            coverUrl = normalizeCoverUrl(detail.cover?.firstOrNull()),
            songCountLabel = detail.songNum?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    // 获取曲风下单曲分页
    override suspend fun getMusicStyleSongs(
        styleId: Long,
        cursor: Long,
        size: Int
    ): MusicStyleSongsPage {
        val response = neteaseApi.getStyleSong(tagId = styleId, cursor = cursor, size = size)
        if (response.code != 200) {
            throw IllegalStateException("Get style songs failed with code ${response.code}")
        }
        val page = response.data?.page
        val songs = response.data?.songs.orEmpty().map { it.toSong() }
        val pageSize = page?.size?.takeIf { it > 0 } ?: size
        val pageCursor = page?.cursor ?: cursor
        val hasMore = page?.more == true
        return MusicStyleSongsPage(
            songs = songs,
            nextCursor = if (hasMore) pageCursor + pageSize else null,
            hasMore = hasMore,
            total = page?.total ?: songs.size
        )
    }
}

private fun SuggestSongDto.toSuggestion(): SearchSuggestion? {
    val songName = name?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val artistName = (artists ?: ar).orEmpty()
        .mapNotNull { it.name?.trim()?.takeIf(String::isNotEmpty) }
        .firstOrNull()
    val text = if (artistName != null) "$songName - $artistName" else songName
    return SearchSuggestion(
        text = text,
        keyword = songName,
        type = SearchSuggestionType.Song
    )
}

private fun SuggestArtistDto.toSuggestion(): SearchSuggestion? {
    val artistName = name?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return SearchSuggestion(
        text = artistName,
        keyword = artistName,
        type = SearchSuggestionType.Artist
    )
}

private fun SuggestAlbumDto.toSuggestion(): SearchSuggestion? {
    val albumName = name?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val artistName = artist?.name?.trim()?.takeIf(String::isNotEmpty)
    val text = if (artistName != null) "$albumName - $artistName" else albumName
    return SearchSuggestion(
        text = text,
        keyword = albumName,
        type = SearchSuggestionType.Album
    )
}
