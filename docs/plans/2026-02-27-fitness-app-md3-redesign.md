# 设计文档: Fitness App MD3 升级与架构重构

## 1. 视觉与交互 (UI/UX)
- **Material Design 3**: 全面应用 MD3 规范，支持动态配色。
- **首页 Tab**: 
  - 顶部 `ScrollableTabRow` 进行身体部位分组（胸、背、腿等）。
  - 3 列网格动作展示，带圆角 `ElevatedCard`。
- **底部导航**: 首页（Library）、计划（Plans）、个人（Profile）。
- **用户体验**: 完美支持原生左滑返回手势。

## 2. 核心功能模块 (Core Modules)
- **动作库 (Library)**: 增加 `category` 字段，支持分类过滤。
- **训练计划 (Plans)**:
  - 实现“自动归档”机制。
  - 数据库支持 `isCurrent` 标记和 `version` 管理。
- **个人中心 (Profile)**:
  - 实现类似 GitHub 的“健身热力图”。
  - 提供黑暗模式和中英文语言切换。

## 3. 技术架构 (Technical Stack)
- **Navigation**: `androidx.navigation:navigation-compose` 处理路由。
- **UI**: Jetpack Compose (MD3)。
- **Storage**: Room 数据库（需迁移或重建 Schema 以支持计划备份）。
- **Image**: Coil (GIF 支持)。

## 4. 数据库 Schema (Plan Table)
```kotlin
@Entity(tableName = "training_plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val exercisesJson: String, // 选中的动作列表
    val isCurrent: Boolean,    // 是否为当前计划
    val version: Int,
    val createdAt: Long
)
```
