# AI Fitness Analysis Implementation Plan

**Goal:** Implement a comprehensive AI analysis feature using standard OpenAI-compatible APIs.

---

### Task 1: API Configuration & Key Storage
**Files:**
- `app/src/commonMain/kotlin/com/fitness/ui/profile/SettingsViewModel.kt`
- `app/src/commonMain/kotlin/com/fitness/ui/profile/SettingsScreen.kt`

1.  Add `aiApiKey`, `aiBaseUrl`, and `aiModel` to `DataStore` preferences.
2.  Expose these in `SettingsViewModel`.
3.  Add text fields in `SettingsScreen` under a new "AI Configuration" category.

---

### Task 2: AI Networking & Prompt Engine
**Files:**
- Create `app/src/commonMain/kotlin/com/fitness/data/AiRepository.kt`
- Create `app/src/commonMain/kotlin/com/fitness/util/AiPromptBuilder.kt`

1.  Implement `AiRepository` using Ktor to call `/v1/chat/completions`.
2.  Define data classes for OpenAI Request/Response.
3.  Implement `AiPromptBuilder.buildSummaryPrompt(sets: List<ExerciseSet>)` to generate a data-heavy context string.

---

### Task 3: Analytics Insight Card (v1)
**Files:**
- `app/src/commonMain/kotlin/com/fitness/ui/profile/AnalyticsScreen.kt`
- `app/src/commonMain/kotlin/com/fitness/ui/profile/ProfileViewModel.kt`

1.  Add `generateInsight()` function to `ProfileViewModel`.
2.  In `AnalyticsScreen`, add a card at the top that displays the AI insight string.
3.  Implement a basic "loading" state for the AI response.

---

### Task 4: AI Coach Chat Interface
**Files:**
- Create `app/src/commonMain/kotlin/com/fitness/ui/profile/AiCoachScreen.kt`
- `app/src/commonMain/kotlin/com/fitness/ui/navigation/Screen.kt`

1.  Add `Screen.AiCoach` to navigation.
2.  Implement a simple chat UI (Messages list + Input field).
3.  Pass the full stats context to every message to ensure AI knows the data.
