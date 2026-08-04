package com.leo.lune.ui.component.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

// Toast 类型：决定图标与强调色
enum class AppToastType {
    Success,
    Error,
    Info,
    Default
}

private val ToastShape = RoundedCornerShape(16.dp)

// 全局轻提示：居中玻璃条，仅内容 + 类型图标，定时自动关闭
@Composable
fun AppToast(
    // 是否展示
    visible: Boolean,
    // 提示文案
    message: String,
    // 类型（图标/颜色）
    type: AppToastType = AppToastType.Default,
    // 展示时长
    durationMs: Long = 2_000L,
    // 深浅色
    darkTheme: Boolean,
    // 关闭回调（超时或外部清空）
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(visible, message, type, durationMs) {
        if (!visible) return@LaunchedEffect
        delay(durationMs)
        onDismiss()
    }

    val colors = toastColors(darkTheme, type)

    AnimatedVisibility(
        visible = visible && message.isNotBlank(),
        enter = fadeIn(tween(180)) + scaleIn(
            initialScale = 0.88f,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = fadeOut(tween(140)) + scaleOut(
            targetScale = 0.88f,
            animationSpec = tween(140)
        ),
        modifier = modifier.zIndex(120f)
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = ToastShape,
                    spotColor = Color.Black.copy(alpha = if (darkTheme) 0.7f else 0.18f),
                    ambientColor = Color.Black.copy(alpha = if (darkTheme) 0.7f else 0.18f)
                )
                .clip(ToastShape)
                .background(colors.background)
                .border(1.dp, colors.border, ToastShape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = colors.icon,
                contentDescription = null,
                tint = colors.iconTint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = message,
                modifier = Modifier.padding(start = 10.dp),
                color = colors.message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 18.sp
            )
        }
    }
}

private data class ToastColors(
    val background: Color,
    val border: Color,
    val message: Color,
    val icon: ImageVector,
    val iconTint: Color
)

private fun toastColors(darkTheme: Boolean, type: AppToastType): ToastColors {
    val icon = when (type) {
        AppToastType.Success -> Icons.Outlined.CheckCircle
        AppToastType.Error -> Icons.Outlined.ErrorOutline
        AppToastType.Info, AppToastType.Default -> Icons.Outlined.Info
    }
    val iconTint = when (type) {
        AppToastType.Success -> Color(0xFF34D399)
        AppToastType.Error -> if (darkTheme) Color(0xFFF87171) else Color(0xFFDC2626)
        AppToastType.Info -> if (darkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
        AppToastType.Default -> if (darkTheme) Color.White.copy(alpha = 0.60f)
        else Color(0xFF1E1A2E).copy(alpha = 0.55f)
    }
    return ToastColors(
        background = if (darkTheme) Color(0xD91C1C20) else Color(0xF0FFFFFF),
        border = if (darkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
        message = if (darkTheme) Color.White.copy(alpha = 0.90f) else Color(0xFF1E1A2E).copy(alpha = 0.90f),
        icon = icon,
        iconTint = iconTint
    )
}
