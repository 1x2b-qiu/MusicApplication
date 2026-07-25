package com.leo.lune.data.download

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.leo.lune.domain.model.SettingKeys
import com.leo.lune.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// 音频文件存取：临时文件始终在应用私有目录（便于断点续传）
// 正式文件：已选 SAF 目录则写入用户文件夹并返回 content URI；否则写入私有目录返回绝对路径
@Singleton
class AudioFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val privateDir: File
        get() = File(context.filesDir, DIR_NAME).also { it.mkdirs() }

    // 私有下载目录绝对路径（未选 SAF 时的默认落盘位置）
    fun privateDownloadDirPath(): String = privateDir.absolutePath

    // 设置页展示用：有 SAF 则尽量解析为绝对路径，解析失败回退 URI；无则私有目录
    fun resolveDisplayPath(treeUri: String?): String {
        if (treeUri.isNullOrBlank()) return privateDownloadDirPath()
        return resolveTreeUriToAbsolutePath(Uri.parse(treeUri)) ?: treeUri
    }

    // 下载过程中的临时文件（始终私有目录）
    fun tempFile(songId: Long, bitrate: Int): File {
        return File(privateDir, "${fileStem(songId, bitrate)}.download")
    }

    // 未选 SAF 时的私有正式文件路径
    fun privateTargetFile(songId: Long, bitrate: Int, extension: String): File {
        val safeExt = extension.trim('.').ifBlank { "mp3" }
        return File(privateDir, "${fileStem(songId, bitrate)}.$safeExt")
    }

    // 临时文件落盘为正式文件，返回可供播放的 localPath（file 路径或 content URI）
    suspend fun commitDownload(
        songId: Long,
        bitrate: Int,
        extension: String,
        tempFile: File
    ): String {
        val safeExt = extension.trim('.').ifBlank { "mp3" }
        val treeUri = settingsRepository.getValue(SettingKeys.DOWNLOAD_STORAGE_TREE_URI)
            ?.takeIf { it.isNotBlank() }

        if (treeUri == null) {
            val target = privateTargetFile(songId, bitrate, safeExt)
            if (target.exists()) target.delete()
            if (!tempFile.renameTo(target)) {
                tempFile.copyTo(target, overwrite = true)
                tempFile.delete()
            }
            return target.absolutePath
        }

        val tree = DocumentFile.fromTreeUri(context, Uri.parse(treeUri))
            ?: throw IllegalStateException("无法访问所选下载目录，请重新选择")
        if (!tree.canWrite()) {
            throw IllegalStateException("所选下载目录不可写，请重新选择")
        }

        val displayName = "${fileStem(songId, bitrate)}.$safeExt"
        // 同名先删，避免 createFile 自动改成 name (1).ext
        tree.findFile(displayName)?.delete()
        val mime = mimeForExtension(safeExt)
        val created = tree.createFile(mime, displayName)
            ?: throw IllegalStateException("无法在所选目录创建文件")
        // 部分实现会改名，以实际 URI 为准
        context.contentResolver.openOutputStream(created.uri, "w")?.use { output ->
            tempFile.inputStream().use { input -> input.copyTo(output) }
        } ?: throw IllegalStateException("无法写入所选目录")
        tempFile.delete()
        return created.uri.toString()
    }

    fun exists(localPath: String): Boolean {
        return if (isContentUri(localPath)) {
            DocumentFile.fromSingleUri(context, Uri.parse(localPath))?.exists() == true
        } else {
            File(localPath).isFile
        }
    }

    // 删除正式文件（私有路径或 content URI）以及对应临时文件
    fun deleteForSong(songId: Long, bitrate: Int, localPath: String?) {
        tempFile(songId, bitrate).delete()
        if (!localPath.isNullOrBlank()) {
            deleteLocal(localPath)
            return
        }
        // 无路径时清掉私有目录下同 stem 的残留
        val stem = fileStem(songId, bitrate)
        privateDir.listFiles()
            ?.filter { it.name.startsWith("$stem.") || it.name == "$stem.download" }
            ?.forEach { it.delete() }
    }

    fun deleteLocal(localPath: String) {
        if (isContentUri(localPath)) {
            DocumentFile.fromSingleUri(context, Uri.parse(localPath))?.delete()
        } else {
            File(localPath).delete()
        }
    }

    private fun fileStem(songId: Long, bitrate: Int): String = "${songId}_$bitrate"

    private fun isContentUri(path: String): Boolean =
        path.startsWith("content:", ignoreCase = true)

    private fun mimeForExtension(ext: String): String = when (ext.lowercase()) {
        "flac" -> "audio/flac"
        "m4a", "aac" -> "audio/mp4"
        "ogg" -> "audio/ogg"
        "wav" -> "audio/wav"
        else -> "audio/mpeg"
    }

    companion object {
        private const val DIR_NAME = "music_downloads"

        // 将 ExternalStorageProvider 的 tree URI 转为绝对路径；无法识别则 null
        // 例：primary:Music → /storage/emulated/0/Music
        fun resolveTreeUriToAbsolutePath(treeUri: Uri): String? {
            if (treeUri.authority != "com.android.externalstorage.documents") return null
            val docId = runCatching {
                android.provider.DocumentsContract.getTreeDocumentId(treeUri)
            }.getOrNull() ?: return null
            val split = docId.split(":", limit = 2)
            if (split.isEmpty()) return null
            val volume = split[0]
            val relative = split.getOrNull(1).orEmpty()
            val root = when {
                volume.equals("primary", ignoreCase = true) ->
                    android.os.Environment.getExternalStorageDirectory().absolutePath
                else -> "/storage/$volume"
            }
            return if (relative.isEmpty()) root else "$root/$relative".replace("//", "/")
        }
    }
}
