package com.leo.lune.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true,
        profileBlock = {
            startActivityAndWait()
            // 等首屏稳定后再走导航，避免模拟器 framestats 抖动
            device.waitForIdle(5_000)
            musicAppJourney(startActivity = false)
        }
    )
}
