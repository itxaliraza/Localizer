# Changelog

Append-only. One entry per change session. Format: `## YYYY-MM-DD — <summary>`

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
