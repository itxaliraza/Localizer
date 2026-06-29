# Translation Orchestration

## What it does

Coordinates the end-to-end translation workflow across one or more modules: for each discovered module, iterates over each selected language, detects which string keys are missing from that language's `strings.xml`, fetches translations for missing keys, assembles the updated XML, and writes it back into that module's own res folder. Emits real-time progress (with module name) and a final success/failure result via a Flow.

## Key files

- `src/main/kotlin/data/translator/TranslationManager.kt` — `translate(selectedLanguages, modules, parallelTranslation): Flow<TranslationResult>` (a `channelFlow`) iterates modules then languages; `ProducerScope<TranslationResult>.processTranslation(lang, file, basePairs, outputDir, completedUnits, totalUnits, moduleName)`: suspend extension handling one language for one module, emitting per-string progress as each key finishes; `getTranslation(text, langCode)`: delegates to repo
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — `translate()`: launches IO coroutine, collects the Flow, updates `state.translationResult`; `cancelTranslation()`: cancels the job
- `src/main/kotlin/data/model/TranslationResult.kt` — sealed interface: `Idle`, `UpdateProgress(translatingLang: String, moduleName: String, completedUnits: Int, totalUnits: Int, translatedStrings: Int, totalStrings: Int)`, `TranslationCompleted`, `TranslationFailed(exc: Exception)`

## State & data

- **Input:** `List<ModuleExtraction>` cached in ViewModel after file load; `selectedLanguages` from state
- **Output:** translated `strings.xml` files written to `values-<lang>/` directories inside each module's own res folder (`ModuleExtraction.resPath`)
- **State field updated:** `HomeScreenState.translationResult`
- **Progress reporting:** overall progress is a **count** `completedUnits / totalUnits` (`totalUnits` = modules × translatable languages), advancing per completed (module, language) unit — shown in the UI as `<n>/<total>`, not a percentage. Within a unit, per-string progress (`translatedStrings`/`totalStrings`) is emitted as each key finishes (see [progress-reporting.md](progress-reporting.md))

## Dependencies

- `MyTranslatorRepoImpl` (injected via Koin into `TranslationManager`)
- `FilesHelper` (object; called for XML parse and write)
- `LocalizationUtils` (via `MyTranslatorRepoImpl`)

## Consumers

- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — sole consumer of `TranslationManager.translate()`
- `src/main/kotlin/home_screen/HomeScreenNew.kt` — observes `translationResult` from state to render progress bar, completion message, or error text

## Notes

- **Module-by-module:** the outer loop is over `modules`; each module is processed fully (all languages) before the next. `mChangeFileCodes` is reset per module. Each module's base (`en`) key set and output dir are independent.
- Missing-key detection: `englishKeys - languageKeys` (set subtraction). If a language file has all keys, no network calls are made for it.
- Parallel mode (`parallelTranslation = true`, the default): uses `async {} / awaitAll()` per language's missing keys. Sequential mode uses a plain `mapNotNull {}` with suspension. In both modes a per-string `UpdateProgress` is `send`-ed after each key finishes (counter is an `AtomicInteger`).
- **`channelFlow`, not `flow`:** `translate()` is a `channelFlow` so per-key progress can be `send`-ed from the concurrent `async` coroutines in parallel mode (a plain `flow`'s `emit` is not concurrency-safe). `processTranslation` is an extension on `ProducerScope<TranslationResult>` to give it `send`.
- **Per-key failure tolerance:** each missing key is translated via `translateKeyOrNull()`. If all endpoints fail for a key (after retries) it returns `null` and that single key is skipped — the run does NOT abort. Skipped keys stay missing and are retried on the next run. `CancellationException` still propagates and aborts.
- **Concurrency cap:** `requestSemaphore = Semaphore(MAX_CONCURRENT_REQUESTS = 8)` bounds simultaneous in-flight requests (was unbounded `async` fan-out → 429s). Each key is retried up to `MAX_ATTEMPTS = 3` with `RETRY_BACKOFF_MS = 350` linear backoff.
- **Partial write:** even if some keys fail, the successfully translated subset is written. Writing is skipped only when zero keys succeeded for a language.
- Output is produced by `FilesHelper.mergeEntriesIntoXml(file.contents, translatedPairs)` — merges into the EXISTING target file, preserving arrays/plurals/comments/`translatable=false` strings (see [xml-parsing-writing.md](../features/xml-parsing-writing.md)).
- `changeFileCodes` remaps non-canonical folder codes (e.g. `in` → `id`) so the output XML is written to the correctly named folder.
- After that remap, the folder code is passed through `FilesHelper.toAndroidResFolderCode` so region-qualified locales become valid Android qualifiers on write (`pt-BR` → `values-pt-rBR`, `zh-CN` → `values-zh-rCN`). Without this, Android Studio rejects the generated `values-pt-BR`-style folders.
- `TranslationFailed` wraps any caught exception including `CancellationException` (cancelled by user).
