package com.leo.lune.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

// 挂在页面根 Modifier 上：非 RESUMED（被盖住 / 退场中）时在 Initial 阶段消费指针，
// 子组件 clickable 不再响应——对应官方「非前台忽略交互」。
@Composable
fun Modifier.consumePointersUnlessResumed(): Modifier {
    val state by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    if (state.isAtLeast(Lifecycle.State.RESUMED)) return this
    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial)
                    .changes
                    .forEach { it.consume() }
            }
        }
    }
}
