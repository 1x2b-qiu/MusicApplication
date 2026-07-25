package com.leo.lune.domain.repository

import com.leo.lune.domain.model.RecognizedTrack

// 听歌识曲仓储：将本地音频片段提交给 AudD
interface IdentifyRepository {
    // 识别本地音频文件；未匹配返回 null
    // audioPath：本地绝对路径（如录音得到的 m4a/wav）
    suspend fun recognize(audioPath: String): RecognizedTrack?
}
