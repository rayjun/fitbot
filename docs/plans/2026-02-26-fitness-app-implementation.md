# 健身 App 实施计划 (Fitness Tracker Implementation Plan)

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 开发一款原生 Android App，内置健身动作 GIF 库，记录训练数据并同步至 Google Drive JSON 文件。

**Architecture:** 采用 MVVM 架构。本地使用 Room 持久化，云端使用 Google Drive REST API。通过 WorkManager 处理增量同步。

**Tech Stack:** Kotlin, Jetpack Compose, Room DB, WorkManager, Google Drive REST API SDK.

---

### Task 1: 初始化 Android 项目脚手架

**Files:**
- Create: `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `app/src/main/AndroidManifest.xml`

**Step 1: 使用 Gradle 创建项目基础结构**
运行：`gradle init --type basic` 并按照 Android 标准结构配置。

**Step 2: 验证编译配置**
运行：`./gradlew assembleDebug`
Expected: 成功生成 APK（或通过编译）。

**Step 3: Commit**
```bash
git add .
git commit -m "chore: initialize android project with compose and room dependencies"
```

---

### Task 2: 定义数据模型与 Room 数据库

**Files:**
- Create: `app/src/main/java/com/fitness/model/TrainingData.kt`
- Create: `app/src/main/java/com/fitness/data/local/SetEntity.kt`
- Create: `app/src/main/java/com/fitness/data/local/AppDatabase.kt`

**Step 1: 编写数据实体测试（验证 JSON 序列化）**
编写测试确保对象能正确转换为设计要求的 JSON 格式。

**Step 2: 编写 Room Entity 和 DAO**
实现 `SetEntity` 和 `ExerciseDao` 以存储每日训练数据。

**Step 3: 验证本地存储读写**
运行：Android Instrumented Tests 验证 Room 数据库读写成功。

**Step 4: Commit**
```bash
git add app/src/main/java/com/fitness/data/
git commit -m "feat: implement local storage schema and data models"
```

---

### Task 3: 实现内置动作库 (Exercise Library)

**Files:**
- Create: `app/src/main/assets/exercises/` (存放 GIF)
- Create: `app/src/main/java/com/fitness/ui/library/ExerciseLibraryScreen.kt`

**Step 1: 导入资源并编写列表 UI**
使用 `LazyColumn` 显示动作名称和预览图。

**Step 2: 编写单元测试**
验证动作库能否正确加载 assets 目录下的所有动作数据。

**Step 3: Commit**
```bash
git add app/src/main/assets/ app/src/main/java/com/fitness/ui/library/
git commit -m "feat: add built-in exercise library with GIF support"
```

---

### Task 4: 实现 Google Drive 同步引擎

**Files:**
- Create: `app/src/main/java/com/fitness/sync/DriveServiceHelper.kt`
- Create: `app/src/main/java/com/fitness/sync/SyncWorker.kt`

**Step 1: 集成 Google Auth 和 Drive API**
配置 OAuth 客户端 ID 并在 `DriveServiceHelper` 中实现文件读写方法。

**Step 2: 编写同步逻辑**
实现逻辑：读取当日所有 Room 记录 -> 转换 JSON -> 写入/更新 Drive 文件。

**Step 3: Commit**
```bash
git add app/src/main/java/com/fitness/sync/
git commit -m "feat: implement real-time Google Drive sync using WorkManager"
```
