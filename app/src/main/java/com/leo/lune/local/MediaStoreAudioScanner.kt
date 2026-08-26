package com.leo.lune.local

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

// 全盘扫描：一次游标查询 MediaStore 全设备音频
// 标签/时长已由系统索引，无需逐文件解析，远快于 SAF 递归 + MediaMetadataRetriever
@Singleton
class MediaStoreAudioScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // 扫描全设备音频；isCancelled 为 true 时提前结束并返回已扫结果
    suspend fun scan(
        isCancelled: () -> Boolean = { false },
        onTrackFound: (ScannedLocalTrack) -> Unit = {}
    ): List<ScannedLocalTrack> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScannedLocalTrack>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
        )
        // 排除铃声 / 闹钟 / 通知音等非音乐项
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        context.contentResolver
            .query(collection, projection, selection, null, sortOrder)
            ?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                while (cursor.moveToNext()) {
                    if (isCancelled()) break
                    coroutineContext.ensureActive()
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                        ?.ifBlank { "未知文件" }
                        ?: "未知文件"
                    val title = cursor.getString(titleColumn)
                        ?.takeIf { it.isNotBlank() }
                        ?: displayName.substringBeforeLast('.').ifBlank { displayName }
                    val track = ScannedLocalTrack(
                        uri = ContentUris.withAppendedId(collection, id).toString(),
                        displayName = displayName,
                        title = title,
                        artists = cursor.getString(artistColumn).orEmpty(),
                        album = cursor.getString(albumColumn).orEmpty(),
                        durationMs = cursor.getLong(durationColumn).coerceAtLeast(0L),
                        fileSizeBytes = cursor.getLong(sizeColumn).coerceAtLeast(0L)
                    )
                    results += track
                    onTrackFound(track)
                }
            }
        results
    }

    companion object {
        // MediaStore 导入来源的哨兵 treeUri：不依赖 SAF 授权目录，权限随运行时授权生效
        const val MediaStoreTreeUri = "mediastore://external"
    }
}
