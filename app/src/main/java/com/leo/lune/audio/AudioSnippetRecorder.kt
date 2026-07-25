package com.leo.lune.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

// 听歌识曲短录音：输出 AAC/M4A 到 cache，供 AudD 上传
class AudioSnippetRecorder(
    private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean
        get() = recorder != null

    // 开始录音，返回输出文件路径
    fun start(): File {
        stopInternal(deleteFile = true)

        val file = File(context.cacheDir, "identify_${System.currentTimeMillis()}.m4a")
        val mediaRecorder = createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(128_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        outputFile = file
        return file
    }

    // 正常结束：停止并返回文件；失败返回 null
    fun stop(): File? {
        val file = outputFile
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            outputFile = null
            file?.takeIf { it.isFile && it.length() > 0L }
        } catch (_: RuntimeException) {
            recorder?.release()
            recorder = null
            outputFile = null
            file?.delete()
            null
        }
    }

    // 取消：停止并删除临时文件
    fun cancel() {
        stopInternal(deleteFile = true)
    }

    private fun stopInternal(deleteFile: Boolean) {
        try {
            recorder?.apply {
                runCatching { stop() }
                release()
            }
        } catch (_: Exception) {
            // ignore
        } finally {
            recorder = null
        }
        if (deleteFile) {
            outputFile?.delete()
        }
        outputFile = null
    }

    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
}
