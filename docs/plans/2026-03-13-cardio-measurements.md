# Feature Plan: Support Distance/Duration for Cardio Exercises (v0.8.0)

**Goal:** Modify the data model and UI to properly support measuring cardiovascular exercises (Running, Walking, Cycling) using Distance (km) and Duration (minutes) instead of Weight and Reps.

## 1. Data Layer Refactoring
**Target:** `ExerciseSet.kt` & `SetEntity.kt`
- Add `distance: Double? = null`
- Add `duration: Int? = null`
- Update mapping functions `toModel()` and `RoomWorkoutRepository.kt` database insertions.

## 2. Database Migration
**Target:** `AppDatabase.kt`
- Bump database version from 5 to 6.
- Write `MIGRATION_5_6` to safely add `distance` (REAL) and `duration` (INTEGER) columns to the `exercise_sets` table.

## 3. UI Refactoring (Recording Screen)
**Target:** `WorkoutScreen.kt` & `WorkoutViewModel.kt`
- Introduce a boolean flag `isCardio` derived from the selected exercise's category (`cat_cardio`).
- **State:** Add `distanceInput` and `durationInput` to the ViewModel.
- **UI:** If `isCardio` is true:
  - Change input layout: replace Weight/Reps with Distance (km) / Duration (min).
  - Modify the "History List" to display "30 min | 5.0 km" instead of "100 kg x 10 reps".

## 4. Analytics Engine Adjustment
**Target:** `AnalyticsEngine.kt`
- When calculating `VolumePerMuscleGroup`, explicitly ignore sets where the exercise belongs to the `cat_cardio` category, so distance/duration numbers don't skew strength volume metrics.

## 5. Testing
- Verify Room database migration doesn't crash on launch.
- Verify UI correctly toggles between input modes based on the exercise.
