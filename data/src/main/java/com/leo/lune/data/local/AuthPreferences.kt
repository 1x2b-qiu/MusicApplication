package com.leo.lune.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.leo.lune.domain.model.LoginState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

// 登录信息本地存储（DataStore）
// 持久化 Cookie、昵称、用户 ID 与头像 URL
@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.authDataStore

    // 观察登录状态；Cookie 非空即视为已登录
    val loginStateFlow: Flow<LoginState> = dataStore.data.map { prefs ->
        val cookie = prefs[KEY_COOKIE]
        LoginState(
            isLoggedIn = !cookie.isNullOrBlank(),
            nickname = prefs[KEY_NICKNAME],
            userId = prefs[KEY_USER_ID],
            avatarUrl = prefs[KEY_AVATAR_URL]
        )
    }

    // 读取持久化的 Cookie，用于启动时恢复会话
    suspend fun getCookie(): String? {
        return dataStore.data.first()[KEY_COOKIE]
    }

    // 登录成功后保存 Cookie 与用户资料
    suspend fun saveLogin(
        cookie: String,
        nickname: String?,
        userId: Long?,
        avatarUrl: String?
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_COOKIE] = cookie
            if (nickname != null) {
                prefs[KEY_NICKNAME] = nickname
            } else {
                prefs.remove(KEY_NICKNAME)
            }
            if (userId != null) {
                prefs[KEY_USER_ID] = userId
            } else {
                prefs.remove(KEY_USER_ID)
            }
            if (!avatarUrl.isNullOrBlank()) {
                prefs[KEY_AVATAR_URL] = avatarUrl
            } else {
                prefs.remove(KEY_AVATAR_URL)
            }
        }
    }

    // 登出时清除所有登录相关字段
    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_COOKIE)
            prefs.remove(KEY_NICKNAME)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_AVATAR_URL)
        }
    }

    companion object {
        private val KEY_COOKIE = stringPreferencesKey("cookie")
        private val KEY_NICKNAME = stringPreferencesKey("nickname")
        private val KEY_USER_ID = longPreferencesKey("user_id")
        private val KEY_AVATAR_URL = stringPreferencesKey("avatar_url")
    }
}
