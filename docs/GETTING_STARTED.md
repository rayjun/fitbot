# FitBot 项目起步指南

本指南旨在帮助开发者快速配置环境、初始化项目并运行 FitBot（基于 Kotlin Multiplatform + Compose Multiplatform）。

---

## 1. 环境要求

在开始之前，请确保您的开发环境满足以下要求：

| 工具 | 版本要求 | 备注 |
|------|---------|-----|
| **JDK** | 17 | 项目根目录附带了 `jdk-17.0.10+7` |
| **Android Studio** | Koala / Ladybug (最新稳定版) | 需安装 Kotlin 和 Compose Multiplatform 插件 |
| **Xcode** | 15.0+ | 仅限 macOS，用于编译 iOS 侧 |
| **CocoaPods** | 1.12.0+ | 用于 iOS 依赖管理 |
| **Kotlin** | 1.9.22 | 项目 Gradle 配置中指定 |

### 1.1 核心框架版本 (KMP)
| 框架/插件 | 版本 | 说明 |
|----------|------|-----|
| **Kotlin** | `1.9.22` | KMP 及 Android 编译核心 |
| **Compose Multiplatform** | `1.6.11` | 跨平台 UI 框架 |
| **Android Gradle Plugin** | `8.3.0` | Android 构建插件 |

### 1.2 核心依赖库
*   **依赖注入**: Koin `3.5.3` / **状态管理**: ViewModel `2.8.0`
*   **本地存储**: DataStore `1.1.0` / **网络**: Ktor `2.3.7`

---

## 2. 项目初始化

### 2.1 克隆仓库
```bash
git clone <repository-url>
cd exercise
```

### 2.2 配置 Android 侧
1. 在 Android Studio 中打开项目，等待 Gradle 同步。
2. 确保 `local.properties` 指向正确的 Android SDK 路径。

### 2.3 配置 iOS 侧
1. 进入 `iosApp` 目录并安装 Pods：
   ```bash
   cd iosApp
   pod install
   cd ..
   ```
2. **关键步骤**：由于 iOS 静态框架链接机制，首次运行前需先通过 Gradle 编译共享模块：
   ```bash
   ./gradlew :app:linkDebugFrameworkIosArm64  # 真机
   # 或
   ./gradlew :app:linkDebugFrameworkIosX64   # 模拟器
   ```

---

## 3. 运行项目

### 3.1 运行单元测试 (推荐)
本项目通过 `commonTest` 实现了核心逻辑覆盖，建议在提交代码前运行：
```bash
./gradlew :app:testDebugUnitTest  # 运行 Android 侧单元测试 (包含共享的 commonTest)
```

### 3.2 Android 端
*   **IDE 运行**：在 Android Studio 中选择 `app` 运行配置，点击 "Run"。
*   **命令行运行**：
    ```bash
    ./gradlew :app:installDebug
    ```

### 3.2 iOS 端（推荐方式：Xcode）
1. 使用 Xcode 打开 `iosApp/FitBot.xcworkspace`。
2. 选择目标设备（真机或模拟器）。
3. 点击 "Run"。

> **注意**：项目已配置 **Run Script ("Copy Compose Resources")**。在每次 Xcode 构建时，它会自动将 Compose 资源从 Gradle 生成目录复制到 App Bundle 中。

### 3.3 iOS 端（命令行方式）
如果您需要通过命令行安装到真机，请参考 `docs/README_IOS.md` 中的详细步骤：
1. 编译 Framework。
2. 复制到 `xcode-frameworks` 搜索路径。
3. 使用 `xcodebuild` 构建并用 `xcrun devicectl` 安装。

---

## 4. 项目结构说明

*   **`app/src/commonMain`**: 核心逻辑。包含 Compose UI、ViewModel、Repository 接口及 Koin 共享模块。
*   **`app/src/androidMain`**: Android 特有实现（如 Room 数据库配置、原生权限处理）。
*   **`app/src/iosMain`**: iOS 特有实现（如 DataStore 路径、UIKit 视图桥接、Google 登录回调）。
*   **`iosApp/`**: 包含原生 Swift 代码和 Xcode 配置，作为 KMP 框架的宿主。

---

## 5. 开发常见问题 (FAQ)

### Q1: 修改了 `commonMain` 的代码，iOS 侧不生效？
**A**: 在 Xcode 中点击 Build 即可触发 Gradle 增量编译。如果失效，请尝试手动运行 `./gradlew :app:embedAndSignAppleFrameworkForXcode` 或清理 `iosApp` 的 Build 缓存。

### Q2: 出现 `No such module 'ComposeApp'` 错误？
**A**: 这是由于 Framework 尚未生成。请先运行 `./gradlew :app:linkDebugFrameworkIosArm64`，并确保 `app/build/xcode-frameworks` 路径下存在对应的框架。

### Q3: 图片或字符串资源找不到（崩溃）？
**A**: 检查 `FitBot.xcodeproj` 中的 Run Script 是否正常执行。资源必须存在于 `FitBot.app/compose-resources/` 目录下。

### Q4: 登录态在应用重启后丢失？
**A**: 已通过 `restoreSignIn` 修复。需确保 `MainViewController` 在启动时触发了自动恢复逻辑。

### Q5: 命令行构建 iOS 真机失败（签名报错）？
**A**: 命令行构建不自动签名。请在 Xcode 中配置 `Signing & Capabilities`，然后点击 Xcode 的运行按钮。

---

## 6. 后续维护

*   **更新动作库**：修改 `commonMain/kotlin/com/fitness/model/ExerciseProvider.kt` 并放入对应的 GIF 到 `composeResources/files/exercises/`。
*   **修改字符串**：统一修改 `commonMain/kotlin/com/fitness/util/ResourceUtils.kt`。
