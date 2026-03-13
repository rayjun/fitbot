# 功能模块文档

## 1. 动作库（Exercise Library）

### 界面文件
`ui/library/ExerciseLibraryScreen.kt` · `ui/library/ExerciseDetailScreen.kt`

### 功能描述
- 展示全部 18 个内置动作，按肌肉群分类（全部 / 胸部 / 肩部 / 背部 / 手臂 / 腿部 / 核心 / 全身）
- 2 列卡片网格，每张卡片显示 GIF 动图 + 动作名 + 目标肌群
- 点击卡片进入详情页：大图 GIF + 目标肌群 + 动作要领
- 点击详情页上方按钮可直接跳转到该动作的训练记录页

### 数据来源
`ExerciseProvider.kt` — 静态列表，包含 18 个 `Exercise` 对象，GIF 资源存储于
`app/src/commonMain/composeResources/files/exercises/*.gif`

### GIF 加载机制

| 平台 | 实现 |
|------|------|
| Android | Coil 2.6.0 + `GifDecoder`（API<28）/ `ImageDecoderDecoder`（API≥28），加载路径：`file:///android_asset/{gifResPath}` |
| iOS | `org.jetbrains.compose.resources.readResourceBytes("files/{gifResPath}")` → `NSData` → `UIImage.imageWithData()` → `UIKitView { UIImageView }` |

### 分类 Tab 实现
```kotlin
ScrollableTabRow(selectedTabIndex = ...) {
    ExerciseProvider.categories.forEach { categoryKey ->
        Tab(selected = ..., onClick = { selectedCategoryKey = categoryKey })
    }
}
```
点击 Tab 后 `filteredExercises` 通过 `remember(selectedCategoryKey)` 重新计算。

---

## 2. 训练计划（Plans）

### 界面文件
`ui/plans/PlansScreen.kt` · `ui/plans/PlanViewModel.kt`

### 功能描述
- 水平翻页（`HorizontalPager`）浏览不同周，显示真实日历日期范围（如 "3/3 – 3/9"）
- 7 个点击式日期按钮（M T W T F S S），当前周今天自动选中
  - **进度可视化**：日期方块背景色会根据当天的训练计划完成比例（`completionRatio`）动态加深透明度，全部完成时会显示白色 `✔` 图标。
- 每天可单独设置为休息日 / 训练日
- 训练日显示该天的动作列表，每个动作卡片显示：
  - 目标肌群标签（primary 色）
  - 动作名称
  - 已完成组数 / 目标组数进度条 + 文字（可点击调整目标组数）
  - 开始训练按钮（仅在查看**当天**计划时显示）/ 完成勾标（完成后）
  - 删除按钮
- "添加动作" 按钮打开 `AddExerciseDialog`

### AddExerciseDialog 交互
- 显示全部 18 个动作，已在计划中的预先勾选并显示当前组数
- 勾选某动作后，行末出现 `−` / 数字 / `+` 步进器（范围 1–10，默认 3）
- 每个动作组数独立控制
- 保存时以对话框状态完整替换当天的动作列表（覆盖合并，非追加）

### 目标组数编辑 Dialog
点击进度文字（如 "0 / 3"）打开独立 AlertDialog，可对单个已添加动作调整目标组数。

### ViewModel 数据流
```
DataStoreRepository.getCurrentRoutine() [Flow<List<RoutineDay>>]
    → PlanViewModel.currentRoutine [StateFlow]
    → PlansScreen(currentRoutine = ...)
```
修改计划：
```
onUpdatePlanDay(dayOfWeek, isRest, exercises)
    → PlanViewModel.updatePlanDay()
    → repository.updateRoutineDay()
    → DataStore.edit { preferences[ROUTINE_KEY] = json }
    → Flow 触发重组
```

### 周日期计算
```kotlin
fun mondayOfWeek(weekOffset: Int): LocalDate {
    val today = Clock.System.now().toLocalDateTime(...).date
    val daysSinceMonday = today.dayOfWeek.ordinal  // Mon=0
    val thisMonday = today.minus(daysSinceMonday, DateTimeUnit.DAY)
    return thisMonday.plus(weekOffset * 7, DateTimeUnit.DAY)
}
```

---

## 3. 训练记录（Workout Recording）

### 界面文件
`ui/workout/WorkoutRecordingScreen.kt` · `ui/workout/WorkoutViewModel.kt`

### 功能描述
- 顶部显示动作名（国际化）+ 返回按钮
- 列表显示当日该动作的所有已记录组（有重量：`80 kg × 12`；徒手：`12 次`）
- **历史数据保护**：为了保证数据的真实性，只有在查看**当天**记录时：
  - 右下角才会显示浮动的 `+` 按钮以打开添加记录对话框。
  - 才允许点击已有记录来编辑重量 / 次数，或进行删除操作。
- 在查看历史或未来日期的记录时，页面完全处于只读模式。

### RecordSetDialog
```
├── 有重量动作：重量输入框（数字键盘）+ 次数输入框
└── 徒手动作：只有次数输入框
```
编辑模式额外显示左下角删除按钮（红色）。

### 数据响应性
```kotlin
val setsFlow = remember(repository, date) { repository.getSetsByDate(date) }
val sets by setsFlow.collectAsState(initial = emptyList())
```
`remember` 确保 Flow 对象稳定，DataStore `edit` 完成后 Flow 立即 emit，触发重组。

### ExerciseSet 创建逻辑
```kotlin
ExerciseSet(
    date = date,
    sessionId = "Session_${now.toEpochMilliseconds()}",
    exerciseName = exerciseId,
    reps = reps,
    weight = weight,
    timestamp = now.toEpochMilliseconds(),
    timeStr = "HH:mm"
)
```

---

## 4. 个人中心（Profile）

### 界面文件
`ui/profile/ProfileScreen.kt` · `ui/profile/ProfileViewModel.kt`

### 功能描述

#### 用户账户卡片
- 未登录：显示匿名头像 + "登录 Google Drive" 按钮
- 已登录：Google 账户头像（首字母 placeholder）+ 账户名 + 可编辑座右铭

#### 训练热力图（WorkoutHeatMap）
仿 GitHub contribution graph，展示近 N 周的训练密度：

```kotlin
@Composable
fun WorkoutHeatMap(data: Map<String, Int>) {
    // BoxWithConstraints 动态计算列数
    val columns = (maxWidth / (cellSize + spacing)).toInt()
    val totalDays = columns * 7
    // 生成最近 totalDays 天的日期列表，倒序排列后按 7 天分组
    // 每格颜色由 data[dateStr] 决定：
    //   0 → alpha 0.05f（近乎透明）
    //   1–4 → #40C463（浅绿）
    //   5–14 → #30A14E（中绿）
    //   15+ → #216E39（深绿）
}
```

热力图数据来源：
```kotlin
DataStoreRepository.getHeatmapData()
// 扫描所有 history_YYYY-MM-DD 前缀的 DataStore key
// 反序列化 List<ExerciseSet> 后统计 size，返回 Map<String, Int>
```

#### 座右铭编辑
点击座右铭文字或编辑图标，弹出 AlertDialog，修改后通过 `SettingsViewModel.setUserQuote()` 写入 DataStore。

---

## 5. 设置（Settings）

### 界面文件
`ui/profile/SettingsScreen.kt` · `ui/profile/SettingsViewModel.kt`

### 功能描述

| 设置项 | 可选值 | 存储 key |
|--------|--------|---------|
| 主题模式 | system / light / dark | `theme_mode` |
| 语言 | en / zh | `language` |

### SettingsViewModel 数据流
```kotlin
val themeMode = repository.getThemeMode().stateIn(...)
val language  = repository.getLanguage().stateIn(...)
val userQuote = repository.getUserQuote().stateIn(...)
```
设置修改后立即通过 DataStore Flow 传播到全局 CompositionLocal：
```kotlin
CompositionLocalProvider(LocalAppLanguage provides language) { ... }
```

### 主题切换
```kotlin
val isDark = when (themeMode) {
    "dark"  -> true
    "light" -> false
    else    -> isSystemInDarkTheme()
}
FitnessTheme(darkTheme = isDark) { ... }
```

---

## 6. 云同步（Cloud Sync）

> 详细架构见 [sync.md](./sync.md)

### 入口

| 平台 | 触发方式 |
|------|---------|
| Android | 登录后自动触发 WorkManager 一次性任务；设置页可手动点击"立即同步" |
| iOS | 设置页点击"立即同步"后调用 `authManager.sync()` |

### 同步状态展示
```kotlin
val isSyncing by authManager.isSyncing.collectAsState()
// SettingsScreen 根据 isSyncing 显示 "正在同步..." 或 "立即同步"
```

---

## 7. 多语言（Localization）

> 详细实现见 [localization.md](./localization.md)

- 支持中文（简体）和英文
- 字符串表定义在 `util/ResourceUtils.kt`（`stringsEn` / `stringsZh` Map）
- 通过 `CompositionLocal` `LocalAppLanguage` 在整棵 Compose 树中透传当前语言
- 所有界面使用 `getString(key)` 获取当前语言字符串
- 语言切换即时生效，无需重启

### Advanced Analytics (v0.6.12)
- **Time-series Filtering**: View workout volume aggregated by Week, Month, or Year.
- **Muscle Category Drill-down**: Filter all charts by specific muscle groups (Chest, Back, etc.) using a streamlined tab interface.
- **Pure Canvas Charts**: Responsive Radar and Bar charts built with zero dependencies.

### AI Personal Coach (v0.7.9)
- **Settings Configuration**: Securely store API Key, Base URL, and Model Name in local DataStore for privacy. Support any OpenAI-compatible endpoints (DeepSeek, Llama, etc.).
- **Smart Analytics Insight**: Dynamically generate fitness performance summaries based on time ranges (Week/Month/Year) directly in the Analytics dashboard.
- **AI Coach Chat**: A classic, polished chat interface allowing users to ask questions grounded in their actual workout data using dynamic prompt injection.
- **Privacy-First Data Flow**: AI only receives aggregated statistics (muscle group volume, sets, and progress), never raw personal details or precise timestamps.
