package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.repository.SearchHistoryRepository
import javax.inject.Inject

// 将搜索词写入最近搜索历史
class AddRecentSearchUseCase @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository
) {
    suspend operator fun invoke(term: String) {
        searchHistoryRepository.addRecentSearch(term)
    }
}
