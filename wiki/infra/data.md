# Data Layer

## Overview

No database or cache. Almost all data is in-memory and lost on close — the one persisted exception is **language templates** (`~/.fast-localizer/templates.json`). Data source types: **file system** (read Android project's `values/` dirs; write translated XMLs), **network** (Google Translate HTTP APIs), and the **templates JSON file** in the user home.

---

## Data Sources

### 1. File System

| Component | File | Role |
|-----------|------|------|
| `FolderExtractor` | `data/util/FolderExtractor.kt` | Resolves a res folder OR a project root into a list of `ModuleExtraction`s; scans `values/` dirs, creates missing `strings.xml` |
| `FilesHelper` | `data/FilesHelper.kt` | DOM parse of individual `strings.xml`; writes output XML to disk |

**Module discovery:** `FolderExtractor.extractModules(path)` — if `path` is itself a res folder (has `values/`) it is one module; otherwise `path` is treated as a project root and every `res/` folder beneath it with a `values/strings.xml` is discovered as its own module (skipping `build/`, `.gradle/`, `.git/`, `.idea/`, `node_modules/`, `intermediates/`). Each module is then extracted via `getKeyWithStringsFromFolder`.

**Read path:** `FolderExtractor.extractModules(path)` → per module `getKeyWithStringsFromFolder(resPath)` → `FilesHelper.parseXml(file)` → `ExtractionResult` (wrapped in `ModuleExtraction`)

**Write path:** `FilesHelper.mergeEntriesIntoXml(existingXml, translatedPairs)` → `FilesHelper.writeXmlToFile(xml, file)` (merges into the existing target DOM, preserving arrays/plurals/comments/non-translatable strings)

**Output location:** translated files written in-place inside each module's own res folder, e.g. `<module>/src/main/res/values-fr/strings.xml`.

### 2. Network — Google Translate

| Endpoint | Impl | URL pattern |
|----------|------|-------------|
| Mobile web | `TranslatorApi1Impl` | `https://translate.google.com/m?sl=en&tl=XX&q=TEXT` (HTML response) |
| translate_a/single | `TranslatorApi2Impl` | `https://translate.google.com/translate_a/single?client=gtx&sl=en&tl=XX&q=TEXT&dt=t` (JSON) |
| clients4 dict-chrome | `TranslatorApi3Impl` | `https://clients4.google.com/translate_a/t?client=dict-chrome-ex&sl=en&tl=XX&q=TEXT` (JSON) |

All requests are HTTP GET. `NetworkClient` uses Ktor CIO engine with 90-second connect/request/socket timeouts.

### 3. Language Templates (user home)

`TemplatesRepository` (`data/util/TemplatesRepository.kt`) reads/writes `~/.fast-localizer/templates.json` — the persisted list of user-defined `LanguageTemplate`s (named language sets). Crash-safe: missing/corrupt file → empty list; write failures are swallowed. Loaded once at `HomeScreenViewModel.init`, rewritten on every create/delete. See [language-templates.md](../features/language-templates.md).

---

## Data Models

| Class | Location | Purpose |
|-------|----------|---------|
| `LanguageModel` | `domain/model/LanguagesModel.kt` | Language entry: `langName`, `nativeName`, `langCode`, `onlyWebTranslate` |
| `HomeScreenState` | `home_screen/HomeScreenState.kt` | All UI state |
| `TranslationResult` | `data/model/TranslationResult.kt` | Sealed: `Idle`, `UpdateProgress`, `TranslationCompleted`, `TranslationFailed` |
| `FileXmlData` | `data/FilesHelper.kt` | Parsed XML: `contents`, `keyValuePairs: Map<String, String>`, `languageCode` |
| `ExtractionResult` | `data/util/FolderExtractor.kt` | `selectedLangs`, `extractedFiles: Map<String, String>`, `changeFileCodes: Map<String, String>` (per res folder) |
| `ModuleExtraction` | `data/util/FolderExtractor.kt` | One discovered module: `moduleName`, `resPath` (output dir), `extraction: ExtractionResult`, `baseStringCount` |
| `ModuleSelection` | `home_screen/HomeScreenState.kt` | UI row for a discovered module: `name`, `resPath`, `stringCount`, `selected` |
| `LanguageTemplate` | `data/model/LanguageTemplate.kt` | Persisted named language set: `id`, `name`, `langCodes` |
| `NetworkResponse<T>` | `data/network/NetworkResponse.kt` | Sealed: `Success<T>`, `Failure`, `Loading`, `Idle` |

---

## Data Flow (End-to-End)

```
User enters path → HomeScreenViewModel.loadFileFromPath()
  → FolderExtractor.extractModules()
    → Discover module res folders (or single res folder)
    → Per module: reads values/*, values-*/strings.xml → FilesHelper.parseXml()
  → List<ModuleExtraction> stored in ViewModel

User clicks Start → HomeScreenViewModel.translate()
  → TranslationManager.translate(modules) [Flow]
    For each module (sequential):
     For each language (sequential):
      → Compute missing keys (englishKeys - langKeys)
      → For each missing key (translateKeyOrNull, Semaphore(8), retry x3):
          → LocalizationUtils.sanitizeForTranslation()
          → MyTranslatorRepoImpl.getTranslation() [API rotation]
            → NetworkClient.makeStringNetworkRequest() → Google Translate API
          → LocalizationUtils.restoreAfterTranslation()
          (on failure: skip this key, continue — no run abort)
      → FilesHelper.mergeEntriesIntoXml(existingXml, translatedPairs)  [partial OK]
      → FilesHelper.writeXmlToFile() → File System
      → emit UpdateProgress
  → emit TranslationCompleted / TranslationFailed
```
