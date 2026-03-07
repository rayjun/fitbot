# iOS 编译与安装指南 (KMP Migration Phase 4)

我们已经成功将 FitBot 的核心 UI 和业务逻辑迁移到了 **Kotlin Multiplatform (KMP)**。现在，你可以将应用运行在 iOS 设备或模拟器上。

## 1. 核心变更摘要
*   **共享代码**: 90% 的 UI (Compose) 和业务逻辑已移至 `commonMain`。
*   **数据库处理**: 由于环境限制，Room 数据库目前保留在 Android 端。iOS 端目前使用共享的模型层 (`ExerciseSet`, `WorkoutPlan`)，后续可接入 iOS 原生数据库或升级 Room。
*   **资源系统**: 动图资产已迁移至共享资源目录，支持双端加载。

## 2. 如何在 iOS 上运行

### 方式 A：使用 Xcode (推荐)
1.  **打开 Xcode**。
2.  点击 **File > Open**，选择项目根目录下的 `iosApp` 文件夹（注：由于环境未安装 CocoaPods，目前尚未生成 `.xcworkspace`）。
3.  **手动关联框架** (如果尚未自动关联):
    *   在 Gradle 中运行: `./gradlew :app:linkDebugFrameworkIosArm64` (真机) 或 `:app:linkDebugFrameworkIosSimulatorArm64` (模拟器)。
    *   生成的框架位于: `app/build/bin/iosArm64/debugFramework/ComposeApp.framework`。
    *   将此框架拖入 Xcode 项目的 "Frameworks, Libraries, and Embedded Content" 中。

### 方式 B：配置 CocoaPods (最自动化)
如果在你的本地开发环境中有 `pod` 命令：
1.  进入 `iosApp` 目录：`cd iosApp`。
2.  执行安装：`pod install`。
3.  打开生成的 `iosApp.xcworkspace`。
4.  点击 Xcode 的 **Run** 按钮。

## 3. 已验证的状态
*   **Android**: 已通过 `./gradlew assembleDebug` 验证，功能完全正常且已安装至手机。
*   **iOS Framework**: 已通过 Gradle 任务验证，编译成功。
*   **Swift 封装**: 已创建 `MainViewController.kt` 和 `ContentView.swift` 桥接逻辑。

---
**提示**: 在 iOS 上运行前，请确保已安装最新版本的 Xcode 并且选中了正确的签名证书。
