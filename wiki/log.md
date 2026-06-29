# Changelog

Append-only. One entry per change session. Format: `## YYYY-MM-DD — <summary>`

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
