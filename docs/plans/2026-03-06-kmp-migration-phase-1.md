# KMP Migration Phase 1: Project Restructuring

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Transform the project into a Kotlin Multiplatform structure supporting Android and iOS.

**Architecture:** Convert the `app` module to a multiplatform module with `commonMain`, `androidMain`, and `iosMain` source sets. UI code will move to `commonMain` using Compose Multiplatform.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Koin (replacing Hilt), Room Multiplatform.

---

### Task 1: Update Root Build Configuration

**Files:**
- Modify: `build.gradle.kts`

**Step 1: Add Multiplatform and Compose plugins**

```kotlin
plugins {
    id("com.android.application") version "8.3.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.multiplatform") version "1.9.22" apply false
    id("org.jetbrains.compose") version "1.6.1" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
}
```

**Step 2: Commit**

```bash
git add build.gradle.kts
git commit -m "chore: add KMP and Compose Multiplatform plugins to root build script"
```

---

### Task 2: Refactor App Module to Multiplatform

**Files:**
- Modify: `app/build.gradle.kts`

**Step 1: Apply Multiplatform and Compose plugins**

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            // Other KMP dependencies
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            // Android specific
        }
        iosMain.dependencies {
        }
    }
}

android {
    // ... existing android config ...
}
```
*(Note: I need to set up version catalog `libs.versions.toml` first or use direct string versions. I'll use direct strings for now to match current project style or initialize a version catalog.)*

---

### Task 3: Initialize Shared Source Sets

**Steps:**
1. Create directories:
   - `app/src/commonMain/kotlin/com/fitness`
   - `app/src/androidMain/kotlin/com/fitness`
   - `app/src/iosMain/kotlin/com/fitness`
2. Move models to `commonMain`:
   - From `app/src/main/java/com/fitness/model/*` to `app/src/commonMain/kotlin/com/fitness/model/`
3. Commit.

---

### Task 4: Move Models to commonMain

**Files:**
- Move: `app/src/main/java/com/fitness/model/*.kt` -> `app/src/commonMain/kotlin/com/fitness/model/`

**Step 1: Relocate model files**
Ensure imports are updated and no Android-specific dependencies exist in models.

**Step 2: Verify compilation**
Run `./gradlew :app:compileKotlinAndroid` (assuming KMP setup is correct).

**Step 3: Commit**
```bash
git add app/src/commonMain
git commit -m "refactor: move models to commonMain for cross-platform sharing"
```
