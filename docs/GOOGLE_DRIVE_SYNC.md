# Google Drive 同步系统技术文档

本系统为 FitBot 提供基于 Google Drive 的全量云同步功能，支持多设备间的增量数据同步、用户偏好同步及训练计划同步。

## 1. 核心架构

同步系统由以下核心组件构成：

*   **`SyncWorker`**: 基于 `WorkManager` 实现的后台异步任务，支持 Hilt 依赖注入。它是同步逻辑的调度中心。
*   **`DriveServiceHelper`**: 对 Google Drive REST API v3 的封装，提供文件夹管理、文件读写、元数据查询等原子操作。
*   **`AuthManager`**: 负责 Google 账号的登录、登出及 OAuth2 权限撤销（`revokeAccess`）。

## 2. 关键技术特性

### 2.1 增量同步 (Incremental Sync)
为了降低流量消耗和 Google Drive API 的调用频率，系统引入了基于修改时间戳的增量同步机制：
1.  **最后同步记录**: 本地 `DataStore` 维护 `last_sync_time`。
2.  **元数据对比**: 在每次同步启动时，先通过一次 API 调用拉取云端文件夹下所有文件的元数据（包含 `id` 和 `modifiedTime`）。
3.  **按需下载**: 仅当云端文件的 `modifiedTime` 晚于本地 `last_sync_time` 时，才会触发下载合并逻辑。
4.  **按需上传**: 仅当本地数据发生实质性变更且云端版本较旧时，才会调用 `updateFile`。

### 2.2 双向同步策略
*   **训练记录 (`YYYY-MM-DD.json`)**: 采用“基于 `remoteId` 的增量合并”策略。下载云端文件后，将缺失的记录插入本地数据库，随后将本地更新合并后上传。
*   **用户偏好 (`user_prefs.json`)**: 采用“最新覆盖”策略。自动同步主题模式、语言设置和用户座右铭。
*   **训练计划 (`plans.json`)**: 支持当前活动计划的双向同步，确保更换手机后计划依然保持一致。

### 2.3 授权鲁棒性 (Robust Authorization)
针对 Google SDK 常见的“权限弹窗不显示”问题，系统实现了以下方案：
*   **强制重置流**: 当检测到已登录但缺少 `DRIVE_FILE` 作用域时，调用 `revokeAccess` 彻底注销并撤销授权。
*   **强制刷新**: 再次登录时，Google 会被迫重新显示账户选择器和完整的权限勾选复选框，避免用户进入“无法勾选权限”的死循环。

## 3. 集成与配置

### 3.1 WorkManager 注入
由于 `SyncWorker` 使用 Hilt 注入，必须按照以下步骤配置：
1.  在 `FitBotApp` 中实现 `Configuration.Provider`。
2.  在 `AndroidManifest.xml` 中禁用默认的 `WorkManagerInitializer`：
    ```xml
    <provider
        android:name="androidx.startup.InitializationProvider"
        android:authorities="${applicationId}.androidx-startup"
        tools:node="merge">
        <meta-data
            android:name="androidx.work.WorkManagerInitializer"
            android:value="androidx.startup"
            tools:node="remove" />
    </provider>
    ```

### 3.2 权限作用域 (OAuth Scopes)
系统严格遵循最小权限原则，仅使用 `https://www.googleapis.com/auth/drive.file`。
*   **限制**: 应用只能读写由其自身创建的文件。
*   **优点**: 保护用户隐私，无需昂贵的 Google Drive 全量访问权限审核。

## 4. 调试与日志
所有同步相关的日志均带有 `FitBotSync` 标签。
```bash
adb logcat -s FitBotSync
```
关键日志节点：
*   `Login successful. Drive permission: true/false`
*   `getOrCreateFolder: Searching/Creating FitBot`
*   `Sync complete successfully.`
