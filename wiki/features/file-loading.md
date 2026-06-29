# File Loading & Extraction

## What it does

Scans a user-supplied path for Android `values/` and `values-<lang>/` directories, reads (or creates) each `strings.xml`, and builds an in-memory map of language → key-value pairs. The result is cached in the ViewModel for later translation.

## Key files

- `src/main/kotlin/data/util/FolderExtractor.kt` — `getKeyWithStringsFromFolder(path)`: suspend fun, walks folder, creates missing `strings.xml`, maps language codes, returns `ExtractionResult`
- `src/main/kotlin/data/FilesHelper.kt` — `parseXml(file)`: DOM parse of a single `strings.xml`; `extractLanguageCode(folderName)`: regex to strip `values-` prefix; `getFilesXmlContents(files)`: batch parse
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — `loadFileFromPath(path)`: launches IO coroutine, calls `FolderExtractor`, emits one-time event, caches `ExtractionResult`

## State & data

- **Triggered by:** user enters path → clicks "Load File" → `viewModel.loadFileFromPath(path)`
- **Reads:** file system (`values/`, `values-*/strings.xml`)
- **Output stored in:** `HomeScreenViewModel` private `extractionResult: ExtractionResult?` field
- **One-time event emitted:** `HomeScreenOneTimeEvents.FileLoadedSuccess` or `FileLoadedFail`
- **Language code remapping** (handled in `FolderExtractor`): `zh-rCN → zh-CN`, `in → id`, `he → iw`, `ji → yi`, etc.

## Dependencies

- `FolderExtractor` (object, called directly from ViewModel)
- `FilesHelper` (object, called from FolderExtractor)
- `AvailableLanguages` (used by FolderExtractor to match language codes)

## Consumers

- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — calls `FolderExtractor`; stores `ExtractionResult`; passes it to `TranslationManager.translate()`
- `src/main/kotlin/home_screen/HomeScreenNew.kt` — triggers `loadFileFromPath()`, observes one-time events for snackbar, displays `loadedPath` from state

## Notes

- If a `values-<lang>/` folder exists but has no `strings.xml`, `FolderExtractor` creates an empty one before parsing.
- The `values/` folder (English base) is included in the extraction; it provides the reference key set for detecting missing translations.
- `changeFileCodes` in `ExtractionResult` maps from the on-disk code (e.g. `in`) to the canonical code (e.g. `id`) so written XML files use the right folder name.
