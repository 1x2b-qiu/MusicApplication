package com.leo.lune.domain.usecase.settings

import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.SettingKeys
import com.leo.lune.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// 观察下载默认音质设置
// SettingsRepository 存的是 bitrate 字符串，此处映射为 DownloadQuality；未配置时回退 Default
class ObserveDefaultDownloadQualityUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    // 持续推送当前默认音质；底层 key 变化时自动更新
    operator fun invoke(): Flow<DownloadQuality> {
        return settingsRepository.observeValue(SettingKeys.DOWNLOAD_DEFAULT_QUALITY).map { raw ->
            parseQuality(raw)
        }
    }

    companion object {
        // 将持久化的 bitrate 字符串解析为枚举；非法或空值 → Default
        fun parseQuality(raw: String?): DownloadQuality {
            val bitrate = raw?.toIntOrNull() ?: return DownloadQuality.Default
            return DownloadQuality.fromBitrate(bitrate)
        }
    }
}
