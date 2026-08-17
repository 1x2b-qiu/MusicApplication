package com.leo.lune.data.repository.impl

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.leo.lune.data.local.dao.PlaybackSnapshotDao
import com.leo.lune.data.local.entity.PlaybackSnapshotEntity
import com.leo.lune.domain.model.PlaybackSnapshot
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.PlaybackSnapshotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// 播放快照仓储实现：Room 单行表 + Gson 序列化队列
@Singleton
class PlaybackSnapshotRepositoryImpl @Inject constructor(
    private val playbackSnapshotDao: PlaybackSnapshotDao
) : PlaybackSnapshotRepository {

    private val gson = Gson()
    private val songListType = object : TypeToken<List<SongJson>>() {}.type

    // 整队 Gson 序列化可能较重，切到 Default 避免占主线程
    override suspend fun save(snapshot: PlaybackSnapshot) = withContext(Dispatchers.Default) {
        val entity = PlaybackSnapshotEntity(
            currentSongId = snapshot.currentSong.id,
            currentSongName = snapshot.currentSong.name,
            currentSongArtists = snapshot.currentSong.artists,
            currentSongAlbum = snapshot.currentSong.album,
            currentSongCoverUrl = snapshot.currentSong.coverUrl,
            currentSongDurationMs = snapshot.currentSong.durationMs,
            queueJson = gson.toJson(snapshot.queue.map { it.toJson() }),
            queueIndex = snapshot.queueIndex
        )
        playbackSnapshotDao.upsert(entity)
    }

    override suspend fun get(): PlaybackSnapshot? = withContext(Dispatchers.Default) {
        val entity = playbackSnapshotDao.get() ?: return@withContext null
        runCatching {
            val queueJson = gson.fromJson<List<SongJson>>(entity.queueJson, songListType)
            PlaybackSnapshot(
                currentSong = Song(
                    id = entity.currentSongId,
                    name = entity.currentSongName,
                    artists = entity.currentSongArtists,
                    album = entity.currentSongAlbum,
                    coverUrl = entity.currentSongCoverUrl,
                    durationMs = entity.currentSongDurationMs
                ),
                queue = queueJson.map { it.toSong() },
                queueIndex = entity.queueIndex
            )
        }.getOrNull()
    }

    override suspend fun clear() {
        playbackSnapshotDao.delete()
    }

    // Gson 序列化用的中间模型（避免 Song 需要 @Serializable）
    private data class SongJson(
        val id: Long,
        val name: String,
        val artists: String,
        val album: String,
        val coverUrl: String?,
        val durationMs: Long,
        // 旧快照无此字段时 Gson 为 null
        val preferredDownloadBitrate: Int? = null
    ) {
        fun toSong(): Song = Song(
            id = id,
            name = name,
            artists = artists,
            album = album,
            coverUrl = coverUrl,
            durationMs = durationMs,
            preferredDownloadBitrate = preferredDownloadBitrate
        )
    }

    private fun Song.toJson(): SongJson = SongJson(
        id = id,
        name = name,
        artists = artists,
        album = album,
        coverUrl = coverUrl,
        durationMs = durationMs,
        preferredDownloadBitrate = preferredDownloadBitrate
    )
}
