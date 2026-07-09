package com.example.musicapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.musicapp.domain.model.LoginState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.authDataStore

    val loginStateFlow: Flow<LoginState> = dataStore.data.map { prefs ->
        val cookie = prefs[KEY_COOKIE]
        LoginState(
            isLoggedIn = !cookie.isNullOrBlank(),
            nickname = prefs[KEY_NICKNAME],
            userId = prefs[KEY_USER_ID]
        )
    }

    suspend fun getCookie(): String? {
        return dataStore.data.first()[KEY_COOKIE]
    }

    suspend fun saveLogin(cookie: String, nickname: String?, userId: Long?) {
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
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_COOKIE)
            prefs.remove(KEY_NICKNAME)
            prefs.remove(KEY_USER_ID)
        }
    }

    companion object {
        private val KEY_COOKIE = stringPreferencesKey("cookie")
        private val KEY_NICKNAME = stringPreferencesKey("nickname")
        private val KEY_USER_ID = longPreferencesKey("user_id")
    }
}
