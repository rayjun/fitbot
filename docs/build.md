# 编译与部署指南

## 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 17（项目内置于 `jdk-17.0.10+7/`） |
| Android Studio / Gradle | Gradle 8.4，AGP 8.3.0 |
| Xcode | 15.x 或以上 |
| CocoaPods | 最新版（`sudo gem install cocoapods`） |
| XcodeGen | 最新版（`brew install xcodegen`） |
| iOS 部署工具 | `xcrun devicectl`（Xcode 15 内置） |

---

## Android 编译

### 调试构建（Debug APK）
```bash
./gradlew :app:assembleDebug
```
产物路径：`app/build/outputs/apk/debug/app-debug.apk`

### 直接安装到设备
```bash
./gradlew :app:installDebug
```

### 发布构建（Release APK）
```bash
./gradlew :app:assembleRelease
```

### 重要 Gradle 配置
- `app/build.gradle.kts` 中 `android.namespace = "com.fitness"`
- `minSdk = 26`，`targetSdk = 34`，`compileSdk = 34`
- Room、Hilt 通过 KSP 生成代码（`ksp(...)` 依赖）

---

## iOS 编译

### 完整编译流程（4 步）

#### Step 1：构建 Kotlin/Native Framework
```bash
./gradlew :app:linkDebugFrameworkIosArm64
```
产物：`app/build/bin/iosArm64/debugFramework/ComposeApp.framework`

> 其他架构：
> - 模拟器（arm64）：`linkDebugFrameworkIosSimulatorArm64`
> - 模拟器（x86_64）：`linkDebugFrameworkIosX64`
> - Release：把 `Debug` 换成 `Release`

#### Step 2：复制 Framework 到 Xcode 查找路径
```bash
cp -R app/build/bin/iosArm64/debugFramework/ComposeApp.framework \
      app/build/xcode-frameworks/Debug/iphoneos/
```
Xcode 工程中的 Framework Search Paths 指向此目录。

#### Step 3：xcodebuild 编译
```bash
cd iosApp
xcodebuild \
  -workspace FitBot.xcworkspace \
  -scheme FitBot \
  -configuration Debug \
  -sdk iphoneos \
  -allowProvisioningUpdates \
  CODE_SIGN_IDENTITY="Apple Development" \
  DEVELOPMENT_TEAM=E9M73F85W6 \
  build
```
产物：`~/Library/Developer/Xcode/DerivedData/FitBot-*/Build/Products/Debug-iphoneos/FitBot.app`

> **必须使用 `.xcworkspace`** 而不是 `.xcodeproj`，因为 CocoaPods（GoogleSignIn）已集成。

#### Step 4：安装并启动（真机）
```bash
# 安装
xcrun devicectl device install app \
  --device <UDID> \
  <path-to-FitBot.app>

# 启动
xcrun devicectl device process launch \
  --device <UDID> \
  com.fitness.FitBot
```

设备 UDID 查询：
```bash
xcrun devicectl list devices
```

---

## iOS 工程文件管理

### 使用 XcodeGen 重新生成工程
当新增 Swift 源文件后，需要重新生成 `.xcodeproj`：
```bash
cd iosApp
xcodegen generate
pod install
```
之后始终使用 `FitBot.xcworkspace`。

### project.yml 关键配置
```yaml
targets:
  FitBot:
    platform: iOS
    deploymentTarget: "14.1"
    sources:
      - iosApp
    dependencies:
      - framework:
          path: ../app/build/xcode-frameworks/$(CONFIGURATION)/$(PLATFORM_NAME)/ComposeApp.framework
    settings:
      FRAMEWORK_SEARCH_PATHS:
        - $(PROJECT_DIR)/../app/build/xcode-frameworks/$(CONFIGURATION)/$(PLATFORM_NAME)
    preBuildScripts:
      - script: |
          cd "$SRCROOT/.."
          ./gradlew :app:embedAndSignAppleFrameworkForXcode
        name: KMP Framework
```

> **注意**：命令行编译时 `embedAndSignAppleFrameworkForXcode` 依赖 Xcode 环境变量，无法直接运行。应使用上述手动 `cp` 方式替代。

### CocoaPods 依赖
`iosApp/Podfile`：
```ruby
platform :ios, '14.1'
use_frameworks!
project 'FitBot.xcodeproj'

target 'FitBot' do
  pod 'GoogleSignIn', '~> 7.1'
end
```
GoogleSignIn 7.1 会自动拉取 GTMAppAuth、GTMSessionFetcher、AppAuth 等依赖。

---

## Compose Resources 配置

GIF 文件存放于 `app/src/commonMain/composeResources/files/exercises/`，
编译时由 Compose Resources 插件打包。

**iOS 端**需要在 Xcode Build Phase 中将资源复制到 app bundle：
```bash
# Run Script（已在 pbxproj 中配置）
cp -R "${PROJECT_DIR}/../app/build/generated/compose/resourceGenerator/preparedResources/commonMain/composeResources" \
  "${BUILT_PRODUCTS_DIR}/${CONTENTS_FOLDER_PATH}/compose-resources"
```

---

## 一键编译脚本（参考）

以下脚本整合了 iOS 真机的完整编译流程：

```bash
#!/bin/bash
set -e
DEVICE_UDID="CF4F653A-6C03-5FEC-A93A-C63FD0FDF084"

echo "==> 1. Build KMP framework"
./gradlew :app:linkDebugFrameworkIosArm64

echo "==> 2. Copy framework"
cp -R app/build/bin/iosArm64/debugFramework/ComposeApp.framework \
      app/build/xcode-frameworks/Debug/iphoneos/

echo "==> 3. Xcodebuild"
cd iosApp
xcodebuild \
  -workspace FitBot.xcworkspace \
  -scheme FitBot \
  -configuration Debug \
  -sdk iphoneos \
  -allowProvisioningUpdates \
  CODE_SIGN_IDENTITY="Apple Development" \
  DEVELOPMENT_TEAM=E9M73F85W6 \
  build
cd ..

echo "==> 4. Install & launch"
APP=$(find ~/Library/Developer/Xcode/DerivedData -name "FitBot.app" -path "*/Debug-iphoneos/*" | head -1)
xcrun devicectl device install app --device "$DEVICE_UDID" "$APP"
xcrun devicectl device process launch --device "$DEVICE_UDID" com.fitness.FitBot

echo "Done!"
```

---

## 常见问题

### `embedAndSignAppleFrameworkForXcode` 失败
原因：该 Gradle task 需要 Xcode 注入的环境变量（`CONFIGURATION`、`PLATFORM_NAME` 等），命令行直接运行时缺少这些变量。
**解决**：跳过该 task，改用 `linkDebugFrameworkIosArm64` + 手动 `cp`。

### `Cannot find 'GoogleSignInBridge' in scope`
原因：新增的 Swift 文件未注册到 `.xcodeproj`。
**解决**：`cd iosApp && xcodegen generate && pod install`，再用 `.xcworkspace` 构建。

### `pod install` 提示无法自动选择 Xcode 工程
原因：目录下存在多个 `.xcodeproj`。
**解决**：在 `Podfile` 中显式指定 `project 'FitBot.xcodeproj'`。

### Koin `Unresolved reference: KoinPlatform`
原因：iOS klib 中正确的包路径是 `org.koin.mp.KoinPlatform`，而非 `org.koin.core.context.KoinPlatform`。

### Compose Resources GIF 在 iOS 上不显示
原因：`ExerciseImage` iOS actual 使用了 `readResourceBytes("files/{gifResPath}")`，需要 `@OptIn(InternalResourceApi::class, ExperimentalResourceApi::class)` 注解，且资源必须已通过 Run Script 复制到 bundle。
