package com.leo.lune.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.leo.lune.MainActivity
import com.leo.lune.controller.MusicPlayerController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// 前台媒体服务：托管 MediaSession，提供通知栏 / 锁屏 / 耳机控制
// ExoPlayer 仍由 MusicPlayerController 持有，本服务只包装 Session；
// 真正开播时由 Media3 升为前台，避免拉 URL 期间 startForeground 超时
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    // Hilt 注入的全局播放控制器；从中取共享 ExoPlayer 并转发上下首
    @Inject
    lateinit var playerController: MusicPlayerController

    // 系统媒体会话；通知栏与外部控制器通过它操作播放器
    private var mediaSession: MediaSession? = null

    // WiFi 锁：在线流媒体播放期间持有，防止 Doze 模式下 WiFi 被挂起导致缓冲中断
    private var wifiLock: WifiManager.WifiLock? = null

    // CPU 唤醒锁：播放期间保持 CPU 活跃，防止锁屏后解码线程被挂起
    private var wakeLock: PowerManager.WakeLock? = null

    // 监听播放状态变化，按需获取 / 释放 WiFi 锁与 CPU 唤醒锁
    private val playbackLockListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                acquireWifiLock()
                acquireWakeLock()
            } else {
                releaseWifiLock()
                releaseWakeLock()
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val player = playerController.player

        // 初始化 WiFi 锁（懒获取，播放时才持有）
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "MusicApp::PlaybackWifiLock"
        ).apply { setReferenceCounted(false) }

        // 初始化 CPU 唤醒锁（懒获取，播放时才持有）
        // PARTIAL_WAKE_LOCK：仅保持 CPU 运行，不亮屏，适合纯音频后台播放
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MusicApp::PlaybackWakeLock"
        ).apply { setReferenceCounted(false) }

        // 若服务启动时播放器已在播放（如进程恢复），立即持锁
        if (player.isPlaying) {
            acquireWifiLock()
            acquireWakeLock()
        }
        player.addListener(playbackLockListener)

        // 用 ForwardingPlayer 包装：系统「上一首/下一首」接到业务队列切歌
        val sessionPlayer = QueueAwarePlayer(
            player = player,
            onNext = { playerController.skipToNext() },
            onPrevious = { playerController.skipToPrevious() },
        )
        mediaSession = MediaSession.Builder(this, sessionPlayer)
            // 点击通知回到 App 主界面
            .setSessionActivity(sessionActivityPendingIntent())
            .build()
    }

    // 向 Media3 / 系统返回当前 Session；无 Session 时外部无法控制
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        // 移除监听，避免泄漏
        playerController.player.removeListener(playbackLockListener)
        releaseWifiLock()
        releaseWakeLock()
        wifiLock = null
        wakeLock = null
        // 释放 Session，避免泄漏；不 release ExoPlayer（由 Controller 单例持有）
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    // 获取 WiFi 锁：在线流媒体播放期间保持 WiFi 活跃，防止 Doze 切断网络
    private fun acquireWifiLock() {
        wifiLock?.let { if (!it.isHeld) it.acquire() }
    }

    // 释放 WiFi 锁：暂停 / 停止播放后交还，避免无谓耗电
    private fun releaseWifiLock() {
        wifiLock?.let { if (it.isHeld) it.release() }
    }

    // 获取 CPU 唤醒锁：播放期间保持 CPU 活跃，防止锁屏后解码线程休眠
    private fun acquireWakeLock() {
        wakeLock?.let { if (!it.isHeld) it.acquire() }
    }

    // 释放 CPU 唤醒锁：暂停 / 停止播放后交还，避免无谓耗电
    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    // 通知点击：回到 MainActivity（SINGLE_TOP，避免重复堆栈）
    private fun sessionActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

// 把系统上一首 / 下一首接到业务队列切歌（Shuffle / Loop 等模式在 Controller）；
// ExoPlayer 播放列表仅承载「当前曲 + 预取的下一首」，完整业务队列仍由 Controller 维护
@UnstableApi
private class QueueAwarePlayer(
    player: ExoPlayer,
    // 业务层下一首（含 Shuffle / Loop 等模式）
    private val onNext: () -> Unit,
    // 业务层上一首
    private val onPrevious: () -> Unit,
) : androidx.media3.common.ForwardingPlayer(player) {

    // 声明支持上下首命令，否则通知栏可能不显示对应按钮
    override fun getAvailableCommands(): Player.Commands {
        return super.getAvailableCommands()
            .buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .build()
    }

    // 上下首命令始终可用（队列逻辑由 Controller 处理，含空队列边界）
    override fun isCommandAvailable(command: @Player.Command Int): Boolean {
        return when (command) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
            else -> super.isCommandAvailable(command)
        }
    }

    // 系统 seekToNext* 统一转发到业务 skipToNext
    override fun seekToNext() = onNext()

    override fun seekToNextMediaItem() = onNext()

    // 系统 seekToPrevious* 统一转发到业务 skipToPrevious
    override fun seekToPrevious() = onPrevious()

    override fun seekToPreviousMediaItem() = onPrevious()

    // 始终回报有上下首，让系统 UI 保持按钮可点
    override fun hasNextMediaItem(): Boolean = true

    override fun hasPreviousMediaItem(): Boolean = true
}
