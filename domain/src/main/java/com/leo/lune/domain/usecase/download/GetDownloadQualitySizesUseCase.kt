package com.leo.lune.domain.usecase.download

import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.usecase.music.GetSongUrlUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

// 并行拉取各下载音质档对应的真实文件大小（字节）；失败或未知为 0
class GetDownloadQualitySizesUseCase @Inject constructor(
    private val getSongUrlUseCase: GetSongUrlUseCase
) {
    suspend operator fun invoke(songId: Long): Map<DownloadQuality, Long> = coroutineScope {
        DownloadQuality.entries.map { quality ->
            async {
                val sizeBytes = runCatching {
                    getSongUrlUseCase(songId, quality.bitrate).sizeBytes
                }.getOrDefault(0L)
                quality to sizeBytes.coerceAtLeast(0L)
            }
        }.awaitAll().toMap()
    }
}
