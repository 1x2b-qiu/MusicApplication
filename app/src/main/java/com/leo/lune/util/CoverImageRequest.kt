package com.leo.lune.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil.request.ImageRequest

/** 按显示尺寸解码，避免列表封面按原图像素进内存。 */
@Composable
fun rememberCoverRequest(
    data: Any?,
    size: Dp,
): ImageRequest = rememberCoverRequest(data, size, size)

@Composable
fun rememberCoverRequest(
    data: Any?,
    width: Dp,
    height: Dp,
): ImageRequest {
    val context = LocalContext.current
    val density = LocalDensity.current
    val widthPx = with(density) { width.roundToPx().coerceAtLeast(1) }
    val heightPx = with(density) { height.roundToPx().coerceAtLeast(1) }
    return remember(data, widthPx, heightPx) {
        ImageRequest.Builder(context)
            .data(data)
            .size(widthPx, heightPx)
            .build()
    }
}
