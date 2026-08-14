package com.leo.lune.local

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

// 扫描到的本地音频（仅内存，不落库）
data class ScannedLocalTrack(
    // content:// 或 file URI
    val uri: String,
    // 文档显示名（含扩展名）
    val displayName: String,
    val title: String,
    val artists: String,
    val album: String,
    val durationMs: Long,
    val fileSizeBytes: Long
)

// 自定义文件夹扫描：SAF DocumentFile 递归 + MediaMetadataRetriever 读标签
@Singleton
class LocalFolderAudioScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // 从用户选中的 tree URI 递归扫描音频；isCancelled 为 true 时提前结束并返回已扫结果
    suspend fun scan(
        treeUri: Uri,
        isCancelled: () -> Boolean = { false },
        onTrackFound: (ScannedLocalTrack) -> Unit = {}
    ): List<ScannedLocalTrack> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext emptyList()
        val results = mutableListOf<ScannedLocalTrack>()
        walk(root, results, isCancelled, onTrackFound)
        results
    }

    private suspend fun walk(
        dir: DocumentFile,
        out: MutableList<ScannedLocalTrack>,
        isCancelled: () -> Boolean,
        onTrackFound: (ScannedLocalTrack) -> Unit
    ) {
        if (isCancelled()) return
        coroutineContext.ensureActive()
        val children = dir.listFiles()
        for (child in children) {
            if (isCancelled()) return
            coroutineContext.ensureActive()
            when {
                child.isDirectory -> walk(child, out, isCancelled, onTrackFound)
                child.isFile && isAudioFile(child) -> {
                    val track = readTrack(child)
                    out += track
                    onTrackFound(track)
                }
            }
        }
    }

    private fun isAudioFile(file: DocumentFile): Boolean {
        val name = file.name.orEmpty()
        val ext = name.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
        if (ext in AudioExtensions) return true
        val mime = file.type.orEmpty().lowercase()
        return mime.startsWith("audio/")
    }

    private fun readTrack(file: DocumentFile): ScannedLocalTrack {
        val uri = file.uri
        val displayName = file.name.orEmpty().ifBlank { "未知文件" }
        val fallbackTitle = displayName.substringBeforeLast('.')
            .ifBlank { displayName }
        var title = fallbackTitle
        var artists = ""
        var album = ""
        var durationMs = 0L

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: fallbackTitle
            artists = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                .orEmpty()
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                .orEmpty()
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
        } catch (_: Throwable) {
            // 标签读取失败时仍保留文件名作为标题
        } finally {
            runCatching { retriever.release() }
        }

        return ScannedLocalTrack(
            uri = uri.toString(),
            displayName = displayName,
            title = title,
            artists = artists,
            album = album,
            durationMs = durationMs,
            fileSizeBytes = file.length().coerceAtLeast(0L)
        )
    }

    companion object {
        private val AudioExtensions = setOf(
            "mp3", "flac", "m4a", "aac", "wav", "ogg", "opus", "wma", "ape", "aiff", "aif"
        )
    }
}
