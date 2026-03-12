# Analytics & UI Refactor Review and Testing Plan

**Goal:** Ensure 100% logic coverage for the new time-range filtering and category drill-down features in the Profile/Analytics module.

---

### Phase 1: Logic Review (Static Analysis)
1.  **File Cleanup Check**: Verify `AnatomyMap.kt` usage is completely removed from `AnalyticsScreen.kt`.
2.  **Resource Audit**: Check `ResourceUtils.kt` for any unused or duplicated keys after multiple iterations.
3.  **Navigation Check**: Ensure `onTimeRangeClick` and `onCategoryClick` are correctly wired up in `MainActivity.kt` and `MainViewController.kt`.

---

### Phase 2: Unit Testing (TDD/Expansion)
**Target File**: `app/src/commonTest/kotlin/com/fitness/ui/profile/ProfileViewModelTest.kt`

1.  **Task 1: Test TimeRange Filtering**
    - Add mock sets with dates: Today, 10 days ago, 400 days ago.
    - Assert `WEEK` range only sees Today.
    - Assert `MONTH` range sees Today + 10 days ago.
    - Assert `YEAR` range sees all except 400 days ago.

2.  **Task 2: Test Composite Filtering**
    - Select `cat_chest` AND `TimeRange.WEEK`.
    - Verify only chest workouts from the last 7 days are included.

3.  **Task 3: i18n Helper Test**
    - Create `app/src/commonTest/kotlin/com/fitness/util/ResourceUtilsTest.kt`.
    - Verify "zh" and "en" returning correct strings for a sample key.

---

### Phase 3: Verification
1.  Run `./gradlew :app:testDebugUnitTest`.
2.  Perform a final build to ensure no regression in UI.
