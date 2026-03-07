# Kotlin Multiplatform (KMP) iOS 集成技术总结

本文档总结了 FitBot 项目在 KMP 迁移过程中，iOS 端遇到的核心编译与链接问题及其最终解决方案。

## 1. 核心问题回顾

在非 CocoaPods 模式下集成 KMP 框架时，我们遇到了两个交替出现的典型错误：

### 问题 A: `Unable to find module dependency: 'ComposeApp'`
*   **现象**: Swift 代码中 `import ComposeApp` 报错，且代码补全失效。
*   **原因**: Swift 编译器在索引（Indexing）阶段无法在 `FRAMEWORK_SEARCH_PATHS` 中定位到正确的 `module.modulemap` 文件。

### 问题 B: `Linker architecture mismatch (built for iOS-simulator)`
*   **现象**: 编译报错：`Building for 'iOS', but linking in object file... built for 'iOS-simulator'`。
*   **原因**: 在 Xcode 的 `dependencies` 中硬编码了指向模拟器架构的 `.framework` 物理路径。当尝试在真机上运行（或反之）时，链接器被强制使用了错误的二进制文件。

---

## 2. 根源分析

KMP 默认的编译产物路径（`app/build/bin/`）区分非常细：
*   模拟器: `iosSimulatorArm64`
*   真机: `iosArm64`

传统的 Xcode 集成方式（手动拖入或单一路径 Search Path）无法在“代码索引阶段”和“链接阶段”同时处理这种物理隔离。如果将所有路径都加入 Search Path，Xcode 的链接器可能会由于搜索顺序问题（Weight）先匹配到错误的架构。

---

## 3. 最终解决方案：SDK 条件搜索路径 (Definitive Solution)

我们采用了一套基于 **Xcode 宏条件过滤** 的健壮方案，彻底解决了上述冲突。

### 核心配置 (project.yml)

#### A. 条件搜索路径 (Conditional Search Paths)
利用 Xcode 的 `[sdk=*]` 语法实现架构级别的物理隔离：
```yaml
configs:
  Debug:
    # 选模拟器时，只看模拟器产物目录
    "FRAMEWORK_SEARCH_PATHS[sdk=iphonesimulator*]": "$(inherited) $(SRCROOT)/../app/build/bin/iosSimulatorArm64/debugFramework"
    # 选真机时，只看真机产物目录
    "FRAMEWORK_SEARCH_PATHS[sdk=iphoneos*]": "$(inherited) $(SRCROOT)/../app/build/bin/iosArm64/debugFramework"
```

#### B. 解耦式链接 (Library-name Linking)
删除了 `targets` 下的硬编码文件引用，改用链接器标志：
```yaml
OTHER_LDFLAGS: $(inherited) -framework ComposeApp
```
*   **效果**: 告诉链接器“寻找 ComposeApp”，但由 Xcode 根据当前的 SDK 类型动态从上述“条件搜索路径”中挑选正确的二进制文件。

#### C. 自动化构建桥接
通过 Build Phase 脚本确保 KMP 框架与 Xcode 步调一致：
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
3.  **架构匹配**: 本方案目前支持 M1/M2 Mac (Arm64 模拟器) 和真机。如果使用 Intel Mac，需在 `project.yml` 中补全 `iosX64` 的路径映射。

---
**FitBot Dev Team**  
*Last Updated: 2026-03-07*
