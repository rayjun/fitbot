# iOS 编译与安装指南 (v0.4.1)

我们已经成功将 FitBot 升级为 **全自动化 Compose Multiplatform 架构**。现在，iOS 的编译、链接和签名已完全自动化。

## 1. 核心架构升级
*   **自动化链接**: 放弃了手动拖入 Framework 的旧方式。现在使用 `embedAndSignAppleFrameworkForXcode` 脚本。
*   **架构自适应**: 无论你选择 iOS 模拟器还是真机，Gradle 都会自动编译对应的架构并完成链接。
*   **零配置**: `project.yml` 已配置好所有路径，无需手动修改 Xcode 设置。

## 2. 如何在 iOS 上运行

### 第一步：生成项目
如果你是第一次运行，或者我修改了项目配置，请在终端执行：
```bash
cd iosApp
xcodegen generate
```

### 第二步：运行应用
1.  **打开项目**:
    ```bash
    open iosApp/FitBot.xcodeproj
    ```
2.  **选择目标**: 在 Xcode 顶部选择你的 **iPhone 真机** 或 **模拟器**。
3.  **点击 Run**: 
    *   Xcode 会自动调用 Gradle 进行后台编译。
    *   **首次运行**可能需要 1-2 分钟，请耐心等待。
    *   编译进度可以在 Xcode 的 `Report Navigator` (左侧最后一个图标) 中查看。

## 3. 已知问题与解决
*   **Linker Error**: 如果遇到 "object file built for iOS-simulator" 错误，请运行 `xcodegen generate` 重新刷新项目。
*   **动图显示**: 目前 iOS 端由于 Skia 引擎限制，动图暂以占位符显示，这保证了应用的稳定性。

---
**提示**: Android 端依然保持完全正常，你可以通过 `./gradlew assembleDebug` 继续生成 Android 安装包。
