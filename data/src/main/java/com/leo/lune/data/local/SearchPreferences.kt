package com.leo.lune.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_prefs")

// 搜索历史本地存储（DataStore）
// 以分隔符拼接字符串保存最近搜索词，最多保留 MAX_RECENT_SEARCHES 条
@Singleton
class SearchPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.searchDataStore

    // 观察最近搜索词列表（按写入顺序，最新在前）
    val recentSearchesFlow: Flow<List<String>> = dataStore.data.map { prefs ->
        decodeRecentSearches(prefs[KEY_RECENT_SEARCHES])
    }

    // 将新的搜索词插入队首；已存在则先移除再置顶
    suspend fun addRecentSearch(term: String) {
        val normalized = term.trim()
        if (normalized.isEmpty()) return

        dataStore.edit { prefs ->
            val updated = decodeRecentSearches(prefs[KEY_RECENT_SEARCHES])
                .filterNot { it == normalized }
                .toMutableList()
            updated.add(0, normalized)
            prefs[KEY_RECENT_SEARCHES] = encodeRecentSearches(updated.take(MAX_RECENT_SEARCHES))
        }
    }

    // 删除单条最近搜索
    suspend fun removeRecentSearch(term: String) {
        dataStore.edit { prefs ->
            val updated = decodeRecentSearches(prefs[KEY_RECENT_SEARCHES])
                .filterNot { it == term }
            prefs[KEY_RECENT_SEARCHES] = encodeRecentSearches(updated)
        }
    }

    // 清空全部最近搜索
    suspend fun clearRecentSearches() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_RECENT_SEARCHES)
        }
    }

    companion object {
        private val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        private const val DELIMITER = "\u001E"
        private const val MAX_RECENT_SEARCHES = 8

        private fun encodeRecentSearches(terms: List<String>): String {
            return terms.joinToString(DELIMITER)
        }

        private fun decodeRecentSearches(raw: String?): List<String> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.split(DELIMITER).filter { it.isNotBlank() }
        }
    }
}
