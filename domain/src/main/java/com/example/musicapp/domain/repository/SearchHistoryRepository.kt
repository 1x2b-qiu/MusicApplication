package com.example.musicapp.domain.repository

import kotlinx.coroutines.flow.Flow

// 搜索历史仓储接口
interface SearchHistoryRepository {
    // 观察最近搜索词
    fun observeRecentSearches(): Flow<List<String>>

    // 记录一次搜索（去重后置顶）
    suspend fun addRecentSearch(term: String)

    // 删除单条搜索记录
    suspend fun removeRecentSearch(term: String)

    // 清空全部搜索记录
    suspend fun clearRecentSearches()
}
