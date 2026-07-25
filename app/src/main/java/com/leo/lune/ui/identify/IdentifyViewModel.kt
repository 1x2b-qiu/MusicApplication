package com.leo.lune.ui.identify

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.audio.AudioSnippetRecorder
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.domain.model.IdentifyMatchResult
import com.leo.lune.domain.model.RecognizedTrack
import com.leo.lune.domain.model.Song
import com.leo.lune.domain.usecase.identify.RecognizeSongUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

// 听歌识曲页阶段（对齐设计稿 idle / listening / processing / result）
enum class IdentifyPhase {
    // 等待用户点击开始
    Idle,
    // 正在麦克风录音（最长 RECORD_SECONDS）
    Listening,
    // 录音结束，正在上传 AudD 并搜网易云
    Processing,
    // AudD 命中，并已带上网易云候选列表
    Success,
    // 未识别 / 录音失败 / 网络错误
    Failure,
}

// 听歌识曲页 UI 状态
data class IdentifyUiState(
    // 当前阶段，驱动界面切换
    val phase: IdentifyPhase = IdentifyPhase.Idle,
    // 聆听中已过秒数（用于计时文案 00:xx）
    val elapsedSeconds: Int = 0,
    // 实际录了多少秒（成功卡「识别片段」展示；满录一般为 8）
    val recordedSeconds: Int = 0,
    // AudD 返回的歌名/歌手等元数据
    val track: RecognizedTrack? = null,
    // 用「歌名 歌手」在网易云搜到的候选；首条作主结果，其余进「其他识曲结果」
    val songs: List<Song> = emptyList(),
    // 「其他识曲结果」列表是否展开
    val othersExpanded: Boolean = false,
    // 失败时的补充说明；为 null 时 UI 用默认文案「请靠近音源后再试一次」
    val failureMessage: String? = null,
)

// 听歌识曲 ViewModel：录音 8 秒 → AudD 识别 → 网易云匹配 → 播放
@HiltViewModel
class IdentifyViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val recognizeSongUseCase: RecognizeSongUseCase,
    private val playerController: MusicPlayerController,
) : ViewModel() {

    // 短录音工具；用 ApplicationContext 避免持有 Activity
    private val recorder = AudioSnippetRecorder(context.applicationContext)
    private val _uiState = MutableStateFlow(IdentifyUiState())
    val uiState: StateFlow<IdentifyUiState> = _uiState.asStateFlow()

    // 聆听倒计时协程（每秒刷新 elapsedSeconds，满 8 秒触发识别）
    private var listenJob: Job? = null
    // 上传识别协程（可与新一轮聆听互斥取消）
    private var recognizeJob: Job? = null

    // 主按钮：空闲/结果页 → 开始录；聆听中 → 取消；识别中忽略（防打断上传）
    fun onPrimaryAction() {
        when (_uiState.value.phase) {
            IdentifyPhase.Idle,
            IdentifyPhase.Success,
            IdentifyPhase.Failure -> startListening()
            IdentifyPhase.Listening -> cancelListening()
            IdentifyPhase.Processing -> Unit
        }
    }

    // 展开/收起「其他识曲结果」
    fun toggleOthersExpanded() {
        _uiState.update { it.copy(othersExpanded = !it.othersExpanded) }
    }

    // 播放指定候选；队列为本次识别的全部 songs
    @RequiresApi(Build.VERSION_CODES.O)
    fun playSong(song: Song) {
        val queue = _uiState.value.songs.ifEmpty { listOf(song) }
        playerController.playSong(song, queue)
        playerController.setPreviewSong(song)
    }

    // 播放主结果（songs 首条）
    @RequiresApi(Build.VERSION_CODES.O)
    fun playPrimary() {
        val song = _uiState.value.songs.firstOrNull() ?: return
        playSong(song)
    }

    // 「重新识别 / 再次识别」：再开一轮聆听
    fun retry() {
        startListening()
    }

    // 开始麦克风录音，并启动 8 秒计时；满时自动 stop 并识别
    private fun startListening() {
        // 打断上一轮：识别请求、计时、以及未结束的录音
        recognizeJob?.cancel()
        listenJob?.cancel()
        recorder.cancel()

        val started = runCatching { recorder.start() }
        if (started.isFailure) {
            // 常见原因：未授麦克风权限、设备占用等
            _uiState.update {
                it.copy(
                    phase = IdentifyPhase.Failure,
                    failureMessage = "无法启动麦克风，请检查权限后重试",
                    track = null,
                    songs = emptyList(),
                    othersExpanded = false,
                    elapsedSeconds = 0,
                    recordedSeconds = 0,
                )
            }
            return
        }

        // 重置为干净的聆听态（清掉上一轮结果）
        _uiState.update {
            IdentifyUiState(phase = IdentifyPhase.Listening, elapsedSeconds = 0)
        }

        listenJob = viewModelScope.launch {
            var elapsed = 0
            // 每秒 +1，直到满 RECORD_SECONDS 或被 cancelListening 取消
            while (isActive && elapsed < RECORD_SECONDS) {
                delay(1_000)
                elapsed += 1
                _uiState.update { state ->
                    // 仅在仍处于 Listening 时刷新，避免与取消/识别态打架
                    if (state.phase == IdentifyPhase.Listening) {
                        state.copy(elapsedSeconds = elapsed)
                    } else {
                        state
                    }
                }
            }
            // 自然满 8 秒且仍在聆听 → 停录并上传；手动取消不会走到这里
            if (isActive && _uiState.value.phase == IdentifyPhase.Listening) {
                finishAndRecognize(recordedSeconds = RECORD_SECONDS)
            }
        }
    }

    // 用户中途点停止：丢弃录音，回到初始空闲态
    private fun cancelListening() {
        listenJob?.cancel()
        listenJob = null
        recorder.cancel()
        _uiState.update { IdentifyUiState() }
    }

    // 停止录音 → Processing → 调 RecognizeSongUseCase（AudD + cloudsearch）
    private fun finishAndRecognize(recordedSeconds: Int) {
        listenJob?.cancel()
        listenJob = null

        val file = recorder.stop()
        if (file == null) {
            // stop 失败或文件为空（录得太短/编码器异常）
            _uiState.update {
                it.copy(
                    phase = IdentifyPhase.Failure,
                    failureMessage = "录音失败，请再试一次",
                    recordedSeconds = recordedSeconds,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                phase = IdentifyPhase.Processing,
                recordedSeconds = recordedSeconds,
                elapsedSeconds = recordedSeconds,
            )
        }

        recognizeJob = viewModelScope.launch {
            runCatching {
                recognizeSongUseCase(file.absolutePath)
            }.onSuccess { result ->
                // 识别结束即删临时文件，无论成败
                file.delete()
                when (result) {
                    // AudD 命中：track 为元数据，songs 为网易云候选
                    is IdentifyMatchResult.Matched -> {
                        _uiState.update {
                            it.copy(
                                phase = IdentifyPhase.Success,
                                track = result.track,
                                songs = result.songs,
                                othersExpanded = false,
                                failureMessage = null,
                            )
                        }
                    }
                    // AudD 无匹配（非网络错误）
                    IdentifyMatchResult.NotRecognized -> {
                        _uiState.update {
                            it.copy(
                                phase = IdentifyPhase.Failure,
                                track = null,
                                songs = emptyList(),
                                othersExpanded = false,
                                failureMessage = null,
                            )
                        }
                    }
                }
            }.onFailure { error ->
                file.delete()
                // Token/网络/服务端错误等：把异常信息透出给 UI
                _uiState.update {
                    it.copy(
                        phase = IdentifyPhase.Failure,
                        track = null,
                        songs = emptyList(),
                        othersExpanded = false,
                        failureMessage = error.message ?: "识别失败，请稍后重试",
                    )
                }
            }
        }
    }

    // 页面销毁：停协程并释放麦克风，避免泄漏
    override fun onCleared() {
        listenJob?.cancel()
        recognizeJob?.cancel()
        recorder.cancel()
        super.onCleared()
    }

    companion object {
        // 默认录音时长（秒）；满此时长自动提交识别
        const val RECORD_SECONDS = 8
    }
}
