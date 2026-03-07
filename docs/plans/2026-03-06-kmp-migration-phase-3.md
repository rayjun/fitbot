# KMP Migration Phase 3: Shared UI and Business Logic

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Migrate Exercise Library logic and UI components to `commonMain` and prepare for iOS app integration.

**Architecture:** Move `ExerciseProvider` and static assets to shared sources. Relocate `ExerciseLibraryScreen` and related UI components to `commonMain` using Compose Multiplatform. Set up initial `iosMain` entry point.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Coil (KMP version or placeholder).

---

### Task 1: Migrate Business Logic (ExerciseProvider)

**Files:**
- Move: `app/src/main/java/com/fitness/data/ExerciseProvider.kt` -> `app/src/commonMain/kotlin/com/fitness/data/ExerciseProvider.kt`

**Step 1: Relocate and Clean up**
Move the file and remove Android `R.string` dependencies. Use a simple string or a shared resource system (like `moko-resources` or Compose Resources).
*Decision: Since we don't have a shared resource system yet, we will temporarily use the raw string IDs or a placeholder mechanism to keep it common-main compatible.*

**Step 2: Commit**
```bash
git add app/src/commonMain
git commit -m "refactor: move ExerciseProvider to commonMain"
```

---

### Task 2: Migrate Static Assets to Common

**Steps:**
1. Move `app/src/main/assets/exercises/` to `app/src/commonMain/composeResources/files/exercises/` (Standard Compose Multiplatform resource path).
2. Update `ExerciseProvider` to point to the new paths if necessary.
3. Commit.

---

### Task 3: Migrate ExerciseLibraryScreen to commonMain

**Files:**
- Move: `app/src/main/java/com/fitness/ui/library/ExerciseLibraryScreen.kt` -> `app/src/commonMain/kotlin/com/fitness/ui/library/ExerciseLibraryScreen.kt`

**Step 1: Relocate and adapt to CMP**
- Replace `coil-compose` (Android) with a KMP-compatible image loader (e.g., `Coil 3` or `Compose Resources`).
- Remove Android-specific `R` references.
- Use `expect/actual` or shared resources for strings.

**Step 2: Commit**
```bash
git add app/src/commonMain
git commit -m "refactor: move ExerciseLibraryScreen to commonMain"
```

---

### Task 4: Setup iOS Entry Point

**Files:**
- Create: `app/src/iosMain/kotlin/com/fitness/MainViewController.kt`

**Step 1: Implement ComposeUIViewController**
```kotlin
package com.fitness

import androidx.compose.ui.window.ComposeUIViewController
import com.fitness.ui.theme.FitnessTheme

fun MainViewController() = ComposeUIViewController {
    FitnessTheme {
        // Entry point for iOS
    }
}
```

**Step 2: Commit**
```bash
git add app/src/iosMain
git commit -m "feat: setup iOS MainViewController entry point"
```
