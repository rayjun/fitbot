> **最后更新**: 2026-03-08
> **当前阶段**: 功能完善期
> **整体进度**: 核心功能完成，iOS 认证与云同步待实现

---

## 当前目标

维护可运行的跨平台健身追踪应用（Android + iOS 真机），下一步补全 iOS 认证和云同步后端。

---

## 功能模块状态

### 已完成

| 模块 | Android | iOS | 备注 |
|------|---------|-----|------|
| 运动库 | ✅ | ✅ | 18 个预设动作 + GIF 演示 |
| 训练计划 | ✅ | ✅ | 周计划编辑、日期管理 |
| 运动记录 | ✅ | ✅ | 组数增删改查 |
| 主题切换 | ✅ | ✅ | system / light / dark |
| 多语言 | ✅ | ✅ | 中文 + 英文 |
| 本地持久化 | ✅ Room + DataStore | ✅ DataStore | Android 用 Room，iOS 用 DataStore |
| 用户认证 | ✅ Google Sign-In | ⚠️ Mock | iOS 认证为开发占位，不可上线 |
| 热力图 | ✅ | ⚠️ 返回空 | iOS 的 `getHeatmapData()` 有 TODO |
| 云同步 | ⚠️ UI 骨架 | ⚠️ UI 骨架 | 后端逻辑未实现 |

### 待实现

#### iOS Google Sign-In
**目标**: 用真实 GIDSignIn SDK 替换 `AuthManager.ios.kt` 中的 Mock 实现
**文件**: `app/src/iosMain/kotlin/com/fitness/auth/AuthManager.ios.kt`
**标记**: `FIXME: Replace with native Google Sign-In SDK (GIDSignIn)`

#### iOS 热力图
**目标**: 实现 `DataStoreRepository.getHeatmapData()`，扫描所有 `history_` 前缀键
**文件**: `app/src/commonMain/kotlin/com/fitness/data/DataStoreRepository.kt`
**标记**: `TODO: Implement by scanning all history_ prefixed keys in DataStore`

#### 云同步后端
**目标**: 实现 Google Drive 同步逻辑，接通 `isSyncing` StateFlow
**参考**: `docs/GOOGLE_DRIVE_SYNC.md`

---

## 架构决策记录

### 决策 #1: SettingsRepository 从 WorkoutRepository 分离
**日期**: 2026-03-08
**决策**: 新增独立 `SettingsRepository` 接口，Settings 方法不再属于 `WorkoutRepository`
**理由**: 单一职责；SettingsViewModel 不应依赖运动数据接口
**影响**: Android 由 `RoomWorkoutRepository` 同时实现两个接口；iOS 由 `DataStoreRepository` 同时实现两个接口，各共享单一实例

### 决策 #2: Koin 在 Swift App.init() 中初始化
**日期**: 2026-03-08
**决策**: 将 `initKoin()` 从 Compose lambda 内移出，暴露为顶层函数 `setupKoin()`，由 Swift `App.init()` 调用
**理由**: Composition 重建时存在重复初始化风险；Koin 必须在 `KoinContext {}` 运行前就绪
**影响**: `iosApp.swift` 必须在 `WindowGroup` 初始化前调用 `MainViewControllerKt.setupKoin()`

### 决策 #3: Compose Resources 通过 Xcode Run Script 打包
**日期**: 2026-03-08
**决策**: 在 `FitBot.xcodeproj` 中添加 "Copy Compose Resources" Run Script Build Phase
**理由**: iOS 静态框架本身不包含资源文件，资源需作为 `compose-resources/` 目录放在 app bundle 根目录
**影响**: 每次 xcodebuild 构建自动执行，无需手动操作；资源来源为 `app/build/generated/compose/resourceGenerator/preparedResources/commonMain/composeResources`

---

## 最近修改

- **2026-03-08** 新增 `docs/README_IOS.md`：完整记录 iOS 编译、真机安装流程及所有踩坑
- **2026-03-08** 修复 Compose Resources 缺失崩溃：pbxproj 添加 Run Script
- **2026-03-08** 修复 Koin 未初始化崩溃：`iosApp.swift` 添加 `setupKoin()` 调用
- **2026-03-08** 分离 `SettingsRepository` 接口；删除调试代码；修复 DayDetailsScreen 数据竞争

---

## 阻塞问题

### 无阻塞

当前 Android 与 iOS 真机均可正常编译、安装、运行。

---

## 下次从这里开始

### 恢复上下文
1. 读取本文件
2. 读取 `docs/README_IOS.md` 了解 iOS 构建命令

### 推荐下一步

**A（推荐）— 实现 iOS 热力图**
修改 `DataStoreRepository.getHeatmapData()`，扫描 DataStore 中所有 `history_` 前缀键，统计每日组数。改动范围小，收益明显。

**B — 实现 iOS Google Sign-In**
需集成 `GoogleSignIn` CocoaPods/SPM 依赖，修改 `AuthManager.ios.kt`。改动涉及 Xcode 项目配置。

**C — 实现云同步后端**
参考 `docs/GOOGLE_DRIVE_SYNC.md`，改动范围最大。

---

## 相关文档

| 文档 | 说明 |
|------|------|
| `docs/README_IOS.md` | iOS 编译与真机安装完整指南 |
| `docs/README.md` | 项目概览 |
| `docs/KMP_IOS_INTEGRATION_GUIDE.md` | KMP iOS 集成技术细节 |
| `docs/GOOGLE_DRIVE_SYNC.md` | 云同步方案设计 |
