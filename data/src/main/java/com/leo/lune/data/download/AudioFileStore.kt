package com.leo.lune.data.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// 应用私有目录下的音频文件存取
// 根路径为 filesDir/music_downloads，卸载随 App 清除，无需申请存储权限
// 文件按 songId + 音质码率区分，支持同曲多档并存
@Singleton
class AudioFileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 下载目录；每次访问时确保已创建
    private val downloadDir: File
        get() = File(context.filesDir, DIR_NAME).also { it.mkdirs() }

    // 正式落盘文件：{songId}_{bitrate}.{ext}，ext 缺省为 mp3
    fun targetFile(songId: Long, bitrate: Int, extension: String): File {
        val safeExt = extension.trim('.').ifBlank { "mp3" }
        return File(downloadDir, "${fileStem(songId, bitrate)}.$safeExt")
    }

    // 下载过程中的临时文件，成功后再 rename / copy 到 targetFile
    fun tempFile(songId: Long, bitrate: Int): File {
        return File(downloadDir, "${fileStem(songId, bitrate)}.download")
    }

    // 删除该歌曲指定音质相关的正式文件与临时文件（兼容不同扩展名）
    fun deleteForSong(songId: Long, bitrate: Int) {
        val stem = fileStem(songId, bitrate)
        downloadDir.listFiles()
            ?.filter { it.name.startsWith("$stem.") || it.name == "$stem.download" }
            ?.forEach { it.delete() }
    }

    private fun fileStem(songId: Long, bitrate: Int): String = "${songId}_$bitrate"

    companion object {
        // 相对 filesDir 的子目录名
        private const val DIR_NAME = "music_downloads"
    }
}
