# Changelog

Append-only. One entry per change session. Format: `## YYYY-MM-DD — <summary>`

---

## 2026-09-20 — "Translation still failed": web-only languages routed through the blocked scraper; route via JSON, add endpoint cool-down

**Reported:** a second real-run log — az and km still failing on every string ("translation container not found").

**What the log confirmed:** the earlier fixes work — one "Trying…" line per attempt (was four) and the breaker stopped az/km after ~8 strings instead of all 69. The remaining failure is Google's captcha block on the HTML scraper (`/m` → 302 → `google.com/sorry/index`, still active).

**Root cause of the *languages* failing:** 116 languages were flagged `onlyWebTranslate` and routed **only** to that scraper, so a block meant they could not be translated at all. Probed live one request at a time, **the JSON endpoints translate 115 of those 116** (az, km, om, pa, pt, sv, tk, … all fine, also with token-bearing strings); only `itg` returns the source unchanged. **I had told you (and written in the wiki) that the JSON endpoint rejects those codes — that was wrong.** It came from one transient failure during a 4-thread sweep that I misread as "unsupported".

**Fix (`MyTranslatorRepoImpl`):**
- All languages use the same list: JSON endpoints first, scraper last. `onlyWebTranslate` is now informational.
- Endpoint cool-down (60 s): an endpoint whose request failed is tried last until it recovers. Before, the rotation started **a third of all requests** at the blocked scraper, failed, then fell back — extra traffic to the endpoint that was blocking us. If every endpoint is cooling, all are still tried.
- Tests: replaced the "web-only uses only the scraper" test (it asserted the removed behaviour), added web-only-via-JSON, scraper-fallback, and three cool-down tests with a fake clock (86 pass).

**Measured (live, real code path, real routing):** `LiveLanguageSweepTest` — **242/242 languages pass**, 126 JSON-endpoint + **116 web-only** (2.5 min). 7 needed a fallback token style: dz, new, or, ty, tt, sah (`%n%`); ug (all four). The sweep checks placeholder/markup integrity, not translation quality.

**Files touched:** `src/main/kotlin/data/translator/MyTranslatorRepoImpl.kt`, `src/test/kotlin/data/translator/MyTranslatorRepoImplTest.kt`, `src/test/kotlin/data/translator/LiveLanguageSweepTest.kt`; wiki: `features/translation-api.md`, `features/string-sanitization.md`, `features/language-selection.md`, `screens/languages-screen.md`, `infra/build.md`, `log.md`.

---

## 2026-09-20 — Real-run log review: fix 4× request amplification, add circuit breaker, confirm the scraper block

**Reviewed:** a console log from a real run over ~130 language folders. Findings:

1. **Extraction is right.** Only `values` + real language folders were read (`values-in`, `-iw`, `-ji`, `-zh` legacy codes included); no configuration-qualifier folders. The `????` in the printed `LanguageModel` names is the Windows console code page (the strings are correct in memory) — display-only, not a data problem. (The `println` debug output itself is noise; not removed.)
2. **Web-only languages (az, km, om, …) fail with "translation container not found" — verified cause: Google's captcha block, not a markup change.** `curl` of the scraper URL returns `HTTP 302 → https://www.google.com/sorry/index?continue=…` while `translate_a/single` returns 200 from the same machine. IP-level, temporary; triggered by heavy testing here.
3. **Bug of mine, found in the log: 4 requests per failed request.** The new token-style ladder also advanced to the next style when a *request* failed, so every failure was sent 4× (12 per string for normal languages incl. retries) — four "Trying translation api 0 error" lines per "attempt N failed". Against a blocked endpoint that made the block worse. Reproduced with a failing test (`expected 1 but was 4`, `[4,4,4]`), then fixed: a style is only retried when an endpoint *answered* with a damaged result; if nothing answered, `MyTranslatorRepoImpl` returns at once.
4. **The app ground through everything while blocked** (every string of every language × 3 attempts). Added a circuit breaker in `TranslationManager`: a language stops after 6 fully-retried failures with no success; 3 blocked languages in a row stop the run with a "Stopped early … press Retry later" issue. A single success disables it for that language, one working language resets the run counter. Mutation-checked: the two breaker tests fail with the breaker disabled.

**Tests:** 82 pass (added 2 repo tests, 4 manager tests; manager suite is faster via the injectable back-off).

**Not verified at the time:** the 116 web-only languages. (Resolved in the entry above this one.)

**Files touched:** `src/main/kotlin/data/translator/MyTranslatorRepoImpl.kt`, `src/main/kotlin/data/translator/TranslationManager.kt`, `src/test/kotlin/data/translator/MyTranslatorRepoImplTest.kt`, `src/test/kotlin/data/translator/TranslationManagerTest.kt`; wiki: `features/translation-api.md`, `features/translation-orchestration.md`, `features/string-sanitization.md`, `infra/build.md`, `log.md`.

---

## 2026-09-20 — Fix placeholder corruption in Serbian/Icelandic (and Hindi/Amharic): letter-free tokens + style ladder

**Reported:** in some languages `%1$d` came back as `КСКСПХ1_дКСКС` (Serbian) and `%2$d` as `XXH2_dXX` (Icelandic).

**Cause:** the sanitizer swapped placeholders for letter-based tokens (`XXPH1_dXX`). Google transliterates or truncates letters per language. The earlier rewrite (`XXPH1XX`) did **not** fix it — reproduced live: Serbian `КСКСПХ0КСКС`, Icelandic `XXH0XX`; Hindi and Amharic also broke. The validation added earlier stopped corrupt strings being written, but those languages' strings would simply have failed every time.

**Fix:**
- Tokens are now **letter-free**: `@n@` (digits and symbols aren't transliterated). Token regex accepts digits in any script.
- Because no single style survives every language (Google sometimes drops a token next to a word it deletes, e.g. "now" in Tatar/Uyghur/Odia), `LocalizationUtils.TokenStyle` is a **ladder** `@n@ → %n%  → [[n]] → ⟦n⟧`; `MyTranslatorRepoImpl` retries a *damaged* response with the next style on the same endpoint, and moves to the next endpoint only when a *request* fails.
- New opt-in `LiveLanguageSweepTest` (`LIVE_SWEEP=1`) that runs every language through the real code path.
- Tests updated/added (76 pass): style round-trips, no letters in tokens, non-Latin digits, sr/is outputs, ladder behaviour.

**Measured (live, real code path):**
- 21 hard-script languages, old `XXPH` format: 4 failed (sr, is, hi, am). `@n@`, `%n%`, `[[n]]`, `<n>`, `⟦n⟧` all 21/21.
- **126 of 126 languages served by the JSON endpoints pass on both test strings** (`%1$d`/`%2$d` sentence, and `<b>…</b>` + `%1$s` with "now, please"). Alone, `[[n]]` failed 6 (gu, ky, or, tt, ug, yo), `@n@` failed 1 (tt), `%n%` failed 1 (ug). With the ladder: `or`, `tt` needed `%n%`; `ug` needed `⟦n⟧`.
- **NOT verified: the 116 web-only languages.** Google returned HTTP 429 for the HTML scraper (`/m`) from this machine during testing, and I believed the JSON endpoint rejected those language codes. **That belief was wrong** — see the 2026-09-20 "web-only languages" entry above, which re-ran them and got 116/116.

**Files touched:** `src/main/kotlin/data/util/LocalizationUtils.kt`, `src/main/kotlin/data/translator/MyTranslatorRepoImpl.kt`, `src/test/kotlin/data/util/LocalizationUtilsTest.kt`, `src/test/kotlin/data/translator/MyTranslatorRepoImplTest.kt`, `src/test/kotlin/data/translator/LiveLanguageSweepTest.kt` (new); wiki: `features/string-sanitization.md`, `features/translation-api.md`, `infra/build.md`, `log.md`.

---

## 2026-09-20 — Translation-correctness pass: failure reporting, read-only load, Koin start, packaging check, markup/arrays/plurals, tests

**What changed:** Fixed the issues found in the review, in this order.

1. **Failures no longer look like success.** `TranslationCompleted` is now `TranslationCompleted(translatedKeys, failedKeys, issues)`. `TranslationManager` aggregates a per-(module, language) `UnitOutcome`; write/merge errors are caught per language (run continues) and `FilesHelper.writeXmlToFile` now **throws** instead of swallowing. The UI shows an amber "Nothing was translated / Completed with problems" summary with issue lines and a **Retry failed strings** button. After every run (finished/failed/cancelled) the ViewModel re-reads the modules from disk so a retry only translates what is still missing (was: re-translated everything from the load-time snapshot). ViewModel also now uses one `SupervisorJob` scope, cancels a superseded load, and ignores `translate()` while a run is active.
2. **Loading no longer writes into the project.** `FolderExtractor` used to create an empty `strings.xml` in *every* `values*` folder (incl. `values-night`, `values-v29`). It is now read-only, reads only `values` + real language folders (`FilesHelper.isLanguageFolder`), and models a missing language `strings.xml` in memory.
3. **`startKoin` moved out of the `App()` composable into `main()`** (a recomposition would have thrown "Koin already started").
4. **Packaging verified.** `createDistributable`, the launched app and `packageExe` (installer built) all work. Added `modules("java.instrument","java.management","jdk.unsupported")` (Compose `suggestRuntimeModules`); a real Ktor request also worked without them in a matching jlink image, so it is precautionary.
5. **Markup & placeholders (#3).** `parseXml` keeps inner XML instead of `textContent`. `LocalizationUtils` rewritten: tags/CDATA/comments and all printf/`{name}` placeholders become numbered tokens (with anti-gluing padding); `restoreAfterTranslation` returns `null` if a token is lost/duplicated/invented or reordering leaves malformed markup, and `MyTranslatorRepoImpl` then tries the next endpoint. Values with no prose (or `@string/x` refs) skip the network. Live-checked against Google: tokens survived 43/44 samples over 15 languages; the miss is what the validation rejects.
6. **Arrays and plurals (#4).** `<string-array>` and `<plurals>` items are parsed (keys `array:<n>:<i>`, `plurals:<n>:<q>`), translated, and merged; a group with any failed item is dropped whole so an array is never written misaligned. `mergeEntriesIntoXml` now uses a deterministic serializer (`serializeResources`) instead of the JDK `Transformer` (which corrupted whitespace in mixed-content strings), keeps whitespace inside `<string>`, adds `xmlns:xliff` when needed, and writes atomically.
7. **Bad results are not written as translations (#5).** Blank/null results are failures; ≥3 translatable strings all identical to the source ⇒ language treated as unsupported (Google returns 200 + source for unknown codes) and not written.
8. **Testability.** New `TranslationRepository` interface (Koin: `factory<TranslationRepository> { MyTranslatorRepoImpl(...) }`), `MyTranslatorRepoImpl` takes `TranslatorApis`; `TranslationManager` takes the repo interface and no longer keeps `mParallelTranslation`/`mChangeFileCodes` as mutable fields.
9. **Tests:** added JUnit 5 setup and 69 tests (`LocalizationUtilsTest`, `FilesHelperTest`, `FolderExtractorTest`, `MyTranslatorRepoImplTest`, `TranslationManagerTest`) — all pass.

**Known limits (documented in the wiki):** inline-tag *placement* is best-effort (translator may move words out of a tag; valid XML, lost emphasis); plurals aren't adapted to each language's quantity set; XXE hardening of the DOM parser not done; endpoints are unofficial and can be rate-limited (an HTML "Sorry…" page is treated as an endpoint failure).

**Files touched:** `build.gradle.kts`, `src/main/kotlin/Main.kt`, `di/SharedModule.kt`, `data/FilesHelper.kt`, `data/model/TranslationResult.kt`, `data/translator/{TranslationManager,MyTranslatorRepoImpl,TranslationRepository}.kt`, `data/util/{LocalizationUtils,FolderExtractor}.kt`, `home_screen/{HomeScreenViewModel,HomeScreenNew}.kt`, `theme/Colors.kt`, `src/test/kotlin/**` (5 new test classes); wiki: `index.md`, `architecture.md`, `screens/home-screen.md`, `features/{file-loading,translation-orchestration,translation-api,string-sanitization,xml-parsing-writing,parallel-translation,translation-cancellation,progress-reporting}.md`, `infra/{build,di,data,navigation}.md`, `log.md`.

---

## 2026-09-20 — Upgrade all libraries/plugins to latest stable

**What changed:** Bumped every dependency and plugin to its latest stable release (pre-releases skipped). Gradle wrapper was already 9.7.1.

- Plugins: Kotlin 2.0.0 → 2.4.20, Compose Multiplatform 1.8.0-beta01 → 1.9.3 (the stale `1.6.10` in `gradle.properties` was previously overridden by a hardcoded version in `build.gradle.kts`). Versions now live only in `gradle.properties`; `settings.gradle.kts` also pins the Kotlin Serialization plugin to `kotlin.version`, and `build.gradle.kts` declares plugins without versions.
- Libraries: Ktor 2.3.12/2.3.4 → 3.6.0, kotlinx-serialization-json 1.6.0 → 1.11.0, Koin 4.0.0-RC2 → 4.2.2, slf4j 2.0.9 → 2.0.19, logback 1.4.11 → 1.6.3, org.json 20210307 → 20260814, junrar 7.5.5 → 8.1.1.
- Compose 1.8+ dropped bundled Material icons from the `compose.material` accessor → added explicit `material-icons-core:1.7.3`.
- `NetworkClient`: removed now-redundant `?: "..."` fallbacks on `ClientRequestException`/`ServerResponseException` `.message` (non-null in Ktor 3).

**Verified:** `compileKotlin` passes; `./gradlew run` launches with no startup errors. Not exercised: a live translation through Ktor 3 CIO (no automated tests exist).

**Files touched:** `gradle.properties`, `settings.gradle.kts`, `build.gradle.kts`, `src/main/kotlin/data/network/client/NetworkClient.kt`, `wiki/index.md`, `wiki/infra/build.md`, `wiki/infra/di.md`, `wiki/features/translation-api.md`, `wiki/log.md`.

---

## 2026-06-29 — Replace language import/export with in-app Language Templates

**What changed:** Removed the JSON import/export-to-Downloads feature and replaced it with persistent, in-app **language templates**. Users save the current selection as a named set ("Save" pill, enabled only when ≥1 language is selected), apply a template in one click (replaces the selection), and delete with confirmation. The template matching the current selection is highlighted as **Active**. Templates persist to `~/.fast-localizer/templates.json` and load at startup, so selections survive restarts (the old export forgot everything and dumped a `.txt` into Downloads).

- New `LanguageTemplate(id, name, langCodes)` serializable model and `TemplatesRepository` (Koin `single`, crash-safe load/save of the JSON file).
- `HomeScreenState` gained `templates`; `HomeScreenViewModel` gained `createTemplate/applyTemplate/deleteTemplate` and loads templates in `init`. Now constructed as `HomeScreenViewModel(get(), get())`.
- New `TemplatesCard` composable (header + Save pill, template rows with code preview/Active state, save dialog, delete-confirm dialog) replaces the Import/Export card in `HomeScreenNew`. Removed `JsonGuideDialog`, the AWT `FileDialog` import flow, the export snackbar, and deleted `LangImportExportHelper.kt`.
- Also added the missing JDK-21 temp-path fix (`TEMP/TMP=C:\tmp`) to `gradlew.bat` so the build runs on this machine.

**Files touched:** `src/main/kotlin/data/model/LanguageTemplate.kt` (new), `src/main/kotlin/data/util/TemplatesRepository.kt` (new), `src/main/kotlin/home_screen/components/TemplatesCard.kt` (new), `src/main/kotlin/home_screen/HomeScreenState.kt`, `src/main/kotlin/home_screen/HomeScreenViewModel.kt`, `src/main/kotlin/home_screen/HomeScreenNew.kt`, `src/main/kotlin/di/SharedModule.kt`, `src/main/kotlin/data/util/LangImportExportHelper.kt` (deleted), `gradlew.bat`, `wiki/features/language-templates.md` (new, replaces `lang-import-export.md`), `wiki/index.md`, `wiki/screens/home-screen.md`, `wiki/features/language-selection.md`, `wiki/infra/data.md`, `wiki/infra/di.md`, `wiki/infra/navigation.md`, `wiki/architecture.md`, `wiki/log.md`.

## 2026-06-29 — Add per-language string-level progress bar

**What changed:** Translation progress now shows a second bar. The existing bar tracks language-level progress across all (module × language) units; the new bar shows, for the language currently being translated, how many of its strings have finished (`<lang>: <done> / <total> strings`), updating live as each key completes.

- `TranslationResult.UpdateProgress` gained `translatedStrings: Int` and `totalStrings: Int` fields.
- `TranslationManager.translate()` was converted from `flow {}` to `channelFlow {}` so per-key progress can be `send`-ed from the concurrent `async` coroutines in parallel mode. `processTranslation` is now an extension on `ProducerScope<TranslationResult>`; it sets `totalStrings` to the count of missing keys and increments an `AtomicInteger` (then emits) as each key finishes.
- `HomeScreenNew` renders the `UpdateProgress` branch as a `Column` of two `LinearProgressIndicator`s; the per-string bar is shown only when `totalStrings > 0`.

**Files touched:** `src/main/kotlin/data/model/TranslationResult.kt`, `src/main/kotlin/data/translator/TranslationManager.kt`, `src/main/kotlin/home_screen/HomeScreenNew.kt`, `wiki/features/progress-reporting.md`, `wiki/features/translation-orchestration.md`, `wiki/screens/home-screen.md`, `wiki/log.md`.

## 2026-06-29 — Overall progress as a count, aligned two-bar layout

**What changed:** Follow-up to the per-string bar. (1) The overall language-level bar no longer shows a percentage — it shows a count `<n>/<total>` (e.g. `2/10`) of the unit currently being translated. (2) Both bars now share a `ProgressRow` composable (label left, count right, full-width 10.dp rounded bar below) so they're properly aligned, replacing the prior text-overlaid 60.dp bars.

- `UpdateProgress` replaced `progress: Int` (percent) with `completedUnits: Int` + `totalUnits: Int`.
- `TranslationManager.processTranslation` now takes `completedUnits, totalUnits` instead of a precomputed percent and forwards them in every `UpdateProgress`.
- `HomeScreenNew` renders the overall row as count `"${completedUnits + 1}/${totalUnits}"` (bar fraction `completedUnits / totalUnits`) and the per-string row as `"${translatedStrings}/${totalStrings}"`, both via the new private `ProgressRow(label, count, fraction)`.

**Files touched:** `src/main/kotlin/data/model/TranslationResult.kt`, `src/main/kotlin/data/translator/TranslationManager.kt`, `src/main/kotlin/home_screen/HomeScreenNew.kt`, `wiki/features/progress-reporting.md`, `wiki/features/translation-orchestration.md`, `wiki/screens/home-screen.md`, `wiki/log.md`.

## 2026-06-29 — Fix module strings viewer growing/blanking while scrolling

**What changed:** The viewer was a Material `AlertDialog` whose `text` slot leaves content height unbounded, so a fixed-height inner box plus scroll caused the dialog to re-measure and grow, leaving blank gaps when scrolling. Replaced it with a custom `androidx.compose.ui.window.Dialog` + fixed-size `Surface` (720×560.dp). The scroll area is now bounded with `weight(1f)` inside the fixed-height column, with a `VerticalScrollbar` (right) and `HorizontalScrollbar` (bottom), and the text uses `SelectionContainer` + `softWrap = false` so long lines scroll horizontally and stay copyable.

**Files touched:** `src/main/kotlin/home_screen/HomeScreenNew.kt`, `wiki/screens/home-screen.md`, `wiki/log.md`.

## 2026-06-29 — In-app "View" of each module's strings.xml

**What changed:** Added a **View** action to every row in the module list. It opens an in-app Compose dialog (`ModuleStringsDialog`) showing that module's base `values/strings.xml` text in a monospace, scrollable box — rendered inside the app, not handed off to the OS "open with" file association.

- `HomeScreenViewModel.moduleStringsXml(resPath)` returns the in-memory base file content for a module (falls back to any extracted file, then a placeholder).
- `ModulesSelectionCard` gained an `onView` callback and a "View" label per row; `HomeScreenNew` tracks the open module via a local `viewingModule` state and renders `ModuleStringsDialog`.
- The dialog body uses a fixed 400.dp height with a desktop `VerticalScrollbar` (`rememberScrollbarAdapter`) on the right edge plus horizontal scroll for long lines, instead of the earlier variable-height `heightIn` box.

**Files touched:** `src/main/kotlin/home_screen/HomeScreenViewModel.kt`, `src/main/kotlin/home_screen/HomeScreenNew.kt`, `wiki/screens/home-screen.md`, `wiki/log.md`.

## 2026-06-29 — Auto-detect Android qualifier folders (values-pt-rBR, b+ms+Arab) for language pre-selection

**What changed:** Existing translation folders using Android resource qualifier forms — region `values-pt-rBR` / `values-zh-rCN` and BCP47 `values-b+ms+Arab` — were not being recognized, so their language wasn't auto-selected and their existing strings weren't detected (causing needless re-translation). Only `zh-rCN`/`zh-rTW` were special-cased before.

- Added `FilesHelper.fromAndroidResFolderCode(code)` — inverse of `toAndroidResFolderCode`: `pt-rBR`→`pt-BR`, `zh-rCN`→`zh-CN`, `b+ms+Arab`→`ms-Arab`; idempotent for plain codes.
- Added private `FilesHelper.resolveAvailableCode(code)` — resolves a locale code to the exact `availableLanguages` entry (case-insensitive exact, then base-language fallback). This handles the Google/Android mismatch where Brazilian Portuguese is `pt` in the list but `values-pt-rBR` on disk (`pt-BR` → `pt`).
- Rewrote `FilesHelper.extractLanguageCode` to: widen the regex to capture `+` (BCP47 folders), normalize via `fromAndroidResFolderCode`, apply the legacy remaps, then `resolveAvailableCode`. Return is still `rawCode to standardizedCode`, so `changeFileCodes` round-trips write output back to the original folder name.

**Files touched:** `src/main/kotlin/data/FilesHelper.kt`, `wiki/features/xml-parsing-writing.md`, `wiki/features/file-loading.md`, `wiki/log.md`.

## 2026-06-29 — Make right-hand control column scrollable

**What changed:** With the module list added, the right column could overflow the window and hide lower controls. Wrapped the right control `Column` in `verticalScroll(rememberScrollState())` so all cards stay reachable. The nested module list keeps its own bounded `heightIn(max = 220.dp)` scroll.

**Files touched:** `src/main/kotlin/home_screen/HomeScreenNew.kt`, `wiki/screens/home-screen.md`, `wiki/log.md`.

## 2026-06-29 — Selectable module list with per-module string counts

**What changed:** After loading a project root, the user now sees each discovered module in a checkbox list with its base-string count, and can include/exclude individual modules before translating.

- `ModuleExtraction` gained `baseStringCount` — count of translatable `<string>` entries parsed from the module's base `values/strings.xml` (`FolderExtractor.baseStringCountOf`).
- New `ModuleSelection(name, resPath, stringCount, selected)` UI model in `HomeScreenState`; `HomeScreenState.discoveredModules: List<String>` replaced by `modules: List<ModuleSelection>`.
- `HomeScreenViewModel`: `toggleModule(resPath, selectAll)` flips one module or all; `translate()` now filters to only `selected` modules.
- `HomeScreenNew`: new private `ModulesSelectionCard` Composable renders the checkbox list ("<name> … <n> strings") with a Select all / Unselect all toggle; loaded-path label shows "<selected>/<total> module(s) selected"; Start is disabled when no module is selected.

**Files touched:** `src/main/kotlin/data/util/FolderExtractor.kt`, `src/main/kotlin/home_screen/HomeScreenState.kt`, `src/main/kotlin/home_screen/HomeScreenViewModel.kt`, `src/main/kotlin/home_screen/HomeScreenNew.kt`, `wiki/screens/home-screen.md`, `wiki/features/file-loading.md`, `wiki/infra/data.md`, `wiki/log.md`.

## 2026-06-29 — Multi-module support: translate a whole Android project root module by module

**What changed:** The app previously required a single `res/` folder. It can now also be pointed at an Android **project root**: it discovers every module with translatable strings (`<module>/src/main/res/values/strings.xml`) and translates them one module at a time, writing each module's output back into its own res folder.

- `FolderExtractor.extractModules(path)` — resolves a path into `List<ModuleExtraction>`. If the path is itself a res folder (`values/` present) it stays a single module (backward compatible); otherwise the path is treated as a project root and walked for `res/` dirs containing `values/strings.xml`, skipping `build`/`.gradle`/`.git`/`.idea`/`node_modules`/`intermediates`. Module name derived from the segment before `/src`.
- New `ModuleExtraction(moduleName, resPath, extraction)` data class wrapping the existing per-folder `ExtractionResult`.
- `TranslationManager.translate(selectedLanguages, modules, parallelTranslation)` — new signature iterating modules then languages; `processTranslation` simplified (dropped index/total params). Progress is computed over the full `modules × languages` work set.
- `TranslationResult.UpdateProgress` gained a `moduleName` field; UI label now reads `Translating <module> → <lang> (n %)`.
- `HomeScreenViewModel` stores `List<ModuleExtraction>` instead of one `ExtractionResult`; pre-selects the union of on-disk languages across all modules. `HomeScreenState.discoveredModules` surfaces module names; UI shows them under the loaded path. Path input label/hint updated to "res folder or project root".

**Files touched:** `src/main/kotlin/data/util/FolderExtractor.kt`, `src/main/kotlin/data/translator/TranslationManager.kt`, `src/main/kotlin/data/model/TranslationResult.kt`, `src/main/kotlin/home_screen/HomeScreenViewModel.kt`, `src/main/kotlin/home_screen/HomeScreenState.kt`, `src/main/kotlin/home_screen/HomeScreenNew.kt`, `wiki/index.md`, `wiki/screens/home-screen.md`, `wiki/features/file-loading.md`, `wiki/features/translation-orchestration.md`, `wiki/features/progress-reporting.md`, `wiki/infra/data.md`, `wiki/log.md`.

## 2026-06-29 — Fix invalid Android resource folder names for region-qualified locales

**What changed:** The app wrote output folders using raw Google/Locale codes (`values-pt-BR`, `values-zh-CN`, `values-pt-PT`, …), which Android Studio rejects — a region subtag must carry an `r` prefix (`values-pt-rBR`). Reading already standardized `zh-rCN`→`zh-CN`, but there was no inverse on write, so every newly created region-qualified folder was invalid.

- Added `FilesHelper.toAndroidResFolderCode(code)` — converts a Google/Locale code to a valid Android qualifier: 2-letter regions → `lang-rREGION` (`pt-BR`→`pt-rBR`, `zh-CN`→`zh-rCN`, `fa-AF`→`fa-rAF`); script subtags and numeric UN regions → BCP47 `b+` form (`ms-Arab`→`b+ms+Arab`, `es-419`→`b+es+419`). Idempotent: `zh-rCN` and `b+zh+CN` pass through unchanged, so existing-folder round-trips via `changeFileCodes` are preserved.
- `TranslationManager.processTranslation` now runs the post-`changeFileCodes` code through `toAndroidResFolderCode` before building the `values-<code>/strings.xml` path.

**Files touched:** `src/main/kotlin/data/FilesHelper.kt`, `src/main/kotlin/data/translator/TranslationManager.kt`, `wiki/features/xml-parsing-writing.md`, `wiki/features/translation-orchestration.md`.

---

## 2026-06-29 — Core translation hardening: resilience, XML preservation, API1 scraper fix

**What changed:** Reviewed the core translation path against the three live Google endpoints, then fixed three classes of fragility.

1. **Resilience** (`TranslationManager.kt`): missing keys are now translated via `translateKeyOrNull()`, which (a) tolerates per-key failure — a key that fails every endpoint after retries is skipped instead of aborting the entire 250-language run; (b) is bounded by a shared `Semaphore(8)` so parallel mode no longer fan-outs hundreds of concurrent requests (the main cause of 429 cascades); (c) retries up to 3× with linear backoff. Partial results are now written (write skipped only when a language got zero translations). `CancellationException` still propagates.
2. **XML data loss** (`FilesHelper.kt`): replaced `addNewEntriesToXmlNew` (rebuilt the file from scratch, dropping `<string-array>`, `<plurals>`, comments and `translatable="false"` strings) with `mergeEntriesIntoXml`, which appends only new `<string>` entries into the existing DOM and preserves everything else. Also: explicit UTF-8 in `parseXml`, duplicate-name guard, whitespace-node cleanup for clean indentation.
3. **API1 scraper** (`TranslatorApi1Impl.kt`): rewrote `getTranslationData` to search for `class="result-container">` / `class="t0">` explicitly (was relying on a `<!DOCTYPE html>` byte-offset coincidence) and to HTML-unescape the result (`&quot;`, `&amp;`, …) — fixing a latent double-escape of `&` on write.
4. **Repo cleanup** (`MyTranslatorRepoImpl.kt`): moved `lastCalledIndex` from a process-global top-level `var` to a private instance field (matches the documented per-session reset), restored the `ensureActive()` cancellation check that had been swallowed into a comment, removed debug `println` block.
5. **Sanitization** (`LocalizationUtils.kt`): `restoreAfterTranslation` now escapes real `'` and `"` for Android uniformly (API1 unescaping made the old `&quot;`-only hack obsolete).

**Verified:** `./gradlew.bat compileKotlin` passes. Live-tested all three endpoints (placeholders survive untranslated; API1 only emits `result-container` now and HTML-escapes special chars; invalid lang codes return HTTP 200).

**Files touched:** `src/main/kotlin/data/translator/TranslationManager.kt`, `src/main/kotlin/data/translator/MyTranslatorRepoImpl.kt`, `src/main/kotlin/data/translator/apis/TranslatorApi1Impl.kt`, `src/main/kotlin/data/FilesHelper.kt`, `src/main/kotlin/data/util/LocalizationUtils.kt`; wiki: `translation-orchestration.md`, `translation-api.md`, `xml-parsing-writing.md`, `string-sanitization.md`, `parallel-translation.md`.

**Not done (deferred):** request batching via `combineStringsWithLimit` (still dead code), on-disk translation cache, unsupported-language-code detection (HTTP 200 on bad codes).

---

## 2026-06-28 — Initial wiki bootstrap

**What changed:** Created full wiki structure from scratch (Phase 1 bootstrap).

**Files created:**
- `wiki/index.md`
- `wiki/architecture.md`
- `wiki/log.md`
- `wiki/screens/home-screen.md`
- `wiki/screens/languages-screen.md`
- `wiki/features/file-loading.md`
- `wiki/features/language-selection.md`
- `wiki/features/lang-import-export.md`
- `wiki/features/translation-orchestration.md`
- `wiki/features/translation-api.md`
- `wiki/features/xml-parsing-writing.md`
- `wiki/features/string-sanitization.md`
- `wiki/features/parallel-translation.md`
- `wiki/features/progress-reporting.md`
- `wiki/features/translation-cancellation.md`
- `wiki/features/open-output-folder.md`
- `wiki/features/custom-window-controls.md`
- `wiki/infra/navigation.md`
- `wiki/infra/di.md`
- `wiki/infra/data.md`
- `wiki/infra/build.md`

**Source code touched:** none (read-only exploration pass).
