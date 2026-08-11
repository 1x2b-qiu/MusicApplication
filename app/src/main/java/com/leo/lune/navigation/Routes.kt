package com.leo.lune.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface MusicRoute {

    // 已登录主壳（曲库 / 电台 / 我的）
    @Serializable
    data object Main : MusicRoute

    @Serializable
    data object Settings : MusicRoute

    @Serializable
    data object DownloadSettings : MusicRoute

    @Serializable
    data object PlaybackSettings : MusicRoute

    @Serializable
    data object Search : MusicRoute

    @Serializable
    data object Liked : MusicRoute

    @Serializable
    data object DailyMix : MusicRoute

    @Serializable
    data object PlaylistPlaza : MusicRoute

    @Serializable
    data class PlaylistDetail(val playlistId: Long) : MusicRoute

    @Serializable
    data object Recent : MusicRoute

    @Serializable
    data object Downloads : MusicRoute

    @Serializable
    data object Identify : MusicRoute

    @Serializable
    data object Login : MusicRoute

    @Serializable
    data object Player : MusicRoute
}
