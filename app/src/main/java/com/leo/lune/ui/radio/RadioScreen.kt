package com.leo.lune.ui.radio

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.android.filament.Engine
import com.google.android.filament.Texture
import com.leo.lune.R
import com.leo.lune.controller.PlaybackPosition
import com.leo.lune.ui.home.formatSongDuration
import com.leo.lune.util.consumePointersUnlessResumed
import com.leo.lune.util.rememberCoverRequest
import io.github.sceneview.RenderQuality
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.material.setBaseColorMap
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.texture.ImageTexture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.math.min

// 底部 TabBar 高度 66 + 外层 bottom 12，控制区叠在模型上并避开 Tab
private val RadioControlsBottomInset = 78.dp

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RadioScreen(
    viewModel: RadioViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .consumePointersUnlessResumed()
    ) {
        // 当前曲封面虚化铺满，作氛围底；无封面时仍见主题色背景
        if (!uiState.coverUrl.isNullOrBlank()) {
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            AsyncImage(
                model = rememberCoverRequest(uiState.coverUrl, screenWidth),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(16.dp),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.background.copy(alpha = 0.45f))
            )
        }

        CrtTvSceneView(
            isPlaying = uiState.isPlaying,
            coverUrl = uiState.coverUrl,
            modifier = Modifier.fillMaxSize()
        )

        RadioPlayerControls(
            uiState = uiState,
            positionState = viewModel.positionState,
            onSeek = viewModel::seekTo,
            onTogglePlayPause = viewModel::togglePlayPause,
            onSkipPrevious = viewModel::skipToPrevious,
            onSkipNext = viewModel::skipToNext,
            onToggleFavorite = viewModel::toggleFavorite,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = RadioControlsBottomInset)
        )
    }
}

// 电台播控：歌名/进度/上播下/收藏；进度条可 seek（对齐 PlayerScreen）
@Composable
private fun RadioPlayerControls(
    uiState: RadioUiState,
    positionState: StateFlow<PlaybackPosition>,
    onSeek: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val songName = uiState.songName.ifBlank { "未知歌曲" }
    val artistName = uiState.artistName.ifBlank { "未知歌手" }

    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = songName,
                    color = colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        uiState.isLoading && uiState.songs.isEmpty() -> "正在加载私人 FM…"
                        uiState.error != null && uiState.songs.isEmpty() -> uiState.error
                        else -> artistName
                    },
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (uiState.qualityLabel.isNotBlank()) {
                Text(
                    text = uiState.qualityLabel,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        RadioProgressSection(
            positionState = positionState,
            songId = uiState.songId,
            fallbackDurationMs = uiState.durationMs,
            isFmSession = uiState.isFmSession,
            onSeek = onSeek
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 观看音乐视频：贴最左（占位，暂无逻辑）
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceVariant)
                    .border(0.67.dp, colorScheme.outlineVariant, CircleShape)
                    .clickable(onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = "观看音乐视频",
                    tint = colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSkipPrevious),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "上一首",
                        tint = colorScheme.onBackground,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(53.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primary)
                        .clickable(onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            if (uiState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (uiState.isPlaying) "暂停" else "播放",
                        colorFilter = ColorFilter.tint(colorScheme.onPrimary),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSkipNext),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "下一首",
                        tint = colorScheme.onBackground,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceVariant)
                    .border(0.67.dp, colorScheme.outlineVariant, CircleShape)
                    .clickable(onClick = onToggleFavorite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        if (uiState.isFavorite) R.drawable.ic_heart2 else R.drawable.ic_heart
                    ),
                    contentDescription = if (uiState.isFavorite) "取消收藏" else "收藏",
                    tint = if (uiState.isFavorite) {
                        Color(0xFFFF4D6A)
                    } else {
                        colorScheme.onBackground
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 进度条 + 时间：独立订阅高频 positionState；未开播时显示 0 进度
@Composable
private fun RadioProgressSection(
    positionState: StateFlow<PlaybackPosition>,
    songId: Long,
    fallbackDurationMs: Long,
    isFmSession: Boolean,
    onSeek: (Long) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val playbackPosition by positionState.collectAsStateWithLifecycle()
    val durationMs = when {
        isFmSession -> playbackPosition.durationMs.takeIf { it > 0L } ?: fallbackDurationMs
        else -> fallbackDurationMs
    }
    var dragFraction by remember(songId) { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val progressFraction = when {
        !isFmSession -> 0f
        isDragging -> dragFraction
        durationMs > 0L -> (playbackPosition.positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        else -> 0f
    }
    val displayPositionMs = when {
        !isFmSession -> 0L
        isDragging && durationMs > 0L -> (dragFraction * durationMs).toLong()
        else -> playbackPosition.positionMs
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatSongDuration(displayPositionMs),
            color = colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = formatSongDuration(durationMs),
            color = colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    RadioProgressBar(
        progress = progressFraction.coerceIn(0f, 1f),
        enabled = isFmSession && durationMs > 0L,
        onProgressChange = { fraction ->
            isDragging = true
            dragFraction = fraction
        },
        onProgressChangeFinished = {
            isDragging = false
            if (isFmSession && durationMs > 0L) {
                onSeek((dragFraction * durationMs).toLong())
            }
        }
    )
}

@Composable
private fun RadioProgressBar(
    progress: Float,
    enabled: Boolean,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val fraction = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = onProgressChangeFinished,
                                onDragCancel = onProgressChangeFinished,
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    val width = size.width.toFloat().coerceAtLeast(1f)
                                    onProgressChange((change.position.x / width).coerceIn(0f, 1f))
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val width = size.width.toFloat().coerceAtLeast(1f)
                                onProgressChange((offset.x / width).coerceIn(0f, 1f))
                                onProgressChangeFinished()
                            }
                        }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(colorScheme.onBackground.copy(alpha = 0.2f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(colorScheme.onBackground)
        )
    }
}

/** assets 下的模型路径（必须带 models/，否则加载失败） */
private const val VinylAssetPath = "models/12_vinyl_record.glb"

/**
 * 相机初始位置（Orbit 圆心环绕的起点）。
 * - x：左右，>0 偏右看，<0 偏左看
 * - y：高低，越大越「俯视」，越小越平视甚至仰视
 * - z：远近，越大越远（唱片显得越小），越小越近（显得越大）
 * 想更斜一点：加大 y、略减 z；想更正一点：减小 y、加大 z。
 */
private val VinylOrbitHome = Position(x = 0.8f, y = 1.15f, z = 3f)

/**
 * 相机注视点（画面「看向」哪里，也是拖转环绕的中心）。
 * - x：左右偏移注视点
 * - y：上下，减小 y 会让唱片在画面里更靠下；增大则更靠上
 * - z：前后偏移注视点
 * 截图里唱片偏中下：y 用负数。
 */
private val VinylLookAt = Position(x = 0f, y = -0.28f, z = 0f)

/**
 * 模型在世界坐标中的平移（相对原点）。
 * - x：整张唱片左右移
 * - y：整张唱片上下移（再微调比改 VinylLookAt 更「挪物体」）
 * - z：整张唱片前后移
 */
private val VinylPosition = Position(x = 0f, y = 0f, z = 0f)

/**
 * 模型自身欧拉角旋转（度），在相机之前先拧模型。
 * - x：前后翻（负值 = 上边缘远离你 / 后仰；正值 = 前倾）
 * - y：左右转（正值 = 逆时针水平转，标签会偏一侧）
 * - z：平面内扭转（像唱片在自己盘面上拧，决定 ATLANTIC 朝向）
 * 标签要偏左上：主要调 y / z。
 */
private val VinylRotation = Rotation(x = 15f, y = -20f, z = -10f)

/**
 * 缩放到「包围盒最长边 ≈ 该值」的世界单位。
 * - 越大：唱片越大
 * - 越小：唱片越小
 * 约 1.0～1.2 时，全屏下大致接近占屏宽六成（还受体位远近影响）。
 */
private const val VinylScaleToUnits = 1.15f

/**
 * 主光强度（无量纲，SceneView/Filament）。
 * - 越大越亮；太暗可提到 120_000f，过曝可降到 60_000f。
 */
private const val VinylLightIntensity = 60_000f

// 电台黑胶预览：Orbit 拖转；背景透出 Compose 主题色；仅播放中转盘
@Composable
fun CrtTvSceneView(
    isPlaying: Boolean,
    coverUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, VinylAssetPath)
    val coverTextureHolder = remember { CoverTextureHolder() }

    // 把歌曲封面合成进 GLB 标签 UV 圆区，替换 baseColorMap
    LaunchedEffect(coverUrl, modelInstance) {
        val instance = modelInstance ?: return@LaunchedEffect
        val url = coverUrl?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val labelBitmap = withContext(Dispatchers.IO) {
            val result = context.imageLoader.execute(
                ImageRequest.Builder(context)
                    .data(url)
                    .size(VinylLabelCoverDecodeSize)
                    .allowHardware(false)
                    .build()
            )
            val cover = (result as? SuccessResult)?.drawable?.toBitmap() ?: return@withContext null
            buildVinylLabelBaseColor(cover)
        } ?: return@LaunchedEffect

        val texture = ImageTexture.Builder().bitmap(labelBitmap).build(engine)
        labelBitmap.recycle()
        try {
            ensureActive()
            // FilamentInstance.materialInstances 是数组，不是 SceneView 的 Map 扩展
            instance.materialInstances.forEach { material ->
                material.setBaseColorMap(texture)
            }
            coverTextureHolder.replace(engine, texture)
        } catch (e: CancellationException) {
            runCatching { engine.destroyTexture(texture) }
            throw e
        }
    }

    DisposableEffect(engine) {
        onDispose { coverTextureHolder.replace(engine, null) }
    }

    SceneView(
        modifier = modifier.fillMaxSize(),
        // TextureSurface：支持透明，才能透出外层主题背景
        surfaceType = SurfaceType.TextureSurface,
        // false：清屏透明、去掉黑 skybox
        isOpaque = false,
        // false：不要自动把内容拉回中心，否则 VinylPosition / LookAt 会被抵消
        autoCenterContent = false,
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator(
            orbitHomePosition = VinylOrbitHome,
            targetPosition = VinylLookAt
        ),
        mainLightNode = rememberMainLightNode(engine) {
            intensity = VinylLightIntensity
        },
        // Performance：中低端优先，降低后处理成本
        renderQuality = RenderQuality.Performance
    ) {
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = VinylScaleToUnits,
                // (0,0,0)=用包围盒中心当原点，方便旋转/缩放不跑偏
                centerOrigin = Position(0f, 0f, 0f),
                position = VinylPosition,
                rotation = VinylRotation,
                // animationName 非空时由 DisposableEffect 播动画，autoAnimate 会被忽略；
                // 用 name 有无切换才能真正启停 Spin
                autoAnimate = false,
                animationName = if (isPlaying) "Spin" else null,
                animationLoop = true
            )
        }
    }
}

/** GLB baseColor 上两侧标签圆（归一化 UV，与 12_vinyl_record 贴图对齐） */
private val VinylLabelCenters = floatArrayOf(
    0.2896f, 0.2896f,
    0.7094f, 0.7094f
)
private const val VinylLabelRadiusNorm = 0.0939f
private const val VinylLabelAtlasSize = 1024
private const val VinylLabelCoverDecodeSize = 256

private class CoverTextureHolder {
    private var texture: Texture? = null

    fun replace(engine: Engine, next: Texture?) {
        val previous = texture
        texture = next
        if (previous != null && previous != next) {
            runCatching { engine.destroyTexture(previous) }
        }
    }
}

/** 黑底 + 两个圆形封面，匹配唱片标签 UV。 */
private fun buildVinylLabelBaseColor(cover: Bitmap): Bitmap {
    val atlas = Bitmap.createBitmap(VinylLabelAtlasSize, VinylLabelAtlasSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(atlas)
    canvas.drawColor(android.graphics.Color.BLACK)
    val circular = cover.toCircularBitmap()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    var i = 0
    while (i < VinylLabelCenters.size) {
        val cx = VinylLabelCenters[i] * VinylLabelAtlasSize
        val cy = VinylLabelCenters[i + 1] * VinylLabelAtlasSize
        val r = VinylLabelRadiusNorm * VinylLabelAtlasSize
        canvas.drawBitmap(circular, null, RectF(cx - r, cy - r, cx + r, cy + r), paint)
        i += 2
    }
    if (circular !== cover) circular.recycle()
    return atlas
}

private fun Bitmap.toCircularBitmap(): Bitmap {
    val size = min(width, height)
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val path = Path().apply {
        addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
    }
    canvas.clipPath(path)
    val left = (width - size) / 2f
    val top = (height - size) / 2f
    canvas.drawBitmap(this, -left, -top, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    return output
}
