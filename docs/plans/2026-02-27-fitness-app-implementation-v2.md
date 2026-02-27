# Fitness App MD3 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 基于 Material Design 3 规范重构 App，实现多 Tab 导航、计划归档及热力图统计。

**Architecture:** 引入 Navigation Compose 处理多级路由。使用 DataStore 或 Room 存储设置偏好。

---

### Task 1: 升级 MD3 依赖与导航架构

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/fitness/ui/navigation/NavGraph.kt`
- Modify: `app/src/main/java/com/fitness/MainActivity.kt`

**Step 1: 添加 Navigation 依赖**
添加 `androidx.navigation:navigation-compose`。

**Step 2: 实现根部 Scaffold 与底部导航**
使用 `NavigationBar` 和 `NavHost` 替代原有的简单状态切换。

**Step 3: Commit**
```bash
git add .
git commit -m "chore: setup MD3 navigation architecture and bottom bar"
```

### Task 2: 首页重构 - 分类动作库

**Files:**
- Modify: `app/src/main/java/com/fitness/model/Exercise.kt` (添加 category)
- Modify: `app/src/main/java/com/fitness/data/ExerciseProvider.kt` (丰富分类数据)
- Modify: `app/src/main/java/com/fitness/ui/library/ExerciseLibraryScreen.kt`

**Step 1: 实现 ScrollableTabRow**
根据动作分类过滤网格内容。

**Step 2: Commit**
```bash
git add .
git commit -m "feat: implement category-based filtering for exercise library"
```

### Task 3: 计划 Tab 与自动归档逻辑

**Files:**
- Create: `app/src/main/java/com/fitness/data/local/PlanEntity.kt`
- Modify: `app/src/main/java/com/fitness/data/local/AppDatabase.kt`
- Create: `app/src/main/java/com/fitness/ui/plans/PlanScreen.kt`

**Step 1: 编写 Room 数据库更新逻辑**
实现“更新即归档”功能。

**Step 2: Commit**
```bash
git add .
git commit -m "feat: implement training plans with versioned archiving"
```

### Task 4: 个人中心与健身热力图

**Files:**
- Create: `app/src/main/java/com/fitness/ui/profile/HeatMap.kt`
- Create: `app/src/main/java/com/fitness/ui/profile/ProfileScreen.kt`

**Step 1: 实现 Canvas 热力图与设置项**
实现统计图表及语言/主题设置。

**Step 2: Commit**
```bash
git add .
git commit -m "feat: add profile screen with github-style heatmap"
```
