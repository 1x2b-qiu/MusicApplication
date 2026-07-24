package com.leo.lune.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

const val PACKAGE_NAME = "com.leo.lune"

/** 冷启动 → 首页/登录 → Tab 切换 → 搜索，覆盖主要导航。 */
fun MacrobenchmarkScope.musicAppJourney(startActivity: Boolean = true) {
    if (startActivity) {
        startActivityAndWait()
    }

    // 会话恢复后直接进首页或登录页
    val ready = device.wait(Until.hasObject(By.text("首页")), 20_000) ||
        device.wait(Until.hasObject(By.text("登录")), 5_000)
    if (!ready) return

    if (device.findObject(By.text("首页")) != null) {
        // 首页列表滑动
        device.findObject(By.scrollable(true))?.also { list ->
            list.setGestureMargin(device.displayWidth / 5)
            list.fling(Direction.DOWN)
            device.waitForIdle()
            list.fling(Direction.UP)
            device.waitForIdle()
        }

        // 底部 Tab：电台 / 分类 / 回首页
        clickTextIfExists("电台")
        device.wait(Until.hasObject(By.text("电台")), 5_000)
        clickTextIfExists("分类")
        device.wait(Until.hasObject(By.text("分类")), 5_000)
        clickTextIfExists("首页")
        device.wait(Until.hasObject(By.text("首页")), 5_000)

        // 进搜索页再返回
        device.findObject(By.desc("搜索"))?.click()
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    } else {
        // 未登录：至少覆盖登录页渲染路径
        device.waitForIdle()
    }
}

private fun MacrobenchmarkScope.clickTextIfExists(text: String) {
    device.findObject(By.text(text))?.click()
    device.waitForIdle()
}
