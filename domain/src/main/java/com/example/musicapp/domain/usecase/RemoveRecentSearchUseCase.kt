package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.repository.SearchHistoryRepository
import javax.inject.Inject

// 删除单条最近搜索记录
class RemoveRecentSearchUseCase @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository
) {
    suspend operator fun invoke(term: String) {
        searchHistoryRepository.removeRecentSearch(term)
    }
}
