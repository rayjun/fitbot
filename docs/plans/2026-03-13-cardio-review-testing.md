# Cardio Metrics Logic Review & Testing Plan (v0.8.0)

**Goal:** Ensure 100% data integrity and logic correctness for the new distance/duration measurement system.

---

### Phase 1: Logic Review (Static Analysis)
1.  **Repository Integrity**: Verify `RoomWorkoutRepository` correctly maps `distance` and `duration` during both `insertSet` and `updateExerciseSet`.
2.  **Default Value Audit**: Check `ExerciseSet` constructor usages in existing code to ensure backward compatibility.
3.  **UI Type Safety**: Ensure `Double?` and `Int?` conversions from `String` in `RecordSetDialog` handle invalid inputs (NaN, empty strings) gracefully.

---

### Phase 2: Unit Testing (Execution)

**Task 1: AnalyticsEngine Isolation Test**
- **File**: `app/src/commonTest/kotlin/com/fitness/data/AnalyticsEngineTest.kt`
- **Scenario**: Provide 3 sets: Bench Press (100kg x 10), Running (5km / 30min), Squat (80kg x 5).
- **Assertion**: Expected volume for `cat_chest` = 1000.0, `cat_legs` = 400.0, and `cat_cardio` should NOT exist or be 0.0.

**Task 2: Model Mapping Test**
- **File**: `app/src/commonTest/kotlin/com/fitness/model/ExerciseSetMappingTest.kt`
- **Scenario**: Create a `SetEntity` with distance/duration and convert to `ExerciseSet`.
- **Assertion**: Fields must match.

**Task 3: ViewModel Integration Test**
- **File**: `app/src/commonTest/kotlin/com/fitness/ui/workout/WorkoutViewModelTest.kt`
- **Scenario**: Call `addSet` with distance/duration values.
- **Assertion**: Verify the repository receives a set with those specific values.

---

### Phase 3: Verification
1.  Run `./gradlew :app:testDebugUnitTest`.
2.  Perform a final build check.
