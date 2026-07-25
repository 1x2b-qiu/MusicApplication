package com.leo.lune.domain.repository

import kotlinx.coroutines.flow.Flow

// 通用设置仓储：Room key-value，便于扩展其它配置项
interface SettingsRepository {

    fun observeValue(key: String): Flow<String?>

    suspend fun getValue(key: String): String?

    suspend fun setValue(key: String, value: String)
}
