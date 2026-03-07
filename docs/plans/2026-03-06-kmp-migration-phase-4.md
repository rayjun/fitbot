# KMP Migration Phase 4: Screen Migration and Navigation Alignment

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Migrate core UI screens and navigation definitions to `commonMain`.

**Architecture:** Move `Screen.kt` to shared code. Relocate stateless or simple screens like `ExerciseDetailScreen` and `DayDetailsScreen` to `commonMain`. Refactor them to use shared `ExerciseImage` and `getString` utilities.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform.

---

### Task 1: Move Navigation Definitions (Screen.kt)

**Files:**
- Move: `app/src/main/java/com/fitness/ui/navigation/Screen.kt` -> `app/src/commonMain/kotlin/com/fitness/ui/navigation/Screen.kt`

**Step 1: Relocate and Clean up**
Move the file. Update package name. Ensure imports are correct.
*Note: `Screen` uses `ImageVector`. This is available in Compose Multiplatform.*

**Step 2: Commit**
```bash
git add app/src/commonMain
git commit -m "refactor: move Screen navigation definitions to commonMain"
```

---

### Task 2: Move ExerciseDetailScreen to commonMain

**Files:**
- Move: `app/src/main/java/com/fitness/ui/library/ExerciseDetailScreen.kt` -> `app/src/commonMain/kotlin/com/fitness/ui/library/ExerciseDetailScreen.kt`

**Step 1: Relocate and adapt**
- Use shared `ExerciseImage` (created in Phase 3).
- Use shared `getString` (created in Phase 3) for labels like "Target Muscle" and "Instructions".
- Remove `coil` and `R` dependencies.

**Step 2: Commit**
```bash
git add app/src/commonMain
git commit -m "refactor: move ExerciseDetailScreen to commonMain"
```

---

### Task 3: Move DayDetailsScreen to commonMain

**Files:**
- Move: `app/src/main/java/com/fitness/ui/plans/DayDetailsScreen.kt` -> `app/src/commonMain/kotlin/com/fitness/ui/plans/DayDetailsScreen.kt`

**Step 1: Relocate and adapt**
- This screen uses `PlanViewModel`. For now, we will pass the required data or use an interface.
- Since we want to keep it simple, we will keep the ViewModel in `androidMain` but move the Screen to `commonMain` and pass the ViewModel as a parameter (using the shared ViewModel interface or just the platform ViewModel for now if KMP Lifecycle is not set up).
- *Update*: If the ViewModel is still Android-specific, we'll need to handle it carefully. Let's move the Screen logic and keep the parameter.

**Step 2: Commit**
```bash
git add app/src/commonMain
git commit -m "refactor: move DayDetailsScreen to commonMain"
```

---

### Task 4: Align Resource Loading

**Files:**
- Modify: `app/src/commonMain/kotlin/com/fitness/ui/library/ExerciseImage.kt` (if needed)
- Modify: `app/src/androidMain/kotlin/com/fitness/ui/library/ExerciseImage.android.kt`

**Step 1: Ensure ExerciseImage supports detail view sizing**
- Update the shared `ExerciseImage` component to accept a `Modifier` so we can set the 300.dp height in `ExerciseDetailScreen`.

**Step 2: Commit**
```bash
git add app/src/commonMain app/src/androidMain
git commit -m "refactor: enhance shared ExerciseImage for flexible sizing"
```
