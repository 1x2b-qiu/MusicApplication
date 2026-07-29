package com.leo.lune.domain.usecase.identify

import com.leo.lune.domain.model.IdentifyMatchResult
import com.leo.lune.domain.repository.IdentifyRepository
import com.leo.lune.domain.repository.MusicRepository
import javax.inject.Inject

// 听歌识曲编排：AudD 识别音频 → 拼「歌名 歌手」→ 网易云搜索候选列表
// 跨 IdentifyRepository 与 MusicRepository，故保留为 UseCase 而非由 ViewModel 直调
class RecognizeSongUseCase @Inject constructor(
    // AudD 侧：根据本地录音文件识别曲目元数据
    private val identifyRepository: IdentifyRepository,
    // 网易云侧：按关键词搜索可播放候选
    private val musicRepository: MusicRepository,
) {
    // audioPath：本地录音绝对路径；searchLimit：网易云候选条数上限
    suspend operator fun invoke(
        audioPath: String,
        searchLimit: Int = 50,
    ): IdentifyMatchResult {
        // 识别失败（无匹配 / 接口失败）时直接返回未识别
        val track = identifyRepository.recognize(audioPath)
            ?: return IdentifyMatchResult.NotRecognized

        // 用歌名 + 歌手组成搜索词；任一段为空则跳过该段
        val keywords = listOf(track.title, track.artist)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")

        // 关键词为空时不再打搜索接口，仍返回已识别到的曲目信息
        val songs = if (keywords.isEmpty()) {
            emptyList()
        } else {
            musicRepository.searchSongs(keywords, searchLimit)
        }

        return IdentifyMatchResult.Matched(track = track, songs = songs)
    }
}
