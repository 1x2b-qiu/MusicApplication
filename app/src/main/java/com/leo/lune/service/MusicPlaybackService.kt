package com.leo.lune.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.leo.lune.MainActivity
import com.leo.lune.R
import com.leo.lune.controller.MusicPlayerController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// 前台媒体服务：托管 MediaSession，提供通知栏 / 锁屏 / 耳机控制
// ExoPlayer 仍由 MusicPlayerController 持有，本服务只包装 Session；
// 服务启动后立即发布「准备中」前台通知，满足 startForegroundService 5 秒限制；
// 真正开播后由 Media3 替换为媒体播放通知
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

    // 标记「准备中」前台通知是否仍在展示；播放开始后需移除以避免与 Media3 通知重复
    private var isShowingPreparingNotification = false

    // 监听播放状态变化：管理锁 + 移除准备中通知
    private val playbackLockListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                acquireWifiLock()
                acquireWakeLock()
                // 真正开播 → 移除「准备中」通知，Media3 随即发布媒体通知
                dismissPreparingNotification()
            } else {
                releaseWifiLock()
                releaseWakeLock()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val player = playerController.player

        // 立即发布「准备中」前台通知，满足 startForegroundService 的 5 秒限制
        postPreparingNotification()

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

        // 若服务启动时播放器已在播放（如进程恢复），立即持锁并移除准备通知
        if (player.isPlaying) {
            acquireWifiLock()
            acquireWakeLock()
            dismissPreparingNotification()
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

    // 用户从最近任务划掉 App 时触发：
    // 正在播放 → 脱离前台但保持服务运行（通知栏继续展示媒体控件，播放不中断）
    // 未在播放 → 交给父类默认行为（服务随任务一起移除）
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = playerController.player
        if (player.isPlaying || player.playbackState != Player.STATE_IDLE) {
            // 脱离前台：通知栏从「前台通知」降级为「媒体会话通知」继续驻留
            // 服务不再受前台保护，但 MediaSession 仍持有，系统不会立即回收
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            super.onTaskRemoved(rootIntent)
        }
    }

    override fun onDestroy() {
        // 移除监听，避免泄漏
        playerController.player.removeListener(playbackLockListener)
        dismissPreparingNotification()
        releaseWifiLock()
        releaseWakeLock()
        wifiLock = null
        wakeLock = null
        // 释放 Session，避免泄漏；不 release ExoPlayer（由 Controller 单例持有）
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    // ── 准备中通知 ─────────────────────────────────────────────

    // 服务启动后立即发布「准备中」前台通知，满足 startForegroundService 5 秒限制
    private fun postPreparingNotification() {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("正在准备播放…")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(NOTIFICATION_ID_PREPARING, notification)
        isShowingPreparingNotification = true
    }

    // 真正开播后移除「准备中」通知；Media3 随即发布媒体播放通知接管前台
    private fun dismissPreparingNotification() {
        if (!isShowingPreparingNotification) return
        isShowingPreparingNotification = false
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    // 确保通知渠道存在（Android 8.0+）
    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "媒体播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "播放控制与媒体通知"
                setShowBadge(false)
            }
        )
    }

    // ── 锁管理 ─────────────────────────────────────────────────

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

    private companion object {
        const val CHANNEL_ID = "media_playback"
        const val NOTIFICATION_ID_PREPARING = 1001
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
