package com.leo.lune.ui.radio.tv

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.sceneview.RenderQuality
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

// assets 路径：models/crt_tv.glb
private const val CrtTvAssetPath = "models/crt_tv.glb"

// 电台 CRT 预览：加载 GLB + Orbit 拖转；背景透出 Compose 主题色
@Composable
fun CrtTvSceneView(
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    SceneView(
        modifier = modifier.fillMaxSize(),
        // TextureSurface 支持透明混合，才能透出外层主题背景
        surfaceType = SurfaceType.TextureSurface,
        // false：清屏透明 + 去掉黑色 skybox，由外层 Box 主题色铺底
        isOpaque = false,
        engine = engine,
        modelLoader = modelLoader,
        // 正对屏幕：相机在中轴前方，略抬高一点更自然
        cameraManipulator = rememberCameraManipulator(
            orbitHomePosition = Position(x = 0f, y = 0.25f, z = 2.0f),
            targetPosition = Position(0f, 0.15f, 0f)
        ),
        mainLightNode = rememberMainLightNode(engine) {
            intensity = 80_000f
        },
        // 中低端优先：降低后处理成本
        renderQuality = RenderQuality.Performance
    ) {
        rememberModelInstance(modelLoader, CrtTvAssetPath)?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.15f,
                centerOrigin = Position(0f, 0f, 0f),
                autoAnimate = false
            )
        }
    }
}
