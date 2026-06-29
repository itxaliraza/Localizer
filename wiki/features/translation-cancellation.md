# Translation Cancellation

## What it does

Allows the user to stop an in-progress translation. Cancels the running coroutine Job and emits a `TranslationFailed` result so the UI reverts from "Stop" back to "Start Translation".

## Key files

- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — `translationJob: Job?` field; `translate()` stores the launched coroutine as `translationJob`; `cancelTranslation()` calls `translationJob?.cancel()` and emits `TranslationFailed(Exception("Translation Cancelled"))`
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
- After cancellation, the user must reload the folder or re-run translation to get complete output.
