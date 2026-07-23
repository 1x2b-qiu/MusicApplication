package com.leo.lune.domain.usecase.history

import com.leo.lune.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// 观察本地最近搜索词
class ObserveRecentSearchesUseCase @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository
) {
    operator fun invoke(): Flow<List<String>> {
        return searchHistoryRepository.observeRecentSearches()
    }
}
