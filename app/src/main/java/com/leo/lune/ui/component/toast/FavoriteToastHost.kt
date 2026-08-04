package com.leo.lune.ui.component.toast

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.leo.lune.manager.FavoriteManager
import com.leo.lune.manager.FavoriteResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 红心操作 Toast 状态（带 key，保证连续同文案也能重新弹出）
data class FavoriteToastUi(
    val message: String,
    val type: AppToastType,
    val key: Long
)

@HiltViewModel
class FavoriteToastViewModel @Inject constructor(
    favoriteManager: FavoriteManager
) : ViewModel() {

    private val _toast = MutableStateFlow<FavoriteToastUi?>(null)
    val toast: StateFlow<FavoriteToastUi?> = _toast.asStateFlow()

    init {
        viewModelScope.launch {
            favoriteManager.results.collect { result ->
                _toast.value = when (result) {
                    is FavoriteResult.Success -> FavoriteToastUi(
                        message = if (result.liked) "已添加到我喜欢的" else "已取消喜欢",
                        type = AppToastType.Success,
                        key = System.nanoTime()
                    )
                    is FavoriteResult.Failure -> FavoriteToastUi(
                        message = result.message,
                        type = AppToastType.Error,
                        key = System.nanoTime()
                    )
                }
            }
        }
    }

    fun dismiss() {
        _toast.value = null
    }
}

// 全局红心结果 Toast 宿主：挂在根布局居中
@Composable
fun BoxScope.FavoriteToastHost(
    darkTheme: Boolean,
    viewModel: FavoriteToastViewModel = hiltViewModel()
) {
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val current = toast

    // key 变化时强制重建，避免同 visible 状态下动画/计时不刷新
    key(current?.key) {
        AppToast(
            visible = current != null,
            message = current?.message.orEmpty(),
            type = current?.type ?: AppToastType.Default,
            darkTheme = darkTheme,
            onDismiss = viewModel::dismiss,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
