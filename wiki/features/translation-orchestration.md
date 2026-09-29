# Translation Orchestration

## What it does

Coordinates the end-to-end translation workflow across one or more modules: for each discovered module, iterates over each selected language, detects which string keys are missing from that language's `strings.xml`, fetches translations for missing keys, assembles the updated XML, and writes it back into that module's own res folder. Emits real-time progress (with module name) and a final result via a Flow. The final `TranslationCompleted` carries a **summary** (translated/failed counts + per-unit issues) so failures are never reported as success.

## Key files

- `src/main/kotlin/data/translator/TranslationManager.kt` — `TranslationManager(translatorRepo: TranslationRepository, retryBackoffMs = RETRY_BACKOFF_MS)` (the back-off is a parameter only so tests don't wait); `translate(selectedLanguages, modules, parallelTranslation): Flow<TranslationResult>` (a `channelFlow`) iterates modules then languages and aggregates a `UnitOutcome(translated, failed, issue)` per unit; `ProducerScope<TranslationResult>.processTranslation(lang, file, basePairs, outputDir, changeFileCodes, parallelTranslation, completedUnits, totalUnits, moduleName): UnitOutcome` handles one language for one module, emitting per-string progress as each key finishes. `changeFileCodes`/`parallelTranslation` are parameters now (were mutable instance fields)
- `src/main/kotlin/data/translator/TranslationRepository.kt` — interface the manager depends on (`getTranslation(fromLanguage = "en", toLanguage, query): NetworkResponse<String>`); implemented by `MyTranslatorRepoImpl`, faked in tests
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — `translate()`: launches IO coroutine, collects the Flow, updates `state.translationResult`; `cancelTranslation()`: cancels the job
- `src/main/kotlin/data/model/TranslationResult.kt` — sealed interface: `Idle`, `UpdateProgress(translatingLang: String, moduleName: String, completedUnits: Int, totalUnits: Int, translatedStrings: Int, totalStrings: Int)`, `TranslationCompleted(translatedKeys, failedKeys, issues: List<String>)` (+ `hasProblems`), `TranslationFailed(exc: Exception)`

## State & data

- **Input:** `List<ModuleExtraction>` cached in ViewModel (re-read from disk after every run); `selectedLanguages` from state. Missing keys include `<string>`, string-array items and plurals items (key scheme in [xml-parsing-writing.md](xml-parsing-writing.md))
- **Output:** translated `strings.xml` files written to `values-<lang>/` directories inside each module's own res folder (`ModuleExtraction.resPath`)
- **State field updated:** `HomeScreenState.translationResult`
- **Progress reporting:** overall progress is a **count** `completedUnits / totalUnits` (`totalUnits` = modules × translatable languages), advancing per completed (module, language) unit — shown in the UI as `<n>/<total>`, not a percentage. Within a unit, per-string progress (`translatedStrings`/`totalStrings`) is emitted as each key finishes (see [progress-reporting.md](progress-reporting.md))

## Dependencies

- `TranslationRepository` (Koin binds it to `MyTranslatorRepoImpl`; injected into `TranslationManager`)
- `FilesHelper` (object; called for XML parse and write)
- `LocalizationUtils` (via `MyTranslatorRepoImpl`)

## Consumers

- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — sole consumer of `TranslationManager.translate()`
- `src/main/kotlin/home_screen/HomeScreenNew.kt` — observes `translationResult` from state to render progress bar, completion summary (incl. problems + retry), or error text
- `src/test/kotlin/data/translator/TranslationManagerTest.kt` — 17 tests with a fake repository (incl. the breaker)

## Notes

- **Module-by-module:** the outer loop is over `modules`; each module is processed fully (all languages) before the next. `mChangeFileCodes` is reset per module. Each module's base (`en`) key set and output dir are independent.
- Missing-key detection: `englishKeys - languageKeys` (set subtraction). If a language file has all keys, no network calls are made for it.
- Parallel mode (`parallelTranslation = true`, the default): uses `async {} / awaitAll()` per language's missing keys. Sequential mode uses a plain `mapNotNull {}` with suspension. In both modes a per-string `UpdateProgress` is `send`-ed after each key finishes (counter is an `AtomicInteger`).
- **`channelFlow`, not `flow`:** `translate()` is a `channelFlow` so per-key progress can be `send`-ed from the concurrent `async` coroutines in parallel mode (a plain `flow`'s `emit` is not concurrency-safe). `processTranslation` is an extension on `ProducerScope<TranslationResult>` to give it `send`.
- **Failures are reported, not hidden.** Each unit returns a `UnitOutcome`; `TranslationCompleted(translatedKeys, failedKeys, issues)` sums them and adds one `"<module> → <lang>: <reason>"` issue per problematic unit. A run where every request failed (offline / blocked) is "Nothing was translated", not a green success. Issue kinds: partial failures (`N of M strings could not be translated`), nothing translated, unchanged-output (below), write failures.
- **Circuit breaker (Google not answering).** `Breaker` per language: once `BREAKER_MIN_FAILED_KEYS = 6` strings have failed *after their full retries* with **zero successes**, the remaining strings of that language return immediately without a request, and the unit reports `"Google is not answering (offline, or rate-limited) — stopped after N failed strings"` (`UnitOutcome.blocked`). Any success disables the breaker for that language (a partly-working language is never cut short). If `BLOCKED_UNITS_BEFORE_ABORT = 3` languages **in a row** are blocked the whole run stops early with a "Stopped early: … press Retry later" issue; one working language resets the count. Without this a blocked run ground through every string of every language (3 attempts each), which prolongs the block.
- **Write failures don't abort the run:** merge/write is wrapped; an unwritable or malformed target file yields an issue and the run continues with the next language (`writeXmlToFile` throws now).
- **Arrays/plurals are all-or-nothing per group:** if any *missing* item of a `string-array`/`plurals` failed, the whole group is dropped from the write (and counted failed) via `FilesHelper.groupOf`, so an array is never written with a missing item that would shift later indexes. Retried on the next run.
- **Unchanged-output check:** Google answers 200 with the source text for unsupported language codes. If >=3 translatable strings all come back identical to the source (`ECHO_CHECK_MIN_SAMPLE`), the language is not written and an issue is reported. Values with no prose (`%d`, digits, `@string/x`) don't count.
- **Per-key failure tolerance:** each missing key is translated via `translateKeyOrNull()`. If all endpoints fail for a key (after retries) it returns `null` and that single key is skipped — the run does NOT abort. Skipped keys stay missing and are retried on the next run. `CancellationException` still propagates and aborts.
- **Concurrency cap:** `requestSemaphore = Semaphore(MAX_CONCURRENT_REQUESTS = 8)` bounds simultaneous in-flight requests (was unbounded `async` fan-out → 429s). Each key is retried up to `MAX_ATTEMPTS = 3` with `RETRY_BACKOFF_MS = 350` linear backoff.
- **Partial write:** even if some keys fail, the successfully translated (usable) subset is written. Writing is skipped only when nothing usable remains for a language. A `Success` with null data counts as a failed attempt (it no longer silently writes the English source).
- Output is produced by `FilesHelper.mergeEntriesIntoXml(file.contents, translatedPairs)` — merges into the EXISTING target file, preserving arrays/plurals/comments/`translatable=false` strings (see [xml-parsing-writing.md](../features/xml-parsing-writing.md)).
- `changeFileCodes` maps a language code back to the folder it was read from when they differ (e.g. `pt-PT` → `pt-rBR`, `in` from an old `values-id` folder → `id`), so the output XML is written into the existing folder instead of a new one.
- After that remap, the folder code is passed through `FilesHelper.toAndroidResFolderCode` so region-qualified locales become valid Android qualifiers on write (`pt-BR` → `values-pt-rBR`, `zh-CN` → `values-zh-rCN`). Without this, Android Studio rejects the generated `values-pt-BR`-style folders.
- `TranslationFailed` wraps any caught non-cancellation exception (e.g. a malformed *source* `strings.xml`); user cancellation is set by the ViewModel (`Exception("Translation Cancelled")`).
