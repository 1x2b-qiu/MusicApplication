package com.leo.lune.domain.usecase.settings

import com.leo.lune.domain.model.SettingKeys
import com.leo.lune.domain.repository.SettingsRepository
import javax.inject.Inject

// 保存 SAF 下载目录（tree URI + 展示名）
// 一次写入两个 key，与 ObserveDownloadStorageLocationUseCase 成对使用
class SetDownloadStorageLocationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    // treeUri：文档树 URI；displayName 为空时写入兜底文案，避免设置页副标题空白
    suspend operator fun invoke(treeUri: String, displayName: String) {
        settingsRepository.setValue(SettingKeys.DOWNLOAD_STORAGE_TREE_URI, treeUri)
        settingsRepository.setValue(
            SettingKeys.DOWNLOAD_STORAGE_DISPLAY_NAME,
            displayName.ifBlank { "已选择文件夹" }
        )
    }
}
