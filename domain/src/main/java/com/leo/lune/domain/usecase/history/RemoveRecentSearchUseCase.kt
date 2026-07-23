package com.leo.lune.domain.usecase.history

import com.leo.lune.domain.repository.SearchHistoryRepository
import javax.inject.Inject

// 删除单条最近搜索记录
class RemoveRecentSearchUseCase @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository
) {
    suspend operator fun invoke(term: String) {
        searchHistoryRepository.removeRecentSearch(term)
    }
}
