package com.leo.lune.data.repository.impl

import com.leo.lune.data.local.SearchPreferences
import com.leo.lune.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

// 搜索历史仓储实现，委托给 DataStore
@Singleton
class SearchHistoryRepositoryImpl @Inject constructor(
    private val searchPreferences: SearchPreferences
) : SearchHistoryRepository {

    override fun observeRecentSearches(): Flow<List<String>> {
        return searchPreferences.recentSearchesFlow
    }

    override suspend fun addRecentSearch(term: String) {
        searchPreferences.addRecentSearch(term)
    }

    override suspend fun removeRecentSearch(term: String) {
        searchPreferences.removeRecentSearch(term)
    }

    override suspend fun clearRecentSearches() {
        searchPreferences.clearRecentSearches()
    }
}
