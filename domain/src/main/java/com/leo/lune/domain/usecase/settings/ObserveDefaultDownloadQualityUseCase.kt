package com.leo.lune.domain.usecase.settings

import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.SettingKeys
import com.leo.lune.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// 观察下载默认音质；未配置时回退 DownloadQuality.Default
class ObserveDefaultDownloadQualityUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<DownloadQuality> {
        return settingsRepository.observeValue(SettingKeys.DOWNLOAD_DEFAULT_QUALITY).map { raw ->
            parseQuality(raw)
        }
    }

    companion object {
        fun parseQuality(raw: String?): DownloadQuality {
            val bitrate = raw?.toIntOrNull() ?: return DownloadQuality.Default
            return DownloadQuality.fromBitrate(bitrate)
        }
    }
}
