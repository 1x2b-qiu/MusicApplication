package com.leo.lune.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.leo.lune.domain.model.ThemeSetting
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

// 主题偏好本地存储（DataStore）
// 持久化用户手动选择的深浅色模式
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.themeDataStore

    // 观察当前主题设置
    val themeSettingFlow: Flow<ThemeSetting> = dataStore.data.map { prefs ->
        if (prefs.contains(KEY_DARK_THEME)) {
            ThemeSetting.Fixed(checkNotNull(prefs[KEY_DARK_THEME]))
        } else {
            ThemeSetting.FollowSystem
        }
    }

    // 保存用户选择的深浅色主题
    suspend fun setUserTheme(darkTheme: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DARK_THEME] = darkTheme
        }
    }

    companion object {
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
    }
}
