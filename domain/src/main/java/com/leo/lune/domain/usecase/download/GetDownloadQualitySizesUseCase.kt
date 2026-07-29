package com.leo.lune.domain.usecase.download

import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.repository.MusicRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

// 并行查询各下载音质档的真实文件大小（字节）
// 供下载音质弹窗展示体积；单档失败或未知时记为 0，不拖垮整次结果
class GetDownloadQualitySizesUseCase @Inject constructor(
    // 通过 getSongUrl(bitrate) 拿到该档 sizeBytes
    private val musicRepository: MusicRepository
) {
    // 返回 Map：音质枚举 → 字节数；key 覆盖 DownloadQuality 全部档位
    suspend operator fun invoke(songId: Long): Map<DownloadQuality, Long> = coroutineScope {
        DownloadQuality.entries.map { quality ->
            // 各档并行请求，缩短弹窗等待
            async {
                val sizeBytes = runCatching {
                    musicRepository.getSongUrl(songId, quality.bitrate).sizeBytes
                }.getOrDefault(0L)
                // 负值按 0 处理，避免 UI 展示异常
                quality to sizeBytes.coerceAtLeast(0L)
            }
        }.awaitAll().toMap()
    }
}
