# Progress Reporting

## What it does

Shows the user real-time translation progress on **two** bars: (1) an overall, language-level bar labelled with a **count** (`<n>/<total>`, e.g. `2/10`) of the module+language unit currently being translated — not a percentage — and (2) a per-language string-level bar showing how many of the current language's strings have finished (`<done>/<total>`) as each key completes. Both bars are rendered by the shared `ProgressRow` composable (label left, count right, full-width rounded bar below) so they line up identically.

## Key files

- `src/main/kotlin/data/model/TranslationResult.kt` — `TranslationResult.UpdateProgress(translatingLang: String, moduleName: String, completedUnits: Int, totalUnits: Int, translatedStrings: Int, totalStrings: Int)` — sealed interface variant emitted during translation. `completedUnits`/`totalUnits` drive the overall count+bar; `translatedStrings`/`totalStrings` drive the per-string bar.
- `src/main/kotlin/data/translator/TranslationManager.kt` — a `channelFlow`; emits `UpdateProgress` once per finished key inside `processTranslation` (an extension on `ProducerScope<TranslationResult>`). `completedUnits` = units fully done so far (0-based, stable across a language's string emits), `totalUnits` = modules × translatable languages; `totalStrings` = count of missing keys for the language, `translatedStrings` = `AtomicInteger` incremented as each key finishes.
- `src/main/kotlin/home_screen/HomeScreenNew.kt` — when `translationResult is UpdateProgress`, renders a `Column` of `ProgressRow`s: the overall row shows count `"${completedUnits + 1}/${totalUnits}"` with bar fraction `completedUnits / totalUnits`, and (when `totalStrings > 0`) a per-string row showing `"${translatedStrings}/${totalStrings}"`. `ProgressRow(label, count, fraction)` is the private helper.
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — collects the Flow and calls `_state.update { it.copy(translationResult = it) }`

## State & data

- **State field:** `HomeScreenState.translationResult: TranslationResult`
- **Updated by:** `HomeScreenViewModel.translate()` collecting `TranslationManager.translate()` Flow
- **Range:** `completedUnits` 0..`totalUnits`; `translatedStrings` 0..`totalStrings` (all integer counts, no percentage)

## Dependencies

- `TranslationResult` sealed interface
- Compose `LinearProgressIndicator` (Material)

## Consumers

- `src/main/kotlin/home_screen/HomeScreenNew.kt` — only consumer of `UpdateProgress` for rendering

## Notes

- The overall bar shows a **count, not a percentage** (`<n>/<total>`, e.g. `2/10`). `n` is the 1-based unit currently being translated (`completedUnits + 1`); the bar fill is `completedUnits / totalUnits`. Granularity is per (module, language) unit, total = modules × translatable languages. The count only advances when a language unit *completes* — within a unit it stays put while the per-string bar fills. The label shown during the last unit is `<total>/<total>`; the run then ends with `TranslationCompleted` (never an `UpdateProgress` beyond `totalUnits`).
- The per-string bar is driven by `translatedStrings`/`totalStrings`. `totalStrings` is the number of **missing** keys for that language (a language already fully translated emits `totalStrings = 0`, so the second bar is hidden). In parallel mode (the default) `translatedStrings` does not increase strictly in order, but it always reaches `totalStrings` when the unit finishes.
- `translate()` is a `channelFlow` (not a plain `flow`) specifically so per-key progress can be `send`-ed from the concurrent `async` coroutines inside `processTranslation`. `processTranslation` is therefore an extension on `ProducerScope<TranslationResult>`. The counter is an `AtomicInteger` because parallel keys increment it concurrently.
- `translatingLang` is the language code string (e.g. `"fr"`, `"zh-CN"`), not the display name. `moduleName` is the discovered module (e.g. `"app"`, `"editor"`); it is blank for legacy single-module flows and the label falls back to language only.
