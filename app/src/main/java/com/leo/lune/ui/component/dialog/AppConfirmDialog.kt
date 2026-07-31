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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

// 弹窗卡片圆角
private val CardShape = RoundedCornerShape(22.dp)

// 全局确认弹窗：半透明置灰遮罩 + 居中圆角卡片 + 取消/确认
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
    // 深浅色，影响遮罩透明度与确认色
    darkTheme: Boolean,
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
                    if (darkTheme) Color.Black.copy(alpha = 0.6f)
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
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        }
    }
}

// 居中确认卡片：缩放淡入淡出，标题区 + 分隔线 + 双按钮
@Composable
private fun AnimatedVisibilityScope.ConfirmDialogCard(
    title: String,
    message: String?,
    confirmLabel: String,
    cancelLabel: String,
    danger: Boolean,
    darkTheme: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .padding(horizontal = 48.dp)
            .widthIn(max = 280.dp)
            .fillMaxWidth()
            // 卡片相对遮罩单独做弹出/收回动画（缩放 + 轻微上移）
            .animateEnterExit(
                enter = fadeIn(tween(200)) +
                    scaleIn(
                        initialScale = 0.86f,
                        animationSpec = spring(
                            dampingRatio = 0.62f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) +
                    slideInVertically(tween(280)) { (it * 0.08f).toInt() },
                exit = fadeOut(tween(160)) +
                    scaleOut(targetScale = 0.90f, animationSpec = tween(160)) +
                    slideOutVertically(tween(160)) { (it * 0.05f).toInt() }
            )
            .shadow(
                elevation = 40.dp,
                shape = CardShape,
                spotColor = if (darkTheme) Color.Black.copy(alpha = 0.8f)
                else Color.Black.copy(alpha = 0.16f),
                ambientColor = if (darkTheme) Color.Black.copy(alpha = 0.8f)
                else Color.Black.copy(alpha = 0.16f)
            )
            .clip(CardShape)
            // 深色 surface* 为半透明玻璃色，弹窗需实心底，否则会透出下层文字
            .background(if (darkTheme) colorScheme.background else colorScheme.surfaceVariant)
            .border(1.dp, colorScheme.outlineVariant, CardShape)
            // 吞掉卡片区域点击，避免穿透到外层遮罩
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        // 标题 + 可选说明
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    modifier = Modifier.padding(top = 6.dp),
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }

        // 标题区与按钮行分隔
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colorScheme.outlineVariant)
        )

        // 取消 | 确认
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cancelLabel,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 按钮中间竖线
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(colorScheme.outlineVariant)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(onClick = onConfirm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = confirmLabel,
                    // danger 时用系统红；否则跟主题主文字色
                    color = when {
                        danger && darkTheme -> Color(0xFFFF453A)
                        danger -> Color(0xFFD70015)
                        else -> colorScheme.onBackground
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
