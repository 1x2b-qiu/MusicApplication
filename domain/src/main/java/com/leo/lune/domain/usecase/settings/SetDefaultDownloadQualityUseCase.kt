package com.leo.lune.domain.usecase.settings

import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.SettingKeys
import com.leo.lune.domain.repository.SettingsRepository
import javax.inject.Inject

// 保存下载默认音质（写入 bitrate 字符串）
class SetDefaultDownloadQualityUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(quality: DownloadQuality) {
        settingsRepository.setValue(
            SettingKeys.DOWNLOAD_DEFAULT_QUALITY,
            quality.bitrate.toString()
        )
    }
}
