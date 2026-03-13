# AI Fitness Analysis System Design (v0.7.0)

**Goal:** Provide intelligent workout insights and a data-driven personal coach using standard OpenAI-compatible APIs.

---

## 1. Technical Architecture

### 1.1 Data Flow
`Local Database` -> `AnalyticsEngine` -> `Stats Summary (JSON)` -> `AI Prompt Builder` -> `OpenAI Compatible API` -> `User Feedback`.

### 1.2 Component Responsibilities
- **AiRepository**: Handles Ktor networking, standard OpenAI headers, and error handling.
- **AiPromptBuilder**: Logic to convert aggregated training metrics into a concise prompt.
- **AiViewModel**: Manages streaming response state, loading states, and API key management.
- **AiSettings**: User-configurable Base URL and API Key in the Settings screen.

---

## 2. Implementation Phases

### Phase 1: Infrastructure
- Add API configuration to `SettingsScreen` (Base URL, API Key, Model Name).
- Create `AiRepository` with Ktor implementation.

### Phase 2: Context Generation
- Implement `AiPromptBuilder` to summarize:
    - 30-day volume trends per muscle group.
    - Consistency score (frequency).
    - PR Progress (1RM changes).
    - Over-trained vs. under-trained areas.

### Phase 3: UI Integration
- **Entry A**: "AI Insight Card" at the top of `AnalyticsScreen`.
- **Entry B**: "AI Coach" chat interface reachable from `ProfileScreen`.

---

## 3. Data Privacy & Security
- API Keys are stored locally in `DataStore`.
- Only aggregated numbers are sent to the AI; no raw exercise notes or timestamps.
