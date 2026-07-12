package com.example.musicapp.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface MusicRoute {

    @Serializable
    data object Home : MusicRoute

    @Serializable
    data object Radio : MusicRoute

    @Serializable
    data object Profile : MusicRoute

    @Serializable
    data object Search : MusicRoute

    @Serializable
    data object Login : MusicRoute

    @Serializable
    data class Player(
        val songId: Long,
        val songName: String,
        val artistName: String,
        val coverUrl: String = ""
    ) : MusicRoute
}
