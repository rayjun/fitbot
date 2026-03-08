# 架构文档

## 技术栈概览

| 层次 | 技术 |
|------|------|
| 跨平台 UI | Kotlin Multiplatform + Compose Multiplatform 1.6.x |
| 状态管理 | Kotlin Coroutines + Flow + ViewModel |
| 依赖注入（通用） | Koin 3.5.3 |
| 依赖注入（Android） | Hilt 2.50（与 Koin 并存，处理 WorkManager） |
| 本地持久化（Android） | Room 2.6.1 |
| 本地持久化（iOS） | DataStore Preferences 1.1.0（OkioStorage） |
| 云同步（Android） | Google Drive API v3（google-api-services-drive） |
| 云同步（iOS） | Ktor 2.3.7（Darwin 引擎）直接调用 Drive REST API |
| 认证（Android） | Google Sign-In（Play Services Auth 21.0.0） |
| 认证（iOS） | GoogleSignIn CocoaPod 7.1（Swift 桥接到 Kotlin） |
| 图片加载（Android） | Coil 2.6.0（支持 GIF） |
| 图片加载（iOS） | Compose Resources + UIKitView（UIImageView） |
| 序列化 | kotlinx.serialization 1.6.2 |
| 日期时间 | kotlinx.datetime 0.5.0 |

---

## 项目结构

```
exercise/
├── app/                             # 主 KMP 模块
│   ├── src/
│   │   ├── commonMain/              # 共享代码（Android + iOS）
│   │   │   ├── kotlin/com/fitness/
│   │   │   │   ├── model/           # 数据模型
│   │   │   │   ├── data/            # Repository 接口 + 共享实现
│   │   │   │   ├── auth/            # AuthManager expect 类
│   │   │   │   ├── di/              # 共享 Koin 模块
│   │   │   │   ├── util/            # 多语言工具
│   │   │   │   └── ui/              # 所有 Compose 界面
│   │   │   │       ├── components/  # 通用组件（ExerciseImage）
│   │   │   │       ├── library/     # 动作库界面
│   │   │   │       ├── plans/       # 训练计划界面
│   │   │   │       ├── workout/     # 训练记录界面
│   │   │   │       ├── profile/     # 个人中心界面
│   │   │   │       ├── navigation/  # 路由定义
│   │   │   │       └── theme/       # 主题（expect）
│   │   │   └── composeResources/
│   │   │       └── files/exercises/ # 18 个动作 GIF 文件
│   │   ├── androidMain/             # Android specific actual 实现
│   │   │   └── kotlin/com/fitness/
│   │   │       ├── auth/            # AuthManager.android.kt
│   │   │       ├── di/              # Koin.android.kt
│   │   │       └── ui/theme/        # Theme.android.kt
│   │   ├── iosMain/                 # iOS specific actual 实现
│   │   │   └── kotlin/com/fitness/
│   │   │       ├── auth/            # AuthManager.ios.kt + GoogleAuthCallback.kt
│   │   │       ├── sync/            # DriveApiClient.kt + IosDriveSyncEngine.kt
│   │   │       ├── di/              # Koin.ios.kt
│   │   │       └── ui/              # Theme.ios.kt + ExerciseImage.ios.kt
│   │   └── main/                    # Android 传统目录
│   │       └── java/com/fitness/
│   │           ├── FitBotApp.kt
│   │           ├── MainActivity.kt
│   │           ├── sync/            # SyncWorker + DriveServiceHelper
│   │           ├── data/local/      # Room DAOs + Entities + AppDatabase
│   │           └── ui/navigation/   # FitBotNavHost（Android nav）
├── iosApp/
│   ├── iosApp/
│   │   ├── iosApp.swift             # @main App 入口
│   │   ├── ContentView.swift        # SwiftUI 根视图
│   │   └── GoogleSignInBridge.swift # Swift-Kotlin 登录桥接
│   ├── project.yml                  # XcodeGen 配置
│   ├── FitBot.xcodeproj/            # 由 XcodeGen 生成
│   ├── FitBot.xcworkspace/          # CocoaPods 集成后使用
│   └── Podfile                      # CocoaPods 依赖
└── build.gradle.kts                 # 根 Gradle 配置
```

---

## 分层架构

```
┌──────────────────────────────────────────────────┐
│                   UI Layer (commonMain)           │
│  LibraryScreen  PlansScreen  WorkoutScreen  ...  │
├──────────────────────────────────────────────────┤
│              ViewModel Layer (commonMain)         │
│  PlanViewModel  WorkoutViewModel  ProfileViewModel│
├──────────────────────────────────────────────────┤
│           Repository Interface (commonMain)       │
│       WorkoutRepository  SettingsRepository       │
├────────────────────┬─────────────────────────────┤
│  Android           │         iOS                  │
│  RoomWorkout       │  DataStoreRepository         │
│  Repository        │  (DataStore + OkioStorage)   │
│  (Room + DataStore)│                              │
├────────────────────┴─────────────────────────────┤
│              Sync Layer                           │
│  Android: SyncWorker (WorkManager)                │
│  iOS:     IosDriveSyncEngine (Ktor)               │
│  Both → Google Drive API v3                       │
└──────────────────────────────────────────────────┘
```

---

## expect/actual 机制

Compose Multiplatform 通过 `expect/actual` 对平台差异进行隔离。项目中使用 `expect/actual` 的位置：

| expect（commonMain） | Android actual | iOS actual |
|----------------------|----------------|------------|
| `AuthManager` | 使用 Google Sign-In SDK + WorkManager | suspendCancellableCoroutine + signInLauncher 回调 |
| `ExerciseImage` | Coil + GIF Decoder | Compose Resources readBytes + UIKitView |
| `FitnessTheme` | Material3 动态颜色主题 | 静态 Material3 主题 |
| `platformModule`（Koin） | Room DB + DataStore 路径 | DataStore（NSDocumentDirectory）+ AuthManager |

---

## 依赖注入

### 共享模块（commonMain/di/Koin.kt）

```kotlin
val commonModule = module {
    single { DataStoreRepository(get()) }
    single<WorkoutRepository> { get<DataStoreRepository>() }
    single<SettingsRepository> { get<DataStoreRepository>() }

    viewModel { PlanViewModel(get()) }
    viewModel { WorkoutViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { ProfileViewModel(get()) }
}
```

### iOS 平台模块（iosMain/di/Koin.ios.kt）

```kotlin
actual val platformModule = module {
    // DataStore 存储在 ~/Documents/fitness_settings.preferences_pb
    single<DataStore<Preferences>> { ... }
    single { AuthManager(get<DataStoreRepository>()) }
}
```

### Android 平台模块（androidMain/di/Koin.android.kt）

```kotlin
actual val platformModule = module {
    single<DataStore<Preferences>> { ... }
    // Android 使用 Room 而不是 DataStore 存储训练数据
    single { RoomWorkoutRepository(get(), get(), get()) }
    single<WorkoutRepository> { get<RoomWorkoutRepository>() }
    single<SettingsRepository> { get<RoomWorkoutRepository>() }
}
```

> **注意**：Android 还额外使用 Hilt 管理 `SyncWorker`（WorkManager Worker 需要 `@HiltWorker` 注入），与 Koin 共存。

---

## 数据模型

### Exercise（动作定义）

```kotlin
@Serializable
data class Exercise(
    val id: String,                // "benchpress"
    val nameKey: String,           // 国际化 key，如 "ex_benchpress_name"
    val gifResPath: String,        // "exercises/benchpress.gif"
    val descriptionKey: String,
    val targetMuscleKey: String,   // "muscle_chest"
    val categoryKey: String,       // "cat_chest"
    val isBodyweight: Boolean = false
)
```

### ExerciseSet（训练组记录）

```kotlin
@Serializable
data class ExerciseSet(
    val id: Long = 0,
    val date: String,              // "2024-03-08"
    val sessionId: String,         // "Session_<timestamp>"
    val exerciseName: String,      // 对应 Exercise.id
    val reps: Int,
    val weight: Double,            // bodyweight 时为 0.0
    val timestamp: Long,           // epoch ms
    val timeStr: String,           // "14:30"
    val remoteId: String = ""      // Google Drive 去重标识
)
```

### RoutineDay（周计划单日）

```kotlin
@Serializable
data class PlannedExercise(
    val id: String,
    val targetSets: Int = 3
)

@Serializable
data class RoutineDay(
    val dayOfWeek: Int,            // 1=周一 … 7=周日
    val isRest: Boolean,
    val exercises: List<PlannedExercise>
)
```

### TrainingDay / TrainingSession（同步数据格式）

```kotlin
@Serializable
data class TrainingDay(
    val date: String,
    val sessions: List<TrainingSession>
)

@Serializable
data class TrainingSession(
    val sessionId: String,
    val startTime: String,
    val endTime: String,
    val exercises: List<ExerciseRecord>
)

@Serializable
data class ExerciseRecord(
    val name: String,
    val sets: List<SetRecord>
)

@Serializable
data class SetRecord(
    val reps: Int,
    val weight: Double,
    val time: String,
    val remoteId: String
)
```

---

## 导航架构

### Android（Compose Navigation）

`FitBotNavHost.kt` 使用 `NavHost` 管理路由：

```
底部导航：Library / Plans / Profile
├── Library → ExerciseDetailScreen(exerciseId)
│                └── WorkoutRecordingScreen(exerciseId, date)
├── Plans → DayDetailsScreen(date)
│           └── WorkoutRecordingScreen(exerciseId, date)
└── Profile → SettingsScreen
```

### iOS（状态驱动导航）

`MainViewController.kt` 用状态变量模拟路由：

```kotlin
var currentScreen: Screen        // 底部 tab 切换
var selectedExercise: Exercise?  // 覆盖显示 ExerciseDetailScreen
var workoutExerciseId: String?   // 覆盖显示 WorkoutRecordingScreen
var workoutDate: String?
var dayDetailsDate: String?      // 覆盖显示 DayDetailsScreen
```

渲染优先级（从高到低）：
1. workoutExerciseId != null → WorkoutRecordingScreen
2. dayDetailsDate != null → DayDetailsScreen
3. selectedExercise != null → ExerciseDetailScreen
4. currentScreen → Library / Plans / Profile / Settings
