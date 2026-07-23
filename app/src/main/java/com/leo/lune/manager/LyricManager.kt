package com.leo.lune.manager

import com.leo.lune.domain.model.LyricLine
import com.leo.lune.domain.model.LyricMatcher
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.usecase.music.GetSongLyricsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// 歌词解析结果：当前高亮行下标 + 展示文案
data class LyricDisplay(
    val index: Int = 0,
    val line: String = "听点音乐吧"
)

// 歌词管理器（Hilt 单例）
// 职责：异步加载 LRC 歌词、根据播放进度匹配当前行、生成展示文案
@Singleton
class LyricManager @Inject constructor(
    private val getSongLyricsUseCase: GetSongLyricsUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lyricJob: Job? = null
    // 当前加载目标 songId，防止过期回调写入
    private var targetSongId: Long? = null

    // 当前歌曲的 LRC 歌词列表
    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    // 异步加载歌词；切歌时自动取消旧任务
    fun loadLyrics(songId: Long) {
        lyricJob?.cancel()
        targetSongId = songId
        _lyrics.value = emptyList()
        lyricJob = scope.launch {
            runCatching { getSongLyricsUseCase(songId) }
                .onSuccess { result ->
                    if (targetSongId == songId) {
                        _lyrics.value = result
                    }
                }
        }
    }

    // 取消进行中的歌词加载（清空队列时调用）
    fun cancelLoading() {
        lyricJob?.cancel()
        lyricJob = null
        targetSongId = null
    }

    // 根据播放进度解析当前歌词行下标与展示文案
    fun resolveCurrentLyric(positionMs: Long, song: Song?): LyricDisplay {
        val currentLyrics = _lyrics.value
        val lyricIndex = LyricMatcher.currentIndex(currentLyrics, positionMs)
        // 进度尚未到达第一句时（下标停在 0）不展示歌词
        val lyricText = currentLyrics.getOrNull(lyricIndex)?.takeIf { it.timeMs <= positionMs }?.text
        val line = when {
            lyricText != null -> "\"$lyricText\""
            song != null -> fallbackLyric(song)
            else -> "听点音乐吧"
        }
        return LyricDisplay(index = lyricIndex, line = line)
    }

    // 无歌词时的兜底文案：展示歌名
    fun fallbackLyric(song: Song): String = "\"${song.name}\""
}
