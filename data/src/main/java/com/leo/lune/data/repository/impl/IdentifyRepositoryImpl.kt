package com.leo.lune.data.repository.impl

import com.leo.lune.data.BuildConfig
import com.leo.lune.data.remote.api.AuddApi
import com.leo.lune.domain.model.RecognizedTrack
import com.leo.lune.domain.repository.IdentifyRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// AudD 听歌识曲仓储实现
@Singleton
class IdentifyRepositoryImpl @Inject constructor(
    private val auddApi: AuddApi,
) : IdentifyRepository {

    override suspend fun recognize(audioPath: String): RecognizedTrack? {
        val token = BuildConfig.AUDD_API_TOKEN
        if (token.isBlank()) {
            throw IllegalStateException(
                "AUDD_API_TOKEN is empty. Add it to local.properties and rebuild."
            )
        }

        val file = File(audioPath)
        if (!file.isFile || file.length() <= 0L) {
            throw IllegalArgumentException("Audio file missing or empty: $audioPath")
        }

        val mediaType = guessMediaType(file.name)
        val filePart = MultipartBody.Part.createFormData(
            name = "file",
            filename = file.name,
            body = file.asRequestBody(mediaType),
        )
        val tokenBody = token.toRequestBody("text/plain".toMediaTypeOrNull())

        val response = auddApi.recognize(apiToken = tokenBody, file = filePart)

        if (response.status.equals("error", ignoreCase = true)) {
            val code = response.error?.errorCode
            val message = response.error?.errorMessage.orEmpty()
            throw IllegalStateException(
                "AudD recognize failed" +
                    (if (code != null) " (#$code)" else "") +
                    (if (message.isNotBlank()) ": $message" else "")
            )
        }

        val result = response.result ?: return null
        val title = result.title?.trim().orEmpty()
        val artist = result.artist?.trim().orEmpty()
        if (title.isEmpty() && artist.isEmpty()) return null

        return RecognizedTrack(
            title = title,
            artist = artist,
            album = result.album?.trim()?.takeIf { it.isNotEmpty() },
            songLink = result.songLink?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun guessMediaType(fileName: String) =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "m4a", "aac", "mp4" -> "audio/mp4".toMediaTypeOrNull()
            "mp3" -> "audio/mpeg".toMediaTypeOrNull()
            "wav" -> "audio/wav".toMediaTypeOrNull()
            "ogg" -> "audio/ogg".toMediaTypeOrNull()
            else -> "application/octet-stream".toMediaTypeOrNull()
        }
}
