package com.leo.lune.domain.usecase.settings

import com.leo.lune.domain.model.SettingKeys
import com.leo.lune.domain.repository.DownloadRepository
import com.leo.lune.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

// 下载存储位置快照（SAF）+ 设置页展示用路径
data class DownloadStorageLocation(
    // content:// 形式的文档树 URI；未选择时为 null
    val treeUri: String?,
    // 用户选择时的文件夹展示名；未选择时为 null
    val displayName: String?,
    // 设置页副标题：绝对路径 / URI / 私有目录（始终有值）
    val pathHint: String
)

// 观察 SAF 下载目录配置
// 合并两个 Settings key，并经 DownloadRepository 解析为可读 pathHint
class ObserveDownloadStorageLocationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val downloadRepository: DownloadRepository
) {
    // URI 或展示名任一变更都会重新发射；空白字符串视为未配置
    operator fun invoke(): Flow<DownloadStorageLocation> {
        return combine(
            settingsRepository.observeValue(SettingKeys.DOWNLOAD_STORAGE_TREE_URI),
            settingsRepository.observeValue(SettingKeys.DOWNLOAD_STORAGE_DISPLAY_NAME)
        ) { uri, name ->
            val treeUri = uri?.takeIf { it.isNotBlank() }
            DownloadStorageLocation(
                treeUri = treeUri,
                displayName = name?.takeIf { it.isNotBlank() },
                pathHint = downloadRepository.resolveStorageDisplayPath(treeUri)
            )
        }
    }
}
