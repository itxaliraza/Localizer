# Progress Reporting

## What it does

Shows the user real-time translation progress as a percentage and the language currently being translated. The UI renders a `LinearProgressIndicator` and a text label that update on every language completion.

## Key files

- `src/main/kotlin/data/model/TranslationResult.kt` — `TranslationResult.UpdateProgress(progress: Int, translatingLang: String)` — sealed interface variant emitted during translation
- `src/main/kotlin/data/translator/TranslationManager.kt` — emits `UpdateProgress` after each language completes; formula: `(index / total) * 100`
- `src/main/kotlin/home_screen/HomeScreenNew.kt` — renders `LinearProgressIndicator(progress / 100f)` and `Text(translatingLang)` when `translationResult is UpdateProgress`
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — collects the Flow and calls `_state.update { it.copy(translationResult = it) }`

## State & data

- **State field:** `HomeScreenState.translationResult: TranslationResult`
- **Updated by:** `HomeScreenViewModel.translate()` collecting `TranslationManager.translate()` Flow
- **Range:** progress 0–100 (integer percent)

## Dependencies

- `TranslationResult` sealed interface
- Compose `LinearProgressIndicator` (Material)

## Consumers

- `src/main/kotlin/home_screen/HomeScreenNew.kt` — only consumer of `UpdateProgress` for rendering

## Notes

- Progress granularity is per-language (not per-string); a project with 50 languages will see 2% increments per language.
- `translatingLang` is the language code string (e.g. `"fr"`, `"zh-CN"`), not the display name. The UI shows this raw code.
