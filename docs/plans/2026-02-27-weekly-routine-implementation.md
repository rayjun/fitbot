# Weekly Routine Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Refactor the existing training plan structure from a flat list of exercises into a structured 7-day (Monday-Sunday) routine with tracking.

**Architecture:** Use `Gson` to serialize a list of 7 `RoutineDay` objects into `PlanEntity.exercisesJson`. Update UI to read this parsed list and display week progress using `java.util.Calendar` to calculate today's progress based on `SetEntity` records.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Gson

---

### Task 1: Update Data Models

**Files:**
- Create: `app/src/main/java/com/fitness/model/RoutineDay.kt`
- Modify: `app/src/main/java/com/fitness/data/local/PlanEntity.kt` (No code change needed, just repurposing `exercisesJson`)

**Step 1: Create the RoutineDay data class**

```kotlin
package com.fitness.model

data class RoutineDay(
    val dayOfWeek: Int, // 1 = Monday, ..., 7 = Sunday
    val isRest: Boolean,
    val exercises: List<String> // List of exercise IDs
)
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/fitness/model/RoutineDay.kt
git commit -m "feat: add RoutineDay data model"
```

### Task 2: Enhance PlanViewModel to Support Routines

**Files:**
- Modify: `app/src/main/java/com/fitness/ui/plans/PlanViewModel.kt`

**Step 1: Add Gson parsing and routine generation**

Modify `PlanViewModel.kt` to include `Gson`. Add a method `generateDefaultRoutine()` that creates a 7-day default plan. Update `updatePlan` to accept `List<RoutineDay>` and serialize it. Add a `StateFlow` to expose the parsed `List<RoutineDay>` of the `currentPlan`.

```kotlin
    private val gson = com.google.gson.Gson()

    // Add state flow for parsed routine
    val currentRoutine: StateFlow<List<RoutineDay>> = _currentPlan.map { plan ->
        if (plan == null) emptyList()
        else try {
            val type = object : com.google.gson.reflect.TypeToken<List<RoutineDay>>() {}.type
            gson.fromJson<List<RoutineDay>>(plan.exercisesJson, type)
        } catch (e: Exception) {
            emptyList()
        }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    // Update updatePlan signature
    fun updatePlan(name: String, routine: List<RoutineDay>) {
        val version = (_currentPlan.value?.version ?: 0) + 1
        val newPlan = PlanEntity(
            name = name,
            exercisesJson = gson.toJson(routine),
            isCurrent = true,
            version = version,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            dao.updatePlan(newPlan)
            refresh()
        }
    }
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/fitness/ui/plans/PlanViewModel.kt
git commit -m "feat: update PlanViewModel to support 7-day routines"
```

### Task 3: Weekly Progress Tracking in WorkoutViewModel

**Files:**
- Modify: `app/src/main/java/com/fitness/ui/workout/WorkoutViewModel.kt`

**Step 1: Add a method to check if a specific day has completed exercises**

```kotlin
    // Query the database to see if any sets exist for a specific date (YYYY-MM-DD)
    suspend fun hasCompletedExercisesOnDate(dateStr: String): Boolean {
        return dao.getSetsByDate(dateStr).isNotEmpty()
    }
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/fitness/ui/workout/WorkoutViewModel.kt
git commit -m "feat: add method to check daily completion status"
```

### Task 4: Rebuild PlansScreen UI

**Files:**
- Modify: `app/src/main/java/com/fitness/ui/plans/PlansScreen.kt`

**Step 1: Add Weekly Progress Bar**

Implement a `WeeklyProgressBar` composable using `Calendar` to determine today's `dayOfWeek`. Display 7 circles. Gray for future, Primary for completed training, Green outline for rest.

**Step 2: Add Today's Task Card**

Show today's `RoutineDay`. If `isRest`, show relax message. Else, show exercise list and a "Start Today's Training" button that calls `onStartPlan(dayOfWeek)`.

**Step 3: Update `updatePlan` call**

Instead of hardcoded exercises, pass a 7-day `List<RoutineDay>`.

**Step 4: Commit**

```bash
git add app/src/main/java/com/fitness/ui/plans/PlansScreen.kt
git commit -m "feat: redesign PlansScreen with weekly progress and today's task"
```

### Task 5: Update PlanSessionScreen & MainActivity Navigation

**Files:**
- Modify: `app/src/main/java/com/fitness/ui/plans/PlanSessionScreen.kt`
- Modify: `app/src/main/java/com/fitness/MainActivity.kt`
- Modify: `app/src/main/java/com/fitness/ui/navigation/Screen.kt`

**Step 1: Update Navigation Arguments**

Change `planId` to `dayOfWeek` in `Screen.kt` and `MainActivity.kt`.

**Step 2: Filter Exercises in PlanSessionScreen**

In `PlanSessionScreen`, use `currentRoutine` from `PlanViewModel` and find the `RoutineDay` matching `dayOfWeek`. Extract its `exercises` list to display, instead of parsing `exercisesJson` directly.

**Step 3: Commit**

```bash
git add app/src/main/java/com/fitness/ui/plans/PlanSessionScreen.kt app/src/main/java/com/fitness/MainActivity.kt app/src/main/java/com/fitness/ui/navigation/Screen.kt
git commit -m "feat: restrict PlanSessionScreen to today's exercises"
```
