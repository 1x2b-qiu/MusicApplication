package com.leo.lune.domain.model

// 播放快照：进程被杀后用于恢复播放状态
// 只保存歌曲元数据与队列结构，不保存播放进度（恢复后从头开始）
data class PlaybackSnapshot(
    // 进程被杀前正在播放的歌曲
    val currentSong: Song,
    // 完整播放队列
    val queue: List<Song>,
    // 当前歌曲在队列中的下标
    val queueIndex: Int
)
