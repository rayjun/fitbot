# Advanced Anatomy & Analytics Upgrade Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Deeply optimize the anatomy heatmap with front/back views and precise muscle paths, and implement data drill-down logic.

**Architecture:** 
1.  **State Management**: `ProfileViewModel` will hold `selectedMuscleGroup: String?`.
2.  **Visualization**: `AnatomyMap` will use a refined SVG-like `Path` system for both front and back views.
3.  **Interaction**: Users tap a muscle block -> `selectedMuscleGroup` updates -> Charts filter automatically.

**Tech Stack:** Kotlin Multiplatform, Compose Canvas API.

---

### Task 1: ViewModel State & Category Filtering

**Files:**
- Modify: `app/src/commonMain/kotlin/com/fitness/ui/profile/ProfileViewModel.kt`
- Test: `app/src/commonTest/kotlin/com/fitness/ui/profile/ProfileViewModelTest.kt`

**Step 1: Write the failing test**
Update `ProfileViewModelTest` to check if `selectedCategory` correctly filters data.

**Step 2: Implement State**
Add `private val _selectedCategory = MutableStateFlow<String?>(null)`.
Expose `val selectedCategory: StateFlow<String?>`.
Update `muscleVolumeData` to optionally filter based on selection.

**Step 3: Verify and Commit**
```bash
git add .
git commit -m "feat: implement muscle category selection state in ProfileViewModel"
```

---

### Task 2: Refined Silhouette & Front/Back View

**Files:**
- Modify: `app/src/commonMain/kotlin/com/fitness/ui/components/AnatomyMap.kt`

**Step 1: Define Path Regions**
Create constants/objects for `frontPaths` and `backPaths`. Use `Path().apply { ... }` to draw stylized polygons for Chest, Lats, Quads, etc.

**Step 2: Implement Toggle Logic**
Update `AnatomyMap` signature: `fun AnatomyMap(isBackView: Boolean, volumeData: Map<String, Double>, onMuscleClick: (String) -> Unit)`.

**Step 3: Hit Testing**
Implement tap detection using `pointerInput { detectTapGestures { offset -> ... } }`. Map offset to path boundaries.

**Step 4: Commit**
```bash
git add .
git commit -m "feat: implement high-fidelity front/back anatomy silhouettes with hit testing"
```

---

### Task 3: Drill-down UI Integration

**Files:**
- Modify: `app/src/commonMain/kotlin/com/fitness/ui/profile/AnalyticsScreen.kt`

**Step 1: Add View Toggle**
Add a `SegmentedButton` or `Switch` above the map to toggle `isBackView`.

**Step 2: Hook up interaction**
Pass `viewModel.selectedCategory` and the update callback to `AnatomyMap`.

**Step 3: Dynamic Titles**
Change "Total Volume" to "Volume: [Muscle Name]" when a selection is active.

**Step 4: Commit**
```bash
git add .
git commit -m "feat: integrate anatomy view toggle and selection drill-down into AnalyticsScreen"
```
