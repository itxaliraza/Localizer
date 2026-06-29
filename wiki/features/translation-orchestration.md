# Translation Orchestration

## What it does

Coordinates the end-to-end translation workflow: iterates over each selected language, detects which string keys are missing from that language's `strings.xml`, fetches translations for missing keys, assembles the updated XML, and writes it to disk. Emits real-time progress and a final success/failure result via a Flow.

## Key files

- `src/main/kotlin/data/translator/TranslationManager.kt` — `translate(selectedLanguages, extractedFiles, changeFileCodes, outputDir, parallelTranslation): Flow<TranslationResult>`; `processTranslation(lang, ...)`: suspend fun handling one language; `getTranslation(text, langCode)`: delegates to repo
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — `translate()`: launches IO coroutine, collects the Flow, updates `state.translationResult`; `cancelTranslation()`: cancels the job
- `src/main/kotlin/data/model/TranslationResult.kt` — sealed interface: `Idle`, `UpdateProgress(progress: Int, translatingLang: String)`, `TranslationCompleted`, `TranslationFailed(exc: Exception)`

## State & data

- **Input:** `ExtractionResult` cached in ViewModel after file load; `selectedLanguages` from state
- **Output:** translated `strings.xml` files written to `values-<lang>/` directories inside the source folder
- **State field updated:** `HomeScreenState.translationResult`
- **Progress formula:** `(languageIndex / totalLanguages) * 100`

## Dependencies

- `MyTranslatorRepoImpl` (injected via Koin into `TranslationManager`)
- `FilesHelper` (object; called for XML parse and write)
- `LocalizationUtils` (via `MyTranslatorRepoImpl`)

## Consumers

- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — sole consumer of `TranslationManager.translate()`
- `src/main/kotlin/home_screen/HomeScreenNew.kt` — observes `translationResult` from state to render progress bar, completion message, or error text

## Notes

- Missing-key detection: `englishKeys - languageKeys` (set subtraction). If a language file has all keys, no network calls are made for it.
- Parallel mode (`parallelTranslation = true`, the default): uses `async {} / awaitAll()` per language's missing keys. Sequential mode uses a plain `mapNotNull {}` with suspension.
- **Per-key failure tolerance:** each missing key is translated via `translateKeyOrNull()`. If all endpoints fail for a key (after retries) it returns `null` and that single key is skipped — the run does NOT abort. Skipped keys stay missing and are retried on the next run. `CancellationException` still propagates and aborts.
- **Concurrency cap:** `requestSemaphore = Semaphore(MAX_CONCURRENT_REQUESTS = 8)` bounds simultaneous in-flight requests (was unbounded `async` fan-out → 429s). Each key is retried up to `MAX_ATTEMPTS = 3` with `RETRY_BACKOFF_MS = 350` linear backoff.
- **Partial write:** even if some keys fail, the successfully translated subset is written. Writing is skipped only when zero keys succeeded for a language.
- Output is produced by `FilesHelper.mergeEntriesIntoXml(file.contents, translatedPairs)` — merges into the EXISTING target file, preserving arrays/plurals/comments/`translatable=false` strings (see [xml-parsing-writing.md](../features/xml-parsing-writing.md)).
- `changeFileCodes` remaps non-canonical folder codes (e.g. `in` → `id`) so the output XML is written to the correctly named folder.
- `TranslationFailed` wraps any caught exception including `CancellationException` (cancelled by user).
