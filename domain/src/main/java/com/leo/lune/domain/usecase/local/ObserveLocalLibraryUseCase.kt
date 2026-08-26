package com.leo.lune.domain.usecase.local

import com.leo.lune.domain.model.LocalLibraryItem
import com.leo.lune.domain.repository.DownloadRepository
import com.leo.lune.domain.repository.ImportedLocalTrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

// 合并本地下载与文件夹导入，按加入时间倒序
class ObserveLocalLibraryUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val importedLocalTrackRepository: ImportedLocalTrackRepository
) {
    operator fun invoke(): Flow<List<LocalLibraryItem>> = combine(
        downloadRepository.observeDownloadedSongs(),
        importedLocalTrackRepository.observeImportedTracks()
    ) { downloaded, imported ->
        buildList {
            downloaded.forEach { add(it.toLocalLibraryItem()) }
            imported.forEach { add(it.toLocalLibraryItem()) }
        }.sortedByDescending { it.addedAt }
    }
}
