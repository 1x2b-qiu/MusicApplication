package com.leo.lune.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

const val PACKAGE_NAME = "com.leo.lune"

/** 冷启动 → 曲库/登录 → Tab 切换 → 搜索，覆盖主要导航。 */
fun MacrobenchmarkScope.musicAppJourney(startActivity: Boolean = true) {
    if (startActivity) {
        startActivityAndWait()
    }

    // 会话恢复后直接进曲库或登录页
    val ready = device.wait(Until.hasObject(By.text("曲库")), 20_000) ||
        device.wait(Until.hasObject(By.text("登录")), 5_000)
    if (!ready) return

    if (device.findObject(By.text("曲库")) != null) {
        // 曲库列表滑动
        device.findObject(By.scrollable(true))?.also { list ->
            list.setGestureMargin(device.displayWidth / 5)
            list.fling(Direction.DOWN)
            device.waitForIdle()
            list.fling(Direction.UP)
            device.waitForIdle()
        }

        // 底部 Tab：电台 / 我的 / 回曲库
        clickTextIfExists("电台")
        device.wait(Until.hasObject(By.text("电台")), 5_000)
        clickTextIfExists("我的")
        device.wait(Until.hasObject(By.text("我的")), 5_000)
        clickTextIfExists("曲库")
        device.wait(Until.hasObject(By.text("曲库")), 5_000)

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
