# Kotlin Multiplatform (KMP) iOS 集成技术总结

本文档总结了 FitBot 项目在 KMP 迁移过程中，iOS 端遇到的核心编译与链接问题及其最终解决方案。

## 1. 核心问题回顾

在非 CocoaPods 模式下集成 KMP 框架时，我们遇到了两个交替出现的典型错误：

### 问题 A: `Unable to find module dependency: 'ComposeApp'`
*   **现象**: Swift 代码中 `import ComposeApp` 报错，出现红色波浪线且代码补全失效。
*   **原因**: Xcode 的索引器（SourceKit）依赖显式的项目引用来寻找 `.modulemap` 文件，如果没有显式的框架引用，编辑器会报错找不到模块。

### 问题 B: `Linker architecture mismatch (built for iOS-simulator)`
*   **现象**: 编译报错：`Building for 'iOS', but linking in object file... built for 'iOS-simulator'`。
*   **原因**: 如果在 Xcode 的 `dependencies` 中强行写入了指向“模拟器”物理路径的 `.framework`，当尝试在“真机”上编译时，链接器被强制使用错误架构的二进制文件从而失败。

---

## 2. 根源分析

KMP 默认的编译产物路径（`app/build/bin/`）严格区分了架构：
*   模拟器: `iosSimulatorArm64`
*   真机: `iosArm64`

这是一个**“既要...又要...”**的矛盾困境：
*   为了解决**问题A（让索引器高兴）**，我们需要给 Xcode 提供一个确切的文件路径。
*   为了解决**问题B（让链接器高兴）**，我们又不能写死某一个架构路径，必须根据当前 `sdk` 动态切换。

---

## 3. 最终解决方案：分离索引引用与链接路径 (Definitive Solution)

我们采用了一套基于 **隐式依赖 (implicit) + 条件宏过滤** 的混合技术，在 `project.yml` 中完美地化解了这一矛盾。

### 核心配置 (project.yml)

#### 第一步：解决代码补全红线 (处理 Indexer)
在 `dependencies` 中添加对某一架构（比如模拟器）的显式框架引用，但增加两个关键属性：
```yaml
    dependencies:
      - framework: ../app/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework
        embed: false
        link: false      # 【关键1】禁止在 Ld(链接) 阶段使用此路径
        implicit: true   # 【关键2】禁止在项目级别生成静态 FRAMEWORK_SEARCH_PATHS
```
*   **原理**: 这样 Xcode 的文件树中会存在 `ComposeApp.framework`，SourceKit 能够据此解析 `module.modulemap`，从而提供代码补全。但 `link: false` 保证了它不会干扰真正的二进制链接，`implicit: true` 保证了 XcodeGen 不会用这个路径污染全局搜索路径。

#### 第二步：解决真机/模拟器链接架构切换 (处理 Linker)
通过在 `configs` 节点中使用 `[sdk=*]` 条件宏，实现动态搜索路径切换：
```yaml
    settings:
      base:
        OTHER_LDFLAGS: $(inherited) -framework ComposeApp
      configs:
        Debug:
          "FRAMEWORK_SEARCH_PATHS[sdk=iphonesimulator*]": "$(inherited) $(SRCROOT)/../app/build/bin/iosSimulatorArm64/debugFramework"
          "FRAMEWORK_SEARCH_PATHS[sdk=iphoneos*]": "$(inherited) $(SRCROOT)/../app/build/bin/iosArm64/debugFramework"
```
*   **原理**: `OTHER_LDFLAGS` 告诉链接器要链接 `ComposeApp`。而具体的产物从哪里找，完全取决于当前选择的目标设备（`sdk` 宏）。

#### 第三步：自动化构建桥接
通过 Build Phase 脚本确保 KMP 框架在 Xcode 编译前总是最新状态：
```bash
cd "$SRCROOT/.."
export JAVA_HOME="$SRCROOT/../jdk-17.0.10+7/Contents/Home"
./gradlew :app:embedAndSignAppleFrameworkForXcode
```

---

## 4. 最佳实践建议

1.  **始终通过 XcodeGen 维护项目**: 不要直接在 Xcode UI 中修改 `Search Paths` 或 `Frameworks` 关联，这会导致配置漂移。修改 `project.yml` 后运行 `xcodegen generate`。
2.  **清理缓存**: 如果遇到极其顽固的模块找不到问题，请依次执行：
    *   `rm -rf iosApp/FitBot.xcodeproj`
    *   `./gradlew clean`
    *   `xcodegen generate`
3.  **Intel Mac 适配**: 本方案目前支持 Apple Silicon (M1/M2) 对应的 `Arm64` 模拟器和真机。如果使用 Intel Mac，需在 `project.yml` 的 `[sdk=iphonesimulator*]` 路径中映射为 `iosX64` 产物。

---
**FitBot Dev Team**  
*Last Updated: 2026-03-08*