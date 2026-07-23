package com.leo.lune.domain.usecase.history

import com.leo.lune.domain.repository.SearchHistoryRepository
import javax.inject.Inject

// 清空全部最近搜索记录
class ClearRecentSearchesUseCase @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository
) {
    suspend operator fun invoke() {
        searchHistoryRepository.clearRecentSearches()
    }
}
