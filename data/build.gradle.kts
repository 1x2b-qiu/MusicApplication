import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// AudD Token：从根目录 local.properties 读取，勿提交到 Git
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val auddApiToken = localProperties.getProperty("AUDD_API_TOKEN").orEmpty()

android {
    namespace = "com.leo.lune.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        // 路由器局域网：手机与跑 NeteaseCloudMusicApi Enhanced 的电脑需在同一 Wi‑Fi
        buildConfigField("String", "NETEASE_BASE_URL", "\"http://81.71.51.73:3000/\"")
        // USB 调试：adb reverse tcp:3000 tcp:3000 后用 127.0.0.1（不依赖局域网/防火墙）
//        buildConfigField("String", "NETEASE_BASE_URL", "\"http://127.0.0.1:3000/\"")
        buildConfigField("String", "AUDD_API_TOKEN", "\"$auddApiToken\"")
        buildConfigField("String", "AUDD_BASE_URL", "\"https://api.audd.io/\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.documentfile)
    ksp(libs.androidx.room.compiler)
}
