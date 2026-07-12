package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.repository.SearchHistoryRepository
import javax.inject.Inject

// 清空全部最近搜索记录
class ClearRecentSearchesUseCase @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository
) {
    suspend operator fun invoke() {
        searchHistoryRepository.clearRecentSearches()
    }
}
