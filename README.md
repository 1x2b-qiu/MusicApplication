# MusicApplication

个人学习用的网易云音乐 Android 客户端，采用 Clean Architecture，通过本机 NeteaseCloudMusicApi 代理访问网易云数据。

## 模块结构

```
:app     → Compose UI、ViewModel、导航、ExoPlayer 播放
:domain  → Song 模型、Repository 接口、UseCase
:data    → Retrofit 对接网易云 API、Repository 实现、Hilt DI
```

## 前置条件

1. 本机已安装 Node.js 18+
2. 启动网易云 API 服务：

```bash
npx NeteaseCloudMusicApi@latest
```

默认地址：`http://localhost:3000`

## 运行 App

1. 用 Android Studio 打开 `MusicApplication` 目录
2. Gradle Sync
3. 启动模拟器（API 默认指向 `http://10.0.2.2:3000/`）
4. Run `app`

真机调试时，修改 `data/build.gradle.kts` 中的 `NETEASE_BASE_URL` 为你电脑的局域网 IP，例如：

```kotlin
buildConfigField("String", "NETEASE_BASE_URL", "\"http://192.168.1.105:3000/\"")
```

## 当前功能

- 搜索歌曲（输入后自动防抖搜索）
- 点击歌曲进入播放页
- ExoPlayer 在线播放

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- Clean Architecture（domain / data / app）
- Hilt、Retrofit、OkHttp、Coil
- Media3 ExoPlayer
