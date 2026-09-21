# Translation Cancellation

## What it does

Allows the user to stop an in-progress translation. Cancels the running coroutine Job and emits a `TranslationFailed` result so the UI reverts from "Stop" back to "Start Translation".

## Key files

- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — `translationJob: Job?` field; `translate()` (ignored if `translationJob` is still active) stores the launched coroutine as `translationJob`; `cancelTranslation()` calls `translationJob?.cancel()` and emits `TranslationFailed(Exception("Translation Cancelled"))`. The run's `finally` block re-reads the modules from disk (`refreshModules()` under `NonCancellable`) so a cancelled run's partial output is visible to the next run
- `src/main/kotlin/home_screen/HomeScreenNew.kt` — shows "Stop Translation" button when `translationResult is UpdateProgress`; onClick calls `viewModel.cancelTranslation()`

## State & data

- **Cancellation resets:** `HomeScreenState.translationResult` to `TranslationFailed` (or `Idle` if the UI resets on failure)
- **Job lifecycle:** `translationJob` is set at the start of `translate()` and nulled or replaced on next call

## Dependencies

- Kotlin coroutines `Job`, `cancel()`

## Consumers

- `src/main/kotlin/home_screen/HomeScreenNew.kt` — "Stop Translation" button
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — owns the Job reference

## Notes

- Cancellation is cooperative: the coroutine is only cancelled at suspension points (e.g. network calls). Partially written XML files for the in-progress language may be left on disk incomplete.
- After cancellation just press Start again: the ViewModel re-reads the module files in `finally`, so only what is still missing is translated (no reload needed). Writes are atomic (temp file + move), so a file is either the old or the new version, never half-written.
