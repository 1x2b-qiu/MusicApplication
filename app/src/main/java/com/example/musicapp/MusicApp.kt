package com.example.musicapp

import android.app.Application
import com.example.musicapp.domain.usecase.RestoreSessionUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class MusicApp : Application() {

    @Inject
    lateinit var restoreSessionUseCase: RestoreSessionUseCase

    override fun onCreate() {
        super.onCreate()
        runBlocking {
            restoreSessionUseCase()
        }
    }
}
