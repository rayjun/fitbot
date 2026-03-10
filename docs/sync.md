# Google Drive 同步架构

## 概述

FitBot 使用 Google Drive（用户个人云盘）实现 Android ↔ iOS 双向数据同步。
文件统一存储于 Drive 根目录下的 **`FitBot/`** 文件夹中。

---

## 文件结构（Drive 端）

```
FitBot/
├── YYYY-MM-DD.json      # 按日期存储的训练组数据，一天一个文件
│   └── 例：2024-03-08.json
├── plans.json           # 当前周训练计划（RoutineDay 列表）
└── user_prefs.json      # 用户偏好（主题、语言、座右铭）
```

---

## 数据格式

### YYYY-MM-DD.json（训练日数据）

对应 Kotlin 模型 `TrainingDay`：

```json
{
  "date": "2024-03-08",
  "sessions": [
    {
      "sessionId": "Session_1709856000000",
      "startTime": "14:30",
      "endTime": "15:10",
      "exercises": [
        {
          "name": "benchpress",
          "sets": [
            { "reps": 12, "weight": 80.0, "time": "14:32", "remoteId": "" },
            { "reps": 10, "weight": 82.5, "time": "14:40", "remoteId": "" }
          ]
        }
      ]
    }
  ]
}
```

### plans.json

**iOS 写入格式**（`List<RoutineDay>` 直接序列化）：
```json
[
  { "dayOfWeek": 1, "isRest": false, "exercises": [{ "id": "benchpress", "targetSets": 3 }] },
  { "dayOfWeek": 2, "isRest": false, "exercises": [{ "id": "squat", "targetSets": 4 }] },
  { "dayOfWeek": 3, "isRest": true, "exercises": [] },
  ...
]
```

**Android 写入格式**（`List<PlanEntity>` 包裹，Room 实体）：
```json
[
  {
    "id": 1,
    "name": "Daily Routine",
    "exercisesJson": "[{\"dayOfWeek\":1,\"isRest\":false,...}]",
    "isCurrent": true,
    "version": 0,
    "createdAt": 1709856000000
  }
]
```
iOS 读取时先尝试 Android 格式（解析 `exercisesJson` 内层），失败则回退到 iOS 格式。

### user_prefs.json

```json
{
  "theme_mode": "dark",
  "language": "zh",
  "user_quote": "保持训练，保持进步"
}
```

---

## 同步逻辑

### 三个同步阶段

```
sync()
├── syncSetsLogic()    — 训练组数据（按日期）
├── syncPlansLogic()   — 周训练计划
└── syncPrefs()        — 用户偏好
```

### 同步方向判断

每个阶段通过比较 **远端文件 modifiedTime** 与 **本地 last_sync_time** 决定方向：

```kotlin
val shouldDownload = remoteFile != null &&
    (lastSync == 0L || remoteModifiedMs > lastSync)

if (shouldDownload) {
    // 下载并合并远端数据到本地
} else {
    // 上传本地数据到远端（如有变化）
}
```

- `lastSync == 0L`：首次同步，强制下载远端数据
- `remoteModifiedMs > lastSync`：远端更新，下载合并
- 否则：本地是最新的，上传到远端

同步完成后更新 `last_sync_time`（DataStore key：`last_sync_time`）。

### 训练组数据去重与冲突合并 (Fetch-Merge-Upload)

为防止多设备离线修改在同时连网时导致的数据相互覆盖，系统采用 **Fetch-Merge-Upload (下载-合并-上传)** 的闭环机制和**软删除 (Tombstone)**：

1. **软删除支持**：`ExerciseSet` 具有 `isDeleted: Boolean` 属性（兼容处理旧数据默认为 false）。在用户执行删除操作时，数据库仅修改标记为 true，保留该记录以便跨设备同步这一“删除意图”。
2. **下载远端**：在尝试将本地修改推送到远端之前，首先调用 `drive.downloadFile(id)` 获取最新的远端版本。
3. **在内存合并 (`mergeTrainingDays` 纯函数)**：
   - 优先通过 `remoteId` 进行精确去重（确保修改行为作用于同一实体）。
   - 若 `remoteId` 缺失，降级使用 `exerciseName + timeStr` 组合键比对。
   - 当两条记录相遇时，如果有一侧标记为 `isDeleted = true`，则最终合并版本也会标记为删除（Tombstone 优先）。
4. **安全覆盖**：将融合了双方所有独立记录（保留增量修改与删除标记）的最终版本序列化，通过 `drive.updateFile()` 写入云端。

---

## 平台实现差异

### Android（SyncWorker.kt）

```
WorkManager CoroutineWorker
    └── DriveServiceHelper（google-api-services-drive）
            ├── Drive.Files.List  — 列举文件
            ├── Drive.Files.Get   — 下载内容
            ├── Drive.Files.Create — 创建文件
            └── Drive.Files.Update — 更新文件
```

- 访问 Room DAOs 获取本地数据
- 由 `MainActivity` 登录后通过 `WorkManager.enqueueUniqueWork` 触发
- 也可通过设置页"立即同步"手动触发

### iOS（IosDriveSyncEngine.kt + DriveApiClient.kt）

```
IosDriveSyncEngine
    └── DriveApiClient（Ktor HttpClient + Darwin engine）
            ├── GET  /drive/v3/files?q=...  — 列举文件
            ├── GET  /drive/v3/files/{id}?alt=media — 下载内容
            ├── POST /upload/drive/v3/files?uploadType=multipart — 创建
            └── PATCH /upload/drive/v3/files/{id}?uploadType=multipart — 更新
```

- 访问 DataStoreRepository 获取本地数据
- 由 `AuthManager.sync()` 调用，入口在设置页"立即同步"按钮
- 使用 Bearer token 认证（access token 由 GoogleSignIn 获取）

### DriveApiClient 文件上传格式

创建 / 更新文件使用 **multipart/related** 格式：

```
--boundary
Content-Type: application/json
{ "name": "2024-03-08.json", "parents": ["<folderId>"] }
--boundary
Content-Type: application/json
{ ...文件内容... }
--boundary--
```

---

## Google OAuth 配置

### Android
- `OAuth 2.0 Client ID` 类型：Android
- 配置在 `google-services.json`（不纳入版本控制）
- Scope：`https://www.googleapis.com/auth/drive.file`

### iOS
- `OAuth 2.0 Client ID` 类型：iOS，Bundle ID：`com.fitness.FitBot`
- Client ID：`1065156283555-rd4u52pj3e0nid1av7almjgk5r6mme19.apps.googleusercontent.com`
- 配置在 `iosApp/iosApp/Info.plist`：
  ```xml
  <key>GIDClientID</key>
  <string>1065156283555-rd4u52pj3e0nid1av7almjgk5r6mme19.apps.googleusercontent.com</string>
  <key>CFBundleURLSchemes</key>
  <array>
    <string>com.googleusercontent.apps.1065156283555-rd4u52pj3e0nid1av7almjgk5r6mme19</string>
  </array>
  ```

---

## iOS 登录桥接（Swift ↔ Kotlin）

### 接口定义（GoogleAuthCallback.kt）
```kotlin
interface GoogleAuthCallback {
    fun onSignInSuccess(userId: String, userName: String?, userEmail: String?, accessToken: String)
    fun onSignInFailed(error: String)
}
```

### AuthManager.ios.kt 登录流程
```kotlin
actual suspend fun signIn() = suspendCancellableCoroutine { cont ->
    signInLauncher?.invoke(object : GoogleAuthCallback {
        override fun onSignInSuccess(..., accessToken: String) {
            currentAccessToken = accessToken
            _currentUser.value = UserProfile(...)
            cont.resume(Unit)
        }
        override fun onSignInFailed(error: String) { cont.resume(Unit) }
    })
}
```

### GoogleSignInBridge.swift
```swift
static func register(_ authManager: AuthManager) {
    authManager.signInLauncher = { callback in
        GIDSignIn.sharedInstance.signIn(
            withPresenting: rootVC,
            additionalScopes: ["https://www.googleapis.com/auth/drive.file"]
        ) { result, error in
            result?.user.refreshTokensIfNeeded { user, error in
                callback.onSignInSuccess(
                    userId: user?.userID ?? "",
                    userName: user?.profile?.name,
                    userEmail: user?.profile?.email,
                    accessToken: user!.accessToken.tokenString
                )
            }
        }
    }
}
```

### App 启动时注册
```swift
// iosApp.swift
@main struct iosApp: App {
    init() {
        MainViewControllerKt.setupKoin()
        let authManager = MainViewControllerKt.getAuthManager()
        GoogleSignInBridge.register(authManager)
    }
    var body: some Scene {
        WindowGroup {
            ContentView().onOpenURL { url in GIDSignIn.sharedInstance.handle(url) }
        }
    }
}
```
