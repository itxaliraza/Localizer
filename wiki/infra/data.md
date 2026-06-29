# Data Layer

## Overview

No database, no SharedPreferences, no persistent cache. All data is in-memory and lost on close. Two data source types: **file system** (read Android project's `values/` dirs; write translated XMLs) and **network** (Google Translate HTTP APIs).

---

## Data Sources

### 1. File System

| Component | File | Role |
|-----------|------|------|
| `FolderExtractor` | `data/util/FolderExtractor.kt` | Scans `values/` dirs, creates missing `strings.xml`, returns `ExtractionResult` |
| `FilesHelper` | `data/FilesHelper.kt` | DOM parse of individual `strings.xml`; writes output XML to disk |

**Read path:** `FolderExtractor.getKeyWithStringsFromFolder(path)` → `FilesHelper.parseXml(file)` → `ExtractionResult`

**Write path:** `FilesHelper.mergeEntriesIntoXml(existingXml, translatedPairs)` → `FilesHelper.writeXmlToFile(xml, file)` (merges into the existing target DOM, preserving arrays/plurals/comments/non-translatable strings)

**Output location:** translated files written in-place inside the user-supplied folder, e.g. `<path>/values-fr/strings.xml`.

### 2. Network — Google Translate

| Endpoint | Impl | URL pattern |
|----------|------|-------------|
| Mobile web | `TranslatorApi1Impl` | `https://translate.google.com/m?sl=en&tl=XX&q=TEXT` (HTML response) |
| translate_a/single | `TranslatorApi2Impl` | `https://translate.google.com/translate_a/single?client=gtx&sl=en&tl=XX&q=TEXT&dt=t` (JSON) |
| clients4 dict-chrome | `TranslatorApi3Impl` | `https://clients4.google.com/translate_a/t?client=dict-chrome-ex&sl=en&tl=XX&q=TEXT` (JSON) |

All requests are HTTP GET. `NetworkClient` uses Ktor CIO engine with 90-second connect/request/socket timeouts.

### 3. Downloads Folder (Export only)

`LangImportExportHelper.exportLanguageCodesToJson()` writes a `.txt` file to the OS `Downloads/` directory. This is a write-only, non-critical output.

---

## Data Models

| Class | Location | Purpose |
|-------|----------|---------|
| `LanguageModel` | `domain/model/LanguagesModel.kt` | Language entry: `langName`, `nativeName`, `langCode`, `onlyWebTranslate` |
| `HomeScreenState` | `home_screen/HomeScreenState.kt` | All UI state |
| `TranslationResult` | `data/model/TranslationResult.kt` | Sealed: `Idle`, `UpdateProgress`, `TranslationCompleted`, `TranslationFailed` |
| `FileXmlData` | `data/FilesHelper.kt` | Parsed XML: `contents`, `keyValuePairs: Map<String, String>`, `languageCode` |
| `ExtractionResult` | `data/util/FolderExtractor.kt` | `selectedLangs`, `extractedFiles: Map<String, String>`, `changeFileCodes: Map<String, String>` |
| `NetworkResponse<T>` | `data/network/NetworkResponse.kt` | Sealed: `Success<T>`, `Failure`, `Loading`, `Idle` |

---

## Data Flow (End-to-End)

```
User enters path → HomeScreenViewModel.loadFileFromPath()
  → FolderExtractor.getKeyWithStringsFromFolder()
    → File System: reads values/*, values-*/strings.xml
    → FilesHelper.parseXml() per file
  → ExtractionResult stored in ViewModel

User clicks Start → HomeScreenViewModel.translate()
  → TranslationManager.translate() [Flow]
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
