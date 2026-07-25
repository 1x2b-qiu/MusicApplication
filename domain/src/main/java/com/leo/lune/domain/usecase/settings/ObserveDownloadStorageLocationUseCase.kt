package com.leo.lune.domain.usecase.settings

import com.leo.lune.domain.model.SettingKeys
import com.leo.lune.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

// 下载存储位置（SAF）：URI + 展示名；未选择时均为 null
data class DownloadStorageLocation(
    val treeUri: String?,
    val displayName: String?
)

class ObserveDownloadStorageLocationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<DownloadStorageLocation> {
        return combine(
            settingsRepository.observeValue(SettingKeys.DOWNLOAD_STORAGE_TREE_URI),
            settingsRepository.observeValue(SettingKeys.DOWNLOAD_STORAGE_DISPLAY_NAME)
        ) { uri, name ->
            DownloadStorageLocation(
                treeUri = uri?.takeIf { it.isNotBlank() },
                displayName = name?.takeIf { it.isNotBlank() }
            )
        }
    }
}
