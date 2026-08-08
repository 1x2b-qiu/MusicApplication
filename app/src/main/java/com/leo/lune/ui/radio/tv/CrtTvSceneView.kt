package com.leo.lune.ui.radio.tv

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.sceneview.RenderQuality
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

// ---------------------------------------------------------------------------
// 可调参数区：改这里就能调初始大小 / 位置 / 角度（单位见各注释）
// 坐标系：+X 右、+Y 上、+Z 朝向屏幕外（近似）；角度单位为「度」
// ---------------------------------------------------------------------------

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
private const val VinylScaleToUnits = 1.05f

/**
 * 主光强度（无量纲，SceneView/Filament）。
 * - 越大越亮；太暗可提到 120_000f，过曝可降到 60_000f。
 */
private const val VinylLightIntensity = 60_000f

// 电台黑胶预览：Orbit 拖转；背景透出 Compose 主题色
@Composable
fun CrtTvSceneView(
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

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
        rememberModelInstance(modelLoader, VinylAssetPath)?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = VinylScaleToUnits,
                // (0,0,0)=用包围盒中心当原点，方便旋转/缩放不跑偏
                centerOrigin = Position(0f, 0f, 0f),
                position = VinylPosition,
                rotation = VinylRotation,
                // 此模型无内嵌动画
                autoAnimate = false
            )
        }
    }
}
