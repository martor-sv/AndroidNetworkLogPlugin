# AndroidNetworkLogPlugin

一个功能对标 vConsole 的 Android 网络日志查看插件。使用 Kotlin 编写，旨在为 Android 开发者在 Debug 阶段提供便捷的网络请求监控。

## 🌟 功能特性

- 🚀 **零配置接入**：简单几行代码即可集成。
- 📊 **列表展示**：实时显示 HTTP 方法、URL、状态码（颜色区分）、耗时、Body 大小。
- 🔍 **关键词过滤**：支持通过 URL 关键词实时筛选请求。
- 📄 **详情展示**：
  - **Overview**: 基本请求信息汇总。
  - **Request Headers**: 完整请求头信息。
  - **Request Body**: 请求正文（支持 JSON 自动格式化）。
  - **Response Headers**: 完整响应头信息。
  - **Response Body**: 响应正文（支持 JSON 自动格式化，支持 gzip 自动解压）。
- 🔘 **悬浮窗入口**：全局悬浮按钮（可拖拽），带未读数角标，点击直达日志列表。
- 🧹 **一键清空**：随时清理内存中的历史记录。
- 🌑 **暗黑模式**：酷炫的暗黑 UI 设计，视觉体验极佳。

## 📦 项目结构

- `networklog`: 核心 SDK 模块。
- `app`: Demo 演示模块，包含集成示例和测试请求。

## 🛠️ 集成指南

### 1. 添加 JitPack 仓库

在你的项目根目录 `settings.gradle.kts` 或 `build.gradle` 中添加 JitPack 仓库地址：

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // ⬅️ 添加这一行
    }
}
```

### 2. 添加依赖

在 `app/build.gradle.kts` 中添加插件依赖：

```kotlin
dependencies {
    // 请将 YOUR_GITHUB_USER 替换为你的 GitHub 用户名
    implementation("com.github.YOUR_GITHUB_USER:AndroidNetworkLogPlugin:1.0.0")
}
```

```kotlin
// 在 app/build.gradle.kts 中添加
dependencies {
    implementation(project(":networklog"))
}
```

### 3. 初始化插件

在你的 `Application` 类中初始化：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 建议仅在 Debug 环境下开启
        if (BuildConfig.DEBUG) {
            NetworkLogPlugin.install(this)
        }
    }
}
```

### 4. 配置 OkHttp 拦截器

将插件提供的拦截器添加到你的 `OkHttpClient` 中：

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(NetworkLogPlugin.getInterceptor())
    .build()
```

## 🔐 权限说明

由于插件使用悬浮窗展示入口，需要 **"显示在其他应用上"** 的权限：
- 插件在启动时会自动检测权限。
- 若无权限，会弹出 Toast 提示并引导用户前往系统设置页面开启。

## ⌨️ 常用 API

```kotlin
// 获取单例拦截器
NetworkLogPlugin.getInterceptor()

// 情况所有日志
NetworkLogPlugin.clearLogs()

// 检查悬浮窗权限
NetworkLogPlugin.hasOverlayPermission(context)

// 请求悬浮窗权限
NetworkLogPlugin.requestOverlayPermission(context)

// 隐藏悬浮窗
NetworkLogPlugin.dismiss(context)
```

## 📸 运行预览

1. 启动 App 后，屏幕右下角会出现 **"Net"** 字样的蓝色悬浮按钮。
2. 进行网络请求后，按钮右上角会出现红色未读数。
3. 点击按钮进入列表页，点击条目进入详情页（支持左右滑动切换 Tab）。

## 🛡️ License

MIT License
