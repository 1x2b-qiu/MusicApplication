package com.leo.lune.domain.usecase.settings

import com.leo.lune.domain.model.SettingKeys
import com.leo.lune.domain.repository.SettingsRepository
import javax.inject.Inject

// 保存 SAF 下载目录（tree URI + 展示名）
class SetDownloadStorageLocationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(treeUri: String, displayName: String) {
        settingsRepository.setValue(SettingKeys.DOWNLOAD_STORAGE_TREE_URI, treeUri)
        settingsRepository.setValue(
            SettingKeys.DOWNLOAD_STORAGE_DISPLAY_NAME,
            displayName.ifBlank { "已选择文件夹" }
        )
    }
}
