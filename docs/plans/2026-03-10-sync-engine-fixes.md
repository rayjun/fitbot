# Sync Engine Fixes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix critical data loss issues in the Drive sync engine by introducing Fetch-Merge-Upload, tombstone soft-deletes, and optimizing iOS DataStore performance.

**Architecture:** 
1. `ExerciseSet` model will gain an `isDeleted` flag for tombstones.
2. The sync logic (both Android and iOS) will implement a "Fetch-Merge-Upload" loop when updating `.json` files instead of blindly overwriting.
3. iOS `IosDriveSyncEngine` will separate local modification state from the main data payload to prevent reading massive JSON strings just to check dirty flags.

**Tech Stack:** Kotlin Multiplatform, Coroutines, kotlinx-serialization

---

### Task 1: Add Tombstone Support to Data Models

**Files:**
- Modify: `app/src/commonMain/kotlin/com/fitness/model/ExerciseSet.kt`
- Modify: `app/src/androidMain/kotlin/com/fitness/data/local/Entities.kt` (if applicable for `SetEntity`)
- Modify: `app/src/commonMain/kotlin/com/fitness/model/TrainingData.kt` (`SetRecord`)

**Step 1: Write the failing test**
Create/Modify: `app/src/commonTest/kotlin/com/fitness/model/ExerciseSetTest.kt`
```kotlin
package com.fitness.model

import kotlin.test.Test
import kotlin.test.assertTrue

class ExerciseSetTest {
    @Test
    fun testSetRecordHasIsDeletedDefaultFalse() {
        val record = SetRecord(reps = 10, weight = 50.0, time = "10:00", remoteId = "123")
        assertTrue(!record.isDeleted)
    }
}
```

**Step 2: Run test to verify it fails**
Run: `./gradlew :app:testDebugUnitTest --tests "com.fitness.model.ExerciseSetTest"`
Expected: FAIL (Unresolved reference: isDeleted)

**Step 3: Write minimal implementation**
Update `SetRecord`, `ExerciseSet`, and `SetEntity` to include `val isDeleted: Boolean = false`.
Update mappers in Android (`toModel()`, `toEntity()`) and iOS (`buildTrainingDay`, `mergeRemoteSetsIntoLocal`) to pass `isDeleted`.

**Step 4: Run test to verify it passes**
Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

**Step 5: Commit**
```bash
git add .
git commit -m "feat: add isDeleted tombstone flag to ExerciseSet and SetRecord models"
```

---

### Task 2: Implement Soft Delete in Repositories

**Files:**
- Modify: `app/src/commonMain/kotlin/com/fitness/data/WorkoutRepository.kt`
- Modify: `app/src/androidMain/kotlin/com/fitness/data/RoomWorkoutRepository.kt`
- Modify: `app/src/commonMain/kotlin/com/fitness/data/DataStoreRepository.kt`
- Modify: `app/src/commonTest/kotlin/com/fitness/data/FakeWorkoutRepository.kt`

**Step 1: Write the failing test**
Update `app/src/commonTest/kotlin/com/fitness/ui/workout/WorkoutViewModelTest.kt` to check soft delete.
```kotlin
    @Test
    fun testDeleteSetIsSoftDelete() = runTest {
        backgroundScope.launch { viewModel.setsToday.collect() }
        val date = "2024-03-09"
        viewModel.setDate(date)
        
        val set = ExerciseSet(id = 1L, date = date, sessionId = "s1", exerciseName = "benchpress", reps = 10, weight = 60.0, timestamp = 0L, timeStr = "12:00")
        repository.addExerciseSet(set)
        viewModel.deleteSet(1L, date)
        advanceUntilIdle()
        
        // Active sets should be empty
        assertEquals(0, viewModel.setsToday.value.size)
        // But repository should still have it as deleted
        val allSets = repository.getAllSets().first()
        assertTrue(allSets.any { it.id == 1L && it.isDeleted })
    }
```

**Step 2: Run test to verify it fails**
Run: `./gradlew :app:testDebugUnitTest --tests "com.fitness.ui.workout.WorkoutViewModelTest.testDeleteSetIsSoftDelete"`
Expected: FAIL

**Step 3: Write minimal implementation**
Change `deleteExerciseSet` implementations to update `isDeleted = true` instead of physical deletion.
Change `getSetsByDate` and `getAllSets` to filter out `isDeleted == true` (for UI display), OR expose a new method for sync to get *everything* including tombstones.

**Step 4: Run test to verify it passes**
Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

**Step 5: Commit**
```bash
git add .
git commit -m "feat: implement soft delete for workout records"
```

---

### Task 3: Implement Fetch-Merge-Upload in iOS Sync Engine

**Files:**
- Modify: `app/src/iosMain/kotlin/com/fitness/sync/IosDriveSyncEngine.kt`

**Step 1: Write the failing test**
Since Drive sync is hard to mock in KMP commonTest without DI interfaces, we will rely on rigorous unit tests for the *merge function* internally.

Extract the merge logic into a pure function `fun mergeTrainingDays(local: TrainingDay, remote: TrainingDay): TrainingDay`.
Test this in `app/src/commonTest/kotlin/com/fitness/sync/MergeLogicTest.kt`.

**Step 2: Run test to verify it fails**
Write test where local has new set, remote has new set -> merged has both.
Run: `./gradlew :app:testDebugUnitTest`

**Step 3: Write minimal implementation**
Implement `mergeTrainingDays` in `TrainingData.kt` or `IosDriveSyncEngine`.
Update `syncSetsLogic` upload pass:
Instead of `drive.updateFile(remoteFile.id, localJsonStr)`, do:
```kotlin
val remoteJson = drive.downloadFile(remoteFile.id)
val remoteDay = json.decodeFromString<TrainingDay>(remoteJson)
val mergedDay = mergeTrainingDays(localDay, remoteDay)
drive.updateFile(remoteFile.id, json.encodeToString(mergedDay))
```

**Step 4: Run test to verify it passes**
Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

**Step 5: Commit**
```bash
git add .
git commit -m "fix(ios): implement Fetch-Merge-Upload in Drive sync to prevent data loss"
```

---

### Task 4: Implement Fetch-Merge-Upload in Android Sync Engine

**Files:**
- Modify: `app/src/main/java/com/fitness/sync/SyncWorker.kt`

**Step 1: Apply identical merge logic to Android**
Update `syncSetsLogic` upload pass in `SyncWorker.kt` to use the same `mergeTrainingDays` logic.

**Step 2: Compile & verify**
Run: `./gradlew :app:compileDebugKotlinAndroid`

**Step 3: Commit**
```bash
git add .
git commit -m "fix(android): implement Fetch-Merge-Upload in Drive sync to prevent data loss"
```

---

### Task 5: Fix iOS DataStore Performance Bottleneck

**Files:**
- Modify: `app/src/commonMain/kotlin/com/fitness/data/DataStoreRepository.kt`
- Modify: `app/src/iosMain/kotlin/com/fitness/sync/IosDriveSyncEngine.kt`

**Step 1: Redesign Dirty Tracking**
Instead of `local_modified_YYYY-MM-DD` living in the same DataStore preferences, create a lightweight `Set<String>` of dirty dates stored in a single JSON string key `dirty_dates`.
When `add/update/deleteExerciseSet` happens, append to this set.

**Step 2: Update SyncEngine**
Read the `dirty_dates` set directly instead of iterating `allPrefs.asMap()`. After successful sync, clear the `dirty_dates` set.

**Step 3: Run existing tests**
Run: `./gradlew :app:testDebugUnitTest`
Ensure no regressions in existing data access.

**Step 4: Commit**
```bash
git add .
git commit -m "perf(ios): separate dirty state tracking from main DataStore to prevent OOM"
```
