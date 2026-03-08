# FitBot 文档目录

本目录包含 FitBot 项目的完整技术文档。

## 文档列表

| 文档 | 内容 |
|------|------|
| [architecture.md](./architecture.md) | 整体架构、技术选型、模块划分 |
| [features.md](./features.md) | 功能模块详解（数据流、UI 交互、业务逻辑） |
| [build.md](./build.md) | 编译与部署指南（Android & iOS） |
| [sync.md](./sync.md) | Google Drive 同步架构与数据格式 |
| [localization.md](./localization.md) | 多语言实现方案 |

## 项目概览

FitBot 是一款基于 **Kotlin Multiplatform + Compose Multiplatform** 的跨平台健身记录 App，同时支持 **Android** 和 **iOS**。

- 动作库浏览（含 GIF 演示）
- 每周训练计划制定
- 训练组数实时记录
- 训练热力图（仿 GitHub 风格）
- Google Drive 云端双向同步
- 中英文双语支持
- 深色 / 浅色 / 跟随系统主题
