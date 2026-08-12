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
    data object Charts : MusicRoute

    @Serializable
    data class ChartDetail(val chartId: Long) : MusicRoute

    @Serializable
    data class GenreDetail(
        val styleId: Long,
        val styleName: String = ""
    ) : MusicRoute

    @Serializable
    data class PlaylistDetail(
        val playlistId: Long,
        // 入口预览：进页即可展示，详情接口返回后覆盖
        val playlistName: String = "",
        val coverUrl: String = "",
        val trackCount: Int = 0
    ) : MusicRoute

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
