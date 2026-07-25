package com.leo.lune.data.repository.impl

import com.leo.lune.data.local.dao.AppSettingDao
import com.leo.lune.data.local.entity.AppSettingEntity
import com.leo.lune.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val appSettingDao: AppSettingDao
) : SettingsRepository {

    override fun observeValue(key: String): Flow<String?> {
        return appSettingDao.observeValue(key)
    }

    override suspend fun getValue(key: String): String? = withContext(Dispatchers.IO) {
        appSettingDao.getValue(key)
    }

    override suspend fun setValue(key: String, value: String) = withContext(Dispatchers.IO) {
        appSettingDao.upsert(AppSettingEntity(key = key, value = value))
    }
}
