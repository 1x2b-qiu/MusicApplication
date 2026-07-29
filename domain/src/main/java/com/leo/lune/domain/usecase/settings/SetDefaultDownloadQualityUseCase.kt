package com.leo.lune.domain.usecase.settings

import com.leo.lune.domain.model.DownloadQuality
import com.leo.lune.domain.model.SettingKeys
import com.leo.lune.domain.repository.SettingsRepository
import javax.inject.Inject

// 保存下载默认音质
// 将 DownloadQuality 写成 bitrate 字符串，与 ObserveDefaultDownloadQualityUseCase 成对使用
class SetDefaultDownloadQualityUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    // quality：用户在设置页或弹窗预选的默认档位
    suspend operator fun invoke(quality: DownloadQuality) {
        settingsRepository.setValue(
            SettingKeys.DOWNLOAD_DEFAULT_QUALITY,
            quality.bitrate.toString()
        )
    }
}
