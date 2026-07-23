package com.leo.lune.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface MusicRoute {

    @Serializable
    data object Splash : MusicRoute

    @Serializable
    data object Home : MusicRoute

    @Serializable
    data object Radio : MusicRoute

    @Serializable
    data object Category : MusicRoute

    @Serializable
    data object Settings : MusicRoute

    @Serializable
    data object Search : MusicRoute

    @Serializable
    data object Liked : MusicRoute

    @Serializable
    data object Recent : MusicRoute

    @Serializable
    data object Downloads : MusicRoute

    @Serializable
    data object Login : MusicRoute

    @Serializable
    data object Player : MusicRoute
}
