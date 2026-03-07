# KMP Migration Phase 2: Serialization and Shared Logic

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace GSON with `kotlinx-serialization` for cross-platform data handling and begin moving UI/Theme logic to `commonMain`.

**Architecture:** Integrate `kotlinx-serialization` plugin. Annotate common models with `@Serializable`. Update Android-specific ViewModels and Workers to use the new serialization. Move Theme and Color definitions to `commonMain`.

**Tech Stack:** Kotlin Multiplatform, kotlinx-serialization, Compose Multiplatform.

---

### Task 1: Setup kotlinx-serialization

**Files:**
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`

**Step 1: Add serialization plugin to root build script**
```kotlin
plugins {
    // ...
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
}
```

**Step 2: Apply plugin and add dependency in app/build.gradle.kts**
Add to `plugins`: `id("org.jetbrains.kotlin.plugin.serialization")`
Add to `commonMain.dependencies`: `implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")`

**Step 3: Commit**
```bash
git add build.gradle.kts app/build.gradle.kts
git commit -m "chore: setup kotlinx-serialization for multiplatform"
```

---

### Task 2: Migrate Common Models to @Serializable

**Files:**
- Modify: `app/src/commonMain/kotlin/com/fitness/model/Exercise.kt`
- Modify: `app/src/commonMain/kotlin/com/fitness/model/RoutineDay.kt`
- Modify: `app/src/commonMain/kotlin/com/fitness/model/TrainingData.kt`

**Step 1: Add @Serializable and @SerialName to models**
Ensure all fields that were previously mapped by GSON (if any specific naming was used) now use `@SerialName`.

**Step 2: Commit**
```bash
git add app/src/commonMain
git commit -m "refactor: migrate common models to kotlinx-serialization"
```

---

### Task 3: Refactor ViewModels and Workers to use kotlinx-serialization

**Files:**
- Modify: `app/src/main/java/com/fitness/ui/plans/PlanViewModel.kt`
- Modify: `app/src/main/java/com/fitness/sync/SyncWorker.kt`

**Step 1: Replace GSON usage with Json.decodeFromString/encodeToString**
In `PlanViewModel`, replace `gson.fromJson` and `gson.toJson` for `RoutineDay` lists.
In `SyncWorker`, update the Drive sync logic to use the new serializer.

**Step 2: Verify Android compilation**
Run `./gradlew :app:compileKotlinAndroid`

**Step 3: Commit**
```bash
git add app/src/main/java
git commit -m "refactor: replace GSON with kotlinx-serialization in ViewModels and Workers"
```

---

### Task 4: Move Theme and Colors to commonMain

**Files:**
- Move: `app/src/main/java/com/fitness/ui/theme/Color.kt` -> `app/src/commonMain/kotlin/com/fitness/ui/theme/Color.kt`
- Move: `app/src/main/java/com/fitness/ui/theme/Theme.kt` -> `app/src/commonMain/kotlin/com/fitness/ui/theme/Theme.kt`
- Move: `app/src/main/java/com/fitness/ui/theme/Type.kt` -> `app/src/commonMain/kotlin/com/fitness/ui/theme/Type.kt`

**Step 1: Relocate theme files and update package/imports**
Ensure no Android-specific graphics or context dependencies exist in the common theme.

**Step 2: Verify Android UI still works**
Since `androidMain` maps to `src/main/java` and `commonMain` is a dependency, it should resolve.

**Step 3: Commit**
```bash
git add app/src/commonMain app/src/main/java
git commit -m "refactor: move theme and color system to commonMain"
```
