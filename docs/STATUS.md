> **最后更新**: 2026-03-08
> **当前阶段**: 功能完善期
> **整体进度**: 核心功能已全部完成，Android 与 iOS 均支持云同步与热力图

---

## 当前目标

维护可运行的跨平台健身追踪应用（Android + iOS 真机），确保双端数据一致性与同步稳定性。

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
| 本地持久化 | ✅ Room + DataStore | ✅ DataStore | iOS 端使用 DataStore + HISTORY_KEY_PREFIX 实现 |
| 用户认证 | ✅ Google Sign-In | ✅ Google Sign-In | 双端均支持静默登录 (Silent Sign-In)，重启 App 不再掉线 |
| 热力图 | ✅ | ✅ | 已通过单元测试验证逻辑准确性 |
| 数据分析 | ✅ | ✅ | 全新数据面板，包含雷达图、柱状图及纯 Canvas 绘制的人体肌肉热力图 |
| 云同步 | ✅ | ✅ | 实现 Fetch-Merge-Upload 防覆盖闭环、软删除及 iOS Datastore 性能优化 |

### 待优化

#### DataStore 数据量长期积压
**目标**: 虽然通过分离 `dirty_dates` 解决了单次修改扫描的性能瓶颈，但在 iOS 侧长期累积数年的 `HISTORY_KEY_PREFIX` 数据依然会随着时间使 `Preferences` 文件臃肿。需考虑引入分页加载或 SQLDelight 数据库化。

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

### 决策 #4: iOS 使用 DataStore 存储训练历史
**日期**: 2026-03-08
**决策**: iOS 端不使用 SQLDelight/Room，而是通过 DataStore 以 `history_YYYY-MM-DD` 为键存储 JSON 字符串
**理由**: 简化初期跨平台数据同步逻辑，iOS 端无需处理复杂的数据库迁移
**影响**: `DataStoreRepository.kt` 中实现了扫描所有 `history_` 前缀键以生成热力图数据的功能

---

## 最近修改

- **2026-03-12** 完成“高级数据分析”模块：增加了包含雷达图 (肌肉均衡度)、交互式人体肌肉热力图 (Physique Focus) 和训练总容量柱状图的专业数据面板。重构了个人中心页面，新增全局设置与数据入口。
- **2026-03-10** 优化全局 UI 布局：统一所有主页面的顶部标题栏为紧凑型 `CompactTopAppBar` (56dp)，移除 iOS 底部导航栏上方的多余遮挡安全区，并将底部导航栏高度压缩至更紧凑的视觉效果。
- **2026-03-10** 优化 `PlansScreen` 用户体验：非当天日期的训练记录变为只读，防止篡改历史记录；周历卡片根据当天完成组数比例动态加深颜色。
- **2026-03-10** 修复非当天日期的训练记录在 `PlansScreen` 中无法正确展示及历史打勾状态丢失的问题，补充了 `allSetsByDate` 映射逻辑及单测。
- **2026-03-08** 实现 iOS Google Sign-In 原生集成（通过 `GoogleSignInBridge` 与 `AuthManager` 协作）。
- **2026-03-08** 实现 iOS 云同步引擎 `IosDriveSyncEngine`，支持与 Android 端的数据互通。
- **2026-03-08** 实现 iOS 热力图扫描逻辑，补全 `DataStoreRepository.getHeatmapData()`。
- **2026-03-08** 新增 `docs/README_IOS.md`：完整记录 iOS 编译、真机安装流程及所有踩坑。
- **2026-03-08** 修复 Compose Resources 缺失崩溃：pbxproj 添加 Run Script。
- **2026-03-08** 修复 Koin 未初始化崩溃：`iosApp.swift` 添加 `setupKoin()` 调用。

---

## 阻塞问题

### 无阻塞

当前 Android 与 iOS 真机均可正常编译、安装、运行，核心功能全量通过。

---

## 相关文档

| 文档 | 说明 |
|------|------|
| `docs/README_IOS.md` | iOS 编译与真机安装完整指南 |
| `docs/README.md` | 项目概览 |
| `docs/KMP_IOS_INTEGRATION_GUIDE.md` | KMP iOS 集成技术细节 |
| `docs/GOOGLE_DRIVE_SYNC.md` | 云同步方案设计 |
