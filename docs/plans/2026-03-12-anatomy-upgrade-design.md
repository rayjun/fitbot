# Advanced Anatomy & Analytics Upgrade Design

**Goal:** Transform the Data Analytics screen into an interactive, full-body physiological dashboard with front/back views and deep data drill-down.

**Architecture:**
1.  **UI Layout**: Refactor `AnalyticsScreen` to center on a large, toggleable `AnatomyMap` (Front/Back).
2.  **Stateful Filtering**: Introduce `selectedMuscleCategory` in `ProfileViewModel`. When a muscle is clicked on the map, the Volume and Strength charts filter to that category.
3.  **AnatomyMap 2.0**:
    *   Pure Compose `Canvas`.
    *   Precise `Path` coordinates for major muscle groups.
    *   Front View groups: Chest, Core, Quads, Front Delts, Biceps.
    *   Back View groups: Lats, Lower Back, Glutes, Rear Delts, Triceps, Calves.
4.  **Advanced Analytics**:
    *   Calculate 1RM trends per exercise.
    *   Aggregate volume trends per muscle group over time (weekly buckets).

**Tech Stack:** Kotlin Multiplatform, Compose Canvas API, kotlinx-datetime.

---

### Implementation Phases

#### Phase 1: Enhanced Data Aggregation
-   Extend `AnalyticsEngine` to support time-series data (e.g., `getVolumeTrend(muscleGroup)`).
-   Update `ProfileViewModel` to manage `selectedMuscleGroup` and filtered data flows.

#### Phase 2: Silhouette 2.0 (Front & Back)
-   Refactor `AnatomyMap.kt` to take an `isBackView` parameter.
-   Draw detailed `Path` shapes for all listed muscle groups.
-   Implement hit-detection (tap handling) on the Canvas to update state.

#### Phase 3: Dynamic Chart Integration
-   Add a `1RM History Chart` that appears when a specific muscle is selected.
-   Update `VolumeBarChart` to react to selection (highlight or filter).

---
*Date: 2026-03-12*
