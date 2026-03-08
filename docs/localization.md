# 多语言实现文档

## 支持的语言

| 代码 | 语言 | 备注 |
|------|------|------|
| `en` | English | 默认语言 |
| `zh` | 简体中文 | 中文界面 |

---

## 实现方案

FitBot **没有使用** Compose Resources 的 `stringResource()` 系统进行运行时语言切换，
原因是该 API 在 iOS 上通过 `NSLocale.preferredLanguages` 解析，
**无法响应应用内的语言切换**，必须重启才能生效。

取而代之的方案：**自定义字符串表 + CompositionLocal**

---

## 核心文件

`app/src/commonMain/kotlin/com/fitness/util/ResourceUtils.kt`

### 字符串表

文件内维护两个 `Map<String, String>`：

```kotlin
private val stringsEn: Map<String, String> = mapOf(
    "app_name"         to "FitBot",
    "nav_library"      to "Library",
    "nav_plans"        to "Plans",
    "nav_profile"      to "Profile",
    "add_record"       to "Add Record",
    "edit_record"      to "Edit Record",
    "delete"           to "Delete",
    // ... 共 100+ 条
)

private val stringsZh: Map<String, String> = mapOf(
    "app_name"         to "FitBot",
    "nav_library"      to "动作库",
    "nav_plans"        to "训练计划",
    "nav_profile"      to "个人中心",
    "add_record"       to "添加记录",
    "edit_record"      to "编辑记录",
    "delete"           to "删除",
    // ... 共 100+ 条
)
```

### CompositionLocal

```kotlin
// 全局语言 CompositionLocal，默认 "en"
val LocalAppLanguage = compositionLocalOf { "en" }
```

### getString 函数

```kotlin
@Composable
fun getString(key: String): String {
    val language = LocalAppLanguage.current
    val table = if (language == "zh") stringsZh else stringsEn
    return table[key] ?: key   // key 不存在时返回 key 本身，便于排查缺失项
}
```

---

## 语言设置流程

```
用户在设置页选择语言
    → SettingsViewModel.setLanguage("zh")
    → repository.setLanguage("zh")
    → DataStore.edit { it[LANGUAGE_KEY] = "zh" }
    → SettingsViewModel.language StateFlow 更新
    → MainViewController/MainActivity 中：
        val language by settingsViewModel.language.collectAsState()
    → CompositionLocalProvider(LocalAppLanguage provides language) { ... }
    → 整棵 Compose 树重组，getString() 返回新语言字符串
```

**效果：** 语言切换即时生效，无需重启 App。

---

## 字符串 Key 命名规范

| 前缀 | 用途 | 示例 |
|------|------|------|
| `nav_` | 底部导航栏标签 | `nav_library`、`nav_plans` |
| `ex_` + `_name` | 动作名称 | `ex_benchpress_name` |
| `ex_` + `_desc` | 动作描述 | `ex_benchpress_desc` |
| `cat_` | 动作分类 | `cat_chest`、`cat_back` |
| `muscle_` | 目标肌群 | `muscle_chest`、`muscle_lats` |
| `settings_` | 设置相关 | `settings_theme`、`settings_language` |
| `day_` | 星期名称 | `day_mon`、`day_tue` |
| `cloud_` | 云同步相关 | `cloud_connect`、`cloud_connected` |
| `theme_` | 主题选项 | `theme_system`、`theme_light`、`theme_dark` |
| `lang_` | 语言选项名 | `lang_zh`、`lang_en` |

---

## 添加新字符串

1. 打开 `app/src/commonMain/kotlin/com/fitness/util/ResourceUtils.kt`
2. 在 `stringsEn` 中添加英文条目
3. 在 `stringsZh` 中添加对应中文条目
4. 在界面中使用 `getString("your_key")`

示例：
```kotlin
// stringsEn
"new_feature" to "New Feature",

// stringsZh
"new_feature" to "新功能",

// 界面使用
Text(getString("new_feature"))
```

> **注意**：字符串 key 必须两个 Map 中都存在，否则在未匹配的语言下会显示 key 字符串本身（用于调试）。

---

## Android 端的兼容说明

Android 端除了使用上述 `getString()` 函数，还保留了 `res/values/strings.xml` 和 `res/values-zh/strings.xml`，供少数 Android 系统级字符串（如通知、AppName 等）使用，但界面内字符串统一走 `getString()` 方案，确保与 iOS 行为一致。
