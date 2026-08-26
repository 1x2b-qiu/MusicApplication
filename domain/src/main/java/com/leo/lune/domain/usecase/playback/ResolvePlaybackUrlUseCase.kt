package com.leo.lune.domain.usecase.playback

import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.PlaybackQuality
import com.leo.lune.domain.model.PlaybackSource
import com.leo.lune.domain.model.SettingKeys
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.DownloadRepository
import com.leo.lune.domain.repository.ImportedLocalTrackRepository
import com.leo.lune.domain.repository.MusicRepository
import com.leo.lune.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * URL 解析 UseCase
 *
 * 优先级：导入本地文件 → 本地下载文件 → 远端流媒体
 * - preferredQuality 非空：从下载库取该档，或以该档码率拉流
 * - preferredQuality 为空：本地取最高音质已下载文件，流媒体读设置页默认音质
 *
 * 返回 null 表示该歌曲无可播放地址（无权限或 API 限制）。
 */
class ResolvePlaybackUrlUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val importedLocalTrackRepository: ImportedLocalTrackRepository,
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        song: Song,
        preferredQuality: DownloadQuality? = null
    ): PlaybackSource? {
        resolveImportedLocal(song)?.let { return it }
        if (song.isImportedLocal) return null

        val songId = song.id
        // 1. 本地已下载 → 直接返回文件路径，零网络消耗
        val local = downloadRepository.getLocalPlayback(songId, preferredQuality)
        if (local != null) {
            val (path, quality) = local
            return PlaybackSource.LocalFile(
                path = path,
                qualityLabel = quality.label
            )
        }

        // 2. 远端流媒体：按优先级解析目标码率
        val bitrate = resolveStreamBitrate(preferredQuality)
        val songUrl = musicRepository.getSongUrl(songId, bitrate)

        // url 为 null 表示无播放权限（版权限制等）
        return songUrl.url?.let { url ->
            PlaybackSource.RemoteStream(
                url = url,
                qualityLabel = PlaybackQuality.fromBitrate(songUrl.bitrate).label
            )
        }
    }

    // 队列/快照里带 URI 时直接播；最近播放等只有负 id 时再查导入表
    private suspend fun resolveImportedLocal(song: Song): PlaybackSource? {
        val fromSong = song.localPlaybackUri?.takeIf { it.isNotBlank() }
        val uri = fromSong
            ?: if (song.id < 0L) importedLocalTrackRepository.getByLocalId(song.id)?.uri else null
        if (uri.isNullOrBlank()) return null
        return PlaybackSource.LocalFile(path = uri, qualityLabel = "本地")
    }

    /**
     * 解析流媒体目标码率
     * - preferredQuality 非空：直接使用该档码率（下载页点播等场景）
     * - 为空：读设置页「默认音质」，未配置时回退 [PlaybackQuality.Default]
     */
    private suspend fun resolveStreamBitrate(preferred: DownloadQuality?): Int {
        if (preferred != null) return preferred.bitrate
        val raw = settingsRepository.getValue(SettingKeys.PLAYBACK_DEFAULT_QUALITY)
        val bitrate = raw?.toIntOrNull() ?: return PlaybackQuality.Default.bitrate
        return PlaybackQuality.fromBitrate(bitrate).bitrate
    }
}
