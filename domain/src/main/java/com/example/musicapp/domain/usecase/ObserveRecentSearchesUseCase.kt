package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.repository.SearchHistoryRepository
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
