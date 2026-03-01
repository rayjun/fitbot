# Profile Enhancements and Full Cloud Sync Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Enhance the Profile screen with Google Account info, an editable quote, and a 3-state dark mode, while implementing full-state synchronization (including training plans and user prefs) to Google Drive with visual feedback.

**Architecture:** 
- Use DataStore for the new 3-state theme and user quote. 
- Use Coil for profile image loading.
- Update `SyncWorker` to pull/merge/push `plans.json` and `user_prefs.json` alongside the daily exercise records.
- Introduce a centralized sync state flow in `MainActivity` to drive the Cloud icon's color/animation.

**Tech Stack:** Kotlin, Jetpack Compose, DataStore, Room, WorkManager, Coil, Google Drive API

---

### Task 1: Update Theme Settings and DataStore

**Files:**
- Modify: `app/src/main/java/com/fitness/ui/profile/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/fitness/MainActivity.kt`
- Modify: `app/src/main/java/com/fitness/ui/theme/Theme.kt`

**Step 1: Update SettingsViewModel**

Replace `isDarkMode` with `themeMode` (String: "system", "light", "dark"). Add `userQuote` (String).

```kotlin
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val USER_QUOTE_KEY = stringPreferencesKey("user_quote")

    val themeMode = context.dataStore.data
        .map { preferences -> preferences[THEME_MODE_KEY] ?: "system" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val userQuote = context.dataStore.data
        .map { preferences -> preferences[USER_QUOTE_KEY] ?: "坚持就是胜利" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "坚持就是胜利")

    fun setThemeMode(mode: String) {
        viewModelScope.launch { context.dataStore.edit { it[THEME_MODE_KEY] = mode } }
    }

    fun setUserQuote(quote: String) {
        viewModelScope.launch { context.dataStore.edit { it[USER_QUOTE_KEY] = quote } }
    }
```

**Step 2: Update MainActivity and Theme**

Update `MainActivity.kt` to read `themeMode` and pass the resolved boolean to `FitnessTheme`. 

```kotlin
val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
val isDark = when (themeMode) {
    "dark" -> true
    "light" -> false
    else -> isSystemInDarkTheme()
}
FitnessTheme(darkTheme = isDark) { ... }
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/fitness/ui/profile/SettingsViewModel.kt app/src/main/java/com/fitness/MainActivity.kt
git commit -m "feat: upgrade to 3-state theme and add user quote to datastore"
```

### Task 2: Profile Screen Enhancements

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/fitness/ui/profile/ProfileScreen.kt`

**Step 1: Add Coil Dependency**

Add `implementation("io.coil-kt:coil-compose:2.5.0")` to `build.gradle.kts`.

**Step 2: Rebuild Profile UI**

Update `ProfileScreen`. Pass in the Google `Account?`.
- If Account is null: Show "Login to Google Drive" button.
- If Account is not null: Use `AsyncImage` for `account.photoUrl`, display `account.displayName`.
- Make the quote text clickable to show an `AlertDialog` with an `OutlinedTextField` to edit `userQuote`.
- Change Theme SettingsItem to show a dialog with "System", "Light", "Dark".
- Ensure `WorkoutHeatMap` fills the width by adjusting `LazyRow` arrangement to `Arrangement.End` or using `Modifier.fillMaxWidth()`.

**Step 3: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/com/fitness/ui/profile/ProfileScreen.kt
git commit -m "feat: enhance profile UI with Google info, editable quote, and theme options"
```

### Task 3: Cloud Sync State & Icon

**Files:**
- Modify: `app/src/main/java/com/fitness/MainActivity.kt`
- Modify: `app/src/main/java/com/fitness/ui/library/ExerciseLibraryScreen.kt`

**Step 1: Sync State Tracking**

In `MainActivity`, observe WorkManager for the sync worker.

```kotlin
val workManager = WorkManager.getInstance(context)
val syncWorkInfos by workManager.getWorkInfosForUniqueWorkLiveData("FullSync").observeAsState()
val isSyncing = syncWorkInfos?.any { it.state == WorkInfo.State.RUNNING } == true
```

**Step 2: Update Cloud Icon**

Pass `isSyncing` and `isCloudConnected` to `ExerciseLibraryScreen`. Update the Icon tint:
- `isSyncing`: Yellow/Orange or animated.
- `isCloudConnected`: Green.
- Else: Gray.
Make the icon click trigger a full sync if logged in, or trigger the login flow if not.

**Step 3: Commit**

```bash
git add app/src/main/java/com/fitness/MainActivity.kt app/src/main/java/com/fitness/ui/library/ExerciseLibraryScreen.kt
git commit -m "feat: implement visual cloud sync state feedback"
```

### Task 4: Full Data Synchronization (Plans & Prefs)

**Files:**
- Modify: `app/src/main/java/com/fitness/sync/SyncWorker.kt`

**Step 1: Update SyncWorker to sync Plans and Prefs**

Modify `SyncWorker.doWork()`.
- **Plans**: Fetch all `PlanEntity` from `dao.getAllPlans()`. Download `plans.json` from Drive. Merge (if remote has newer version or ID not in local, insert to local). Then upload merged list to `plans.json`.
- **Prefs**: We need context to access DataStore. Read DataStore, download `user_prefs.json`. Resolve conflicts (e.g., local timestamp vs remote timestamp). For simplicity, since it's personal prefs, we can just push local to remote if local changed, or pull if remote changed. (Assume push overrides for now, or simple merge).

*Note: Since DataStore inside a Worker is tricky to inject cleanly without Hilt/Dagger, we can pass the preferences as JSON strings via WorkManager `inputData` when enqueuing.*

**Step 2: Trigger Full Sync on Login**

In `MainActivity.kt`, when Google Login succeeds, enqueue a `OneTimeWorkRequestBuilder<SyncWorker>()` with `setUniqueWork("FullSync", ExistingWorkPolicy.KEEP)`.

**Step 3: Commit**

```bash
git add app/src/main/java/com/fitness/sync/SyncWorker.kt app/src/main/java/com/fitness/MainActivity.kt
git commit -m "feat: implement full synchronization for plans and user preferences"
```
