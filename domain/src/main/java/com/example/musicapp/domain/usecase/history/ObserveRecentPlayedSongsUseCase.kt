package com.example.musicapp.domain.usecase.history

import com.example.musicapp.domain.model.Song
import com.example.musicapp.domain.repository.PlayHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// 观察本地最近播放列表，供首页「最近播放」区块使用
class ObserveRecentPlayedSongsUseCase @Inject constructor(
    private val playHistoryRepository: PlayHistoryRepository
) {
    // 观察最近播放列表，limit 控制返回条数
    operator fun invoke(limit: Int = 20): Flow<List<Song>> {
        return playHistoryRepository.observeRecentPlays(limit)
    }
}
