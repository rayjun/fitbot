# iOS 编译与真机安装指南

本文档记录将 FitBot（Kotlin Multiplatform + Compose Multiplatform）编译并安装到 iOS 真机的完整流程，以及遇到过的关键问题和修复方案。

---

## 环境要求

| 工具 | 说明 |
|------|------|
| Xcode | 需要登录 Apple 账号，确保 Development 证书有效 |
| `xcodebuild` | Xcode 命令行工具 |
| `xcrun devicectl` | macOS 15+ 提供，用于管理真机 |
| Gradle | 项目根目录使用 `./gradlew` |

---

## 一、编译流程

### 第 1 步：编译 Kotlin 框架（iosArm64）

```bash
./gradlew :app:linkDebugFrameworkIosArm64
```

编译完成后，框架输出在：

```
app/build/bin/iosArm64/debugFramework/ComposeApp.framework
```

### 第 2 步：将框架复制到 Xcode 搜索路径

Xcode 项目的 `FRAMEWORK_SEARCH_PATHS` 指向以下目录，需手动创建并复制框架：

```bash
mkdir -p app/build/xcode-frameworks/Debug/iphoneos
cp -R app/build/bin/iosArm64/debugFramework/ComposeApp.framework \
      app/build/xcode-frameworks/Debug/iphoneos/
```

> **为何需要手动复制？**
> `embedAndSignAppleFrameworkForXcode` Gradle 任务依赖 Xcode 构建时注入的环境变量（`PLATFORM_NAME`、`CONFIGURATION` 等），在命令行单独调用时不可用。直接 `cp` 是最可靠的替代方案。

### 第 3 步：使用 xcodebuild 构建 .app

```bash
cd iosApp
xcodebuild \
  -project FitBot.xcodeproj \
  -scheme FitBot \
  -configuration Debug \
  -sdk iphoneos \
  -allowProvisioningUpdates \
  CODE_SIGN_IDENTITY="Apple Development" \
  DEVELOPMENT_TEAM=E9M73F85W6 \
  build
```

构建产物路径：

```
~/Library/Developer/Xcode/DerivedData/FitBot-<hash>/Build/Products/Debug-iphoneos/FitBot.app
```

查找最新的 DerivedData hash：

```bash
ls ~/Library/Developer/Xcode/DerivedData/ | grep FitBot
```

---

## 二、安装到真机

### 查找设备 UDID

```bash
xcrun devicectl list devices
```

输出示例：
```
CF4F653A-6C03-5FEC-A93A-C63FD0FDF084  iPhone 14 Pro  iOS 26.3
```

### 安装 .app

```bash
xcrun devicectl device install app \
  --device CF4F653A-6C03-5FEC-A93A-C63FD0FDF084 \
  ~/Library/Developer/Xcode/DerivedData/FitBot-<hash>/Build/Products/Debug-iphoneos/FitBot.app
```

### 启动应用

```bash
xcrun devicectl device process launch \
  --device CF4F653A-6C03-5FEC-A93A-C63FD0FDF084 \
  com.fitness.FitBot
```

---

## 三、排查崩溃

### 查看设备崩溃日志列表

```bash
xcrun devicectl device info files \
  --device CF4F653A-6C03-5FEC-A93A-C63FD0FDF084 \
  --domain-type systemCrashLogs
```

### 下载指定崩溃日志

```bash
xcrun devicectl device copy from \
  --device CF4F653A-6C03-5FEC-A93A-C63FD0FDF084 \
  --domain-type systemCrashLogs \
  --source "FitBot-2026-03-08-143542.ips" \
  --destination /tmp/fitbot_crash.ips
```

### 读取崩溃原因

`.ips` 是两段式文件（头部 JSON + 正文 JSON），可以用 `grep` 快速提取关键信息：

```bash
# 查看异常类型和 abort 原因
grep -A5 '"exception"' /tmp/fitbot_crash.ips | head -10

# 查看 Kotlin 符号化的调用栈
grep -o '"symbol":"[^"]*"' /tmp/fitbot_crash.ips | grep -v '"symbol":""' | head -40
```

---

## 四、已遇到并修复的关键问题

### 问题 1：`No such module 'ComposeApp'`

**现象：** xcodebuild 构建失败，找不到 ComposeApp 模块。

**原因：** `app/build/xcode-frameworks/Debug/iphoneos/` 目录不存在（只有 `iphonesimulator/`）。

**修复：** 执行第 1、2 步手动复制框架（见上文）。

---

### 问题 2：Koin 未初始化崩溃

**现象：** 应用启动后立刻崩溃，日志为：
```
kotlin.IllegalStateException: KoinApplication has not been started
```

**原因：** `setupKoin()` Kotlin 函数已暴露给 Swift，但 Swift 入口 `iosApp.swift` 没有调用它，导致 `KoinContext {}` Composable 在 Koin 未启动时执行。

**修复：** 在 `iosApp/iosApp/iosApp.swift` 中添加调用：

```swift
import SwiftUI
import ComposeApp

@main
struct iosApp: App {
    init() {
        MainViewControllerKt.setupKoin()  // 必须在 UI 初始化前调用
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

对应的 Kotlin 侧定义（`MainViewController.kt`）：

```kotlin
// 顶层函数，供 Swift 在 App.init() 中调用
fun setupKoin() = initKoin()
```

---

### 问题 3：Compose Resources 缺失导致崩溃

**现象：** 应用启动进入 UI 后立刻崩溃，崩溃栈为：
```
MissingResourceException → stringResource() → getString() → ExerciseLibraryScreen
```

**原因：** Compose Multiplatform 的 iOS 静态框架本身不包含资源文件（字符串、图片等）。资源需要以 `compose-resources/` 目录的形式放置在 app bundle 根目录下，但 Xcode 项目中没有任何步骤完成这一复制操作。

**修复：** 在 `FitBot.xcodeproj/project.pbxproj` 中添加 **Run Script Build Phase**（"Copy Compose Resources"），在每次构建时将 Gradle 生成的资源目录复制进 app bundle：

```sh
RESOURCES_DIR="$SRCROOT/../app/build/generated/compose/resourceGenerator/preparedResources/commonMain/composeResources"
DEST_DIR="$BUILT_PRODUCTS_DIR/$CONTENTS_FOLDER_PATH/compose-resources"
if [ -d "$RESOURCES_DIR" ]; then
  rm -rf "$DEST_DIR"
  cp -R "$RESOURCES_DIR" "$DEST_DIR"
fi
```

此脚本已写入项目，后续构建会自动执行，无需手动操作。

---

### 问题 4：签名 Team ID 错误

**现象：** xcodebuild 报错 `No Account for Team "JA4E9U5888"`。

**原因：** 证书的 Team ID 与 Xcode 账号的 Team ID 不一致。

**修复：** 用以下命令查找账号真实的 Team ID：

```bash
defaults read com.apple.dt.Xcode IDEProvisioningTeams
```

找到正确的 Team ID（本项目为 `E9M73F85W6`）后，在构建命令中通过 `DEVELOPMENT_TEAM=E9M73F85W6` 指定，并加上 `-allowProvisioningUpdates` 让 Xcode 自动生成描述文件。

---

### 问题 5：安装失败 `The item at FitBot.app is not a valid bundle`

**现象：** `xcrun devicectl device install app` 报错。

**原因：** `find` 命令返回的是旧的 DerivedData 路径（哈希值不同），而不是最新构建产物的路径。

**修复：** 用 `ls ~/Library/Developer/Xcode/DerivedData/ | grep FitBot` 确认当前正确的 DerivedData 哈希目录，使用完整绝对路径安装。

---

## 五、架构说明

```
Kotlin 源码 (commonMain / iosMain)
        ↓ ./gradlew :app:linkDebugFrameworkIosArm64
ComposeApp.framework  (二进制 + Swift 模块头)
        ↓ cp 到 xcode-frameworks/Debug/iphoneos/
Xcode 构建 (xcodebuild)
  ├── 链接 ComposeApp.framework (OTHER_LDFLAGS = -framework ComposeApp)
  ├── Run Script: Copy Compose Resources
  │     └── compose-resources/ → FitBot.app/compose-resources/
  └── 代码签名
        ↓ xcrun devicectl
真机安装 & 运行
```
