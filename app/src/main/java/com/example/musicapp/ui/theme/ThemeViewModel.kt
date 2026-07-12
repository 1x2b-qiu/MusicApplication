package com.example.musicapp.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.model.ThemeSetting
import com.example.musicapp.domain.usecase.ObserveThemeSettingUseCase
import com.example.musicapp.domain.usecase.SetUserThemeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// 全局主题 ViewModel：读取 DataStore 并在用户切换时持久化
@HiltViewModel
class ThemeViewModel @Inject constructor(
    observeThemeSettingUseCase: ObserveThemeSettingUseCase,
    private val setUserThemeUseCase: SetUserThemeUseCase
) : ViewModel() {

    val themeSetting: StateFlow<ThemeSetting> = observeThemeSettingUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeSetting.FollowSystem
        )

    fun setUserTheme(darkTheme: Boolean) {
        viewModelScope.launch {
            setUserThemeUseCase(darkTheme)
        }
    }
}
