package com.leo.lune.ui.component.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

// 弹窗卡片 / 按钮圆角（对齐参考 GlassDialog rounded-2xl / rounded-xl）
private val CardShape = RoundedCornerShape(16.dp)
private val ButtonShape = RoundedCornerShape(12.dp)

// 全局确认弹窗宿主：挂在导航根，卡片采样根 Haze
@Composable
fun BoxScope.AppConfirmDialogHost(
    hazeState: HazeState,
    darkTheme: Boolean,
    controller: ConfirmDialogController = rememberConfirmDialogController()
) {
    val request by controller.request.collectAsStateWithLifecycle()
    val current = request

    AppConfirmDialog(
        visible = current != null,
        title = current?.title.orEmpty(),
        message = current?.message,
        confirmLabel = current?.confirmLabel ?: "确认",
        cancelLabel = current?.cancelLabel ?: "取消",
        danger = current?.danger == true,
        darkTheme = darkTheme,
        hazeState = hazeState,
        onConfirm = {
            val action = current?.onConfirm
            controller.dismiss()
            action?.invoke()
        },
        onDismiss = {
            val action = current?.onDismiss
            controller.dismiss()
            action?.invoke()
        }
    )
}

// 全局确认弹窗：半透明置灰遮罩 + 居中磨砂卡片 + 取消/确认
@Composable
fun AppConfirmDialog(
    // 是否展示
    visible: Boolean,
    // 标题
    title: String,
    // 可选说明文案
    message: String? = null,
    // 确认按钮文案
    confirmLabel: String = "确认",
    // 取消按钮文案
    cancelLabel: String = "取消",
    // 确认按钮是否用危险色（如退出登录）
    danger: Boolean = false,
    // 深浅色，影响遮罩与文案配色
    darkTheme: Boolean,
    // 根布局 Haze，与 MiniPlayerBar 同源采样
    hazeState: HazeState,
    // 点确认
    onConfirm: () -> Unit,
    // 点取消 / 系统返回
    onDismiss: () -> Unit
) {
    // 弹窗打开时拦截返回键，走关闭而非退出页面
    BackHandler(enabled = visible) { onDismiss() }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(160)),
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        // 全屏置灰遮罩：拦截下层点击；点空白处不关闭（仅取消/返回可关）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (darkTheme) Color.Black.copy(alpha = 0.5f)
                    else Color.Black.copy(alpha = 0.28f)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            contentAlignment = Alignment.Center
        ) {
            ConfirmDialogCard(
                title = title,
                message = message,
                confirmLabel = confirmLabel,
                cancelLabel = cancelLabel,
                danger = danger,
                darkTheme = darkTheme,
                hazeState = hazeState,
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        }
    }
}

// 居中确认卡片：MiniPlayerBar 同款磨砂 + 两个独立按钮
@Composable
private fun AnimatedVisibilityScope.ConfirmDialogCard(
    title: String,
    message: String?,
    confirmLabel: String,
    cancelLabel: String,
    danger: Boolean,
    darkTheme: Boolean,
    hazeState: HazeState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val colors = remember(darkTheme) { ConfirmDialogColors(darkTheme) }

    Column(
        modifier = Modifier
            .widthIn(max = 284.dp)
            .fillMaxWidth()
            .animateEnterExit(
                enter = fadeIn(tween(200)) +
                    scaleIn(
                        initialScale = 0.88f,
                        animationSpec = spring(
                            dampingRatio = 0.78f,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) +
                    slideInVertically(tween(280)) { (it * 0.08f).toInt() },
                exit = fadeOut(tween(160)) +
                    scaleOut(targetScale = 0.88f, animationSpec = tween(160)) +
                    slideOutVertically(tween(160)) { (it * 0.05f).toInt() }
            )
            .shadow(
                elevation = 8.dp,
                shape = CardShape,
                spotColor = Color(0xB3000000),
                ambientColor = Color(0xB3000000)
            )
            .clip(CardShape)
            .hazeEffect(state = hazeState) {
                blurRadius = 24.dp
                tints = listOf(
                    HazeTint(colorScheme.background.copy(alpha = 0.5f)),
                    HazeTint(Color.White.copy(alpha = 0.08f))
                )
                noiseFactor = 0.15f
            }
            .border(1.dp, Color(0x26FFFFFF), CardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Box {
            // 顶部细高光
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x66FFFFFF),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = colors.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(top = 6.dp),
                        color = colors.message,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(ButtonShape)
                    .background(colors.cancelButton)
                    .border(1.dp, colors.cancelBorder, ButtonShape)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cancelLabel,
                    color = colors.cancelLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(ButtonShape)
                    .background(if (danger) colors.dangerButton else colors.confirmButton)
                    .border(
                        1.dp,
                        if (danger) colors.dangerBorder else colors.confirmBorder,
                        ButtonShape
                    )
                    .clickable(onClick = onConfirm)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = confirmLabel,
                    color = if (danger) colors.dangerLabel else colors.confirmLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// 确认弹窗文案 / 按钮配色（卡片本体走 Haze）
private data class ConfirmDialogColors(
    val title: Color,
    val message: Color,
    val cancelButton: Color,
    val cancelBorder: Color,
    val cancelLabel: Color,
    val confirmButton: Color,
    val confirmBorder: Color,
    val confirmLabel: Color,
    val dangerButton: Color,
    val dangerBorder: Color,
    val dangerLabel: Color
) {
    constructor(darkTheme: Boolean) : this(
        title = if (darkTheme) Color.White else Color(0xFF1E1A2E),
        message = if (darkTheme) Color.White.copy(alpha = 0.45f) else Color(0xFF1E1A2E).copy(alpha = 0.45f),
        cancelButton = if (darkTheme) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f),
        cancelBorder = if (darkTheme) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f),
        cancelLabel = if (darkTheme) Color.White.copy(alpha = 0.50f) else Color(0xFF1E1A2E).copy(alpha = 0.50f),
        confirmButton = if (darkTheme) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f),
        confirmBorder = if (darkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.10f),
        confirmLabel = if (darkTheme) Color.White else Color(0xFF1E1A2E),
        dangerButton = if (darkTheme) Color(0xFFFF453A).copy(alpha = 0.15f) else Color(0xFFD70015).copy(alpha = 0.10f),
        dangerBorder = if (darkTheme) Color(0xFFFF453A).copy(alpha = 0.25f) else Color(0xFFD70015).copy(alpha = 0.22f),
        dangerLabel = if (darkTheme) Color(0xFFF87171) else Color(0xFFD70015)
    )
}
