package com.example.musicapp.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MusicPlayerControllerEntryPoint {
    fun musicPlayerController(): MusicPlayerController
}

@Composable
fun rememberMusicPlayerController(): MusicPlayerController {
    val application = LocalContext.current.applicationContext
    return remember(application) {
        EntryPointAccessors.fromApplication(
            application,
            MusicPlayerControllerEntryPoint::class.java
        ).musicPlayerController()
    }
}
