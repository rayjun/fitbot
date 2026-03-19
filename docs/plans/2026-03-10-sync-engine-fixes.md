# Sync Engine Fix: Workout Plan Overwrite Prevention

## 1. Root Cause
The sync engine (both Android and iOS) used a simple "Remote file newer than last sync -> Download" rule. This caused data loss if a device had local changes that hadn't been synced yet, but the remote file had been touched by another device (or a previous sync) more recently than the current device's recorded `last_sync_time`.

## 2. Solution: "Latest Wins" Strategy
Modified the sync logic to compare the actual content timestamps:
- **Android**: Uses the `createdAt` field in the `PlanEntity` stored in Room.
- **iOS**: Introduced `workout_routine_modified` in DataStore to track local modification time.
- **Unified Format**: Both platforms now write `plans.json` as a `List<PlanEntity>` ensuring cross-platform timestamp visibility.
- **Comparison**: The sync engine now fetches the remote file, parses it to extract the remote timestamp, and compares it with the local modification timestamp. The version with the larger timestamp wins.

## 3. Implementation Details
- `DataStoreRepository.kt`: Added `ROUTINE_MODIFIED_KEY` and updated it in `updateRoutineDay`.
- `SyncWorker.kt` (Android): Implemented timestamp-based comparison in `syncPlansLogic`. Added fallback for older iOS formats.
- `IosDriveSyncEngine.kt` (iOS): Implemented timestamp-based comparison and unified the upload format to be Android-compatible.

## 4. Verification Plan
1. **Compilation**: Ensure Android and iOS targets build correctly.
2. **Unit Test**: Test the timestamp comparison logic if possible (mocking Drive).
3. **Manual Test**: 
   - Modify plan on Device A, sync.
   - Modify plan on Device B (different change), sync.
   - Verify Device B uploads its change (since it's newer) rather than being overwritten by Device A's sync.
