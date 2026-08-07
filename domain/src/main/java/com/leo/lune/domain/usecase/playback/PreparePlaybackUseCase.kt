package com.leo.lune.domain.usecase.playback

import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.PlaybackSource
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.repository.ArtworkRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * 播放准备结果
 *
 * artworkBytes 可为 null：封面加载超时或失败时，通知栏改由系统自行拉 artworkUri，
 * 不影响播放启动，调用方无需做额外处理。
 *
 * 注意：data class 含 ByteArray 时 equals/hashCode 语义不完整，
 * 但此类仅作单向数据传输对象（DTO），不参与比较，因此安全。
 */
@Suppress("ArrayInDataClass")
data class PlaybackPreparation(
    val source: PlaybackSource,
    val artworkBytes: ByteArray?
)

/**
 * 播放准备 UseCase
 *
 * 编排两个独立子任务的并行执行：
 * 1. [ResolvePlaybackUrlUseCase]：解析可播放 URL（本地文件 or 远端流）
 * 2. [ArtworkRepository.loadArtworkBytes]：下载并压缩封面
 *
 * 两者并行，互不阻塞，整体耗时 = max(URL 解析, 封面下载)。
 * URL 解析失败时抛出异常，封面加载失败时返回 null（降级处理）。
 *
 * @throws IllegalStateException 当歌曲无播放权限（无可用 URL）时
 */
class PreparePlaybackUseCase @Inject constructor(
    private val resolvePlaybackUrl: ResolvePlaybackUrlUseCase,
    private val artworkRepository: ArtworkRepository
) {
    suspend operator fun invoke(
        song: Song,
        preferredQuality: DownloadQuality? = null
    ): PlaybackPreparation = coroutineScope {
        // 并行启动，URL 解析通常比封面下载快（仅一次 API，封面需完整下载图片）
        val urlDeferred = async { resolvePlaybackUrl(song.id, preferredQuality) }
        // 封面失败不影响播放：runCatching 吸收所有异常，超时由 ArtworkRepository 自行处理
        val artworkDeferred = async {
            runCatching { artworkRepository.loadArtworkBytes(song.coverUrl) }.getOrNull()
        }

        val source = urlDeferred.await()
            ?: throw IllegalStateException("该歌曲暂无播放权限")
        // source 解析失败时 coroutineScope 会取消 artworkDeferred，避免浪费网络

        val artworkBytes = artworkDeferred.await()
        PlaybackPreparation(source = source, artworkBytes = artworkBytes)
    }
}
