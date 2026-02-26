# 设计文档: Android 健身记录应用 (Fitness Tracker)

## 项目背景
开发一款轻量级的 Android 健身 App，主要用于展示健身动作演示（动图）、记录训练过程，并实时将数据同步到 Google Drive 以便 Gemini 后续分析。

## 1. 系统架构 (Architecture)
采用 **MVVM (Model-View-ViewModel)** 架构搭配 **Repository 模式**。
- **UI 层**: 使用 **Jetpack Compose** 构建，支持流畅的动图展示和数据录入。
- **本地存储 (Room)**: 所有训练数据首先存储在本地 SQLite 数据库中，确保离线可用。
- **云端同步 (Google Drive API)**: 实时将本地数据增量合并到 Google Drive 的 JSON 文件中。
- **后台任务 (WorkManager)**: 确保即使 App 退出，数据同步也能完成。

## 2. 核心功能模块 (Core Modules)
- **动作库 (ExerciseLibrary)**: 
  - 内置 GIF 动图资源。
  - 文字描述动作要领及目标肌群。
- **计划与记录 (TrainingTracker)**:
  - 预设训练计划（如“胸部训练”、“背部训练”）。
  - 一个计划包含多个动作，一个动作包含多组。
  - 支持手动输入重量（kg）和次数（reps）。
- **云同步引擎 (CloudSyncEngine)**:
  - 处理 Google 账号登录授权。
  - 按天（`YYYY-MM-DD.json`）管理 Google Drive 上的数据文件。

## 3. 数据结构 (Data Structure)
Google Drive 上的文件路径示例: `/MyFitnessData/2026-02-26.json`
格式示例:
```json
{
  "date": "2026-02-26",
  "sessions": [
    {
      "sessionId": "胸部训练_0900", 
      "startTime": "09:00",
      "endTime": "10:00",
      "exercises": [
        {
          "name": "杠铃卧推",
          "sets": [
            { "reps": 12, "weight": 60, "time": "09:05" },
            { "reps": 10, "weight": 65, "time": "09:12" }
          ]
        },
        {
          "name": "哑铃飞鸟",
          "sets": [
            { "reps": 15, "weight": 10, "time": "09:25" },
            { "reps": 12, "weight": 10, "time": "09:35" }
          ]
        }
      ]
    }
  ]
}
```

## 4. 后续步骤
1. 集成 Google Drive REST API SDK。
2. 构建本地数据库 Schema。
3. 实现 Compose UI 界面（动作库查看、训练引导、数据录入）。
4. 编写同步逻辑。
