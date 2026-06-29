# File Loading & Extraction

## What it does

Resolves a user-supplied path into one or more translatable Android modules. The path can be either a single `res/` folder OR a whole Android project root — in the latter case every module's `res/` folder (with a `values/strings.xml`) is discovered automatically. For each module it reads (or creates) each `strings.xml` under `values*/` and builds an in-memory map of language → key-value pairs. The result is cached in the ViewModel for later module-by-module translation.

## Key files

- `src/main/kotlin/data/util/FolderExtractor.kt` — `extractModules(path)`: suspend fun, resolves res folder vs project root, returns `List<ModuleExtraction>` (each carrying a `baseStringCount` parsed from the module's `values/strings.xml`); `getKeyWithStringsFromFolder(resPath)`: walks a single res folder, creates missing `strings.xml`, maps language codes, returns `ExtractionResult`
- `src/main/kotlin/data/FilesHelper.kt` — `parseXml(file)`: DOM parse of a single `strings.xml`; `extractLanguageCode(folderName)`: regex to strip `values-` prefix; `getFilesXmlContents(files)`: batch parse
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — `loadFileFromPath(path)`: launches IO coroutine, calls `FolderExtractor.extractModules`, emits one-time event, caches `List<ModuleExtraction>`, surfaces module names in state

## State & data

- **Triggered by:** user enters path → clicks "Load File" → `viewModel.loadFileFromPath(path)`
- **Reads:** file system — either `<path>/values*/strings.xml` (single res folder) or `<root>/**/res/values*/strings.xml` across all modules
- **Output stored in:** `HomeScreenViewModel` private `modules: List<ModuleExtraction>` field; a UI-facing `List<ModuleSelection>` (name, resPath, stringCount, selected) is exposed via `HomeScreenState.modules`. Only selected modules are passed to translation.
- **One-time event emitted:** `HomeScreenOneTimeEvents.FileLoadedSuccess` or `FileLoadedFail`
- **Module discovery:** if `path/values` exists → single module; otherwise walk the root for `res/` dirs containing `values/strings.xml`, skipping `build`, `.gradle`, `.git`, `.idea`, `node_modules`, `intermediates`. Module name derived as the segment before `/src` (e.g. `editor/src/main/res` → `editor`).
- **Language code remapping** (handled in `FilesHelper.extractLanguageCode`): Android qualifier folders are normalized back to locale codes (`pt-rBR → pt-BR`, `zh-rCN → zh-CN`, `b+ms+Arab → ms-Arab`), legacy/Google remaps applied (`in → id`, `he → iw`, `ji → yi`, `zh → zh-CN`), then resolved to the exact `availableLanguages` code with a base-language fallback (`pt-BR → pt`, `es-MX → es`). This is what lets existing `values-pt-rBR`/`values-zh-rCN`/etc. folders be auto-detected and their language auto-selected.
- **Pre-selected languages:** union of `selectedLangs` across all discovered modules.

## Dependencies

- `FolderExtractor` (object, called directly from ViewModel)
- `FilesHelper` (object, called from FolderExtractor)
- `AvailableLanguages` (used by FolderExtractor to match language codes)

## Consumers

- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — calls `FolderExtractor.extractModules`; stores `List<ModuleExtraction>`; passes it to `TranslationManager.translate()`
- `src/main/kotlin/home_screen/HomeScreenNew.kt` — triggers `loadFileFromPath()`, observes one-time events for snackbar, displays `loadedPath` and `discoveredModules` from state

## Notes

- If a `values-<lang>/` folder exists but has no `strings.xml`, `FolderExtractor` creates an empty one before parsing.
- The `values/` folder (English base) is included in the extraction; it provides the reference key set for detecting missing translations.
- A module is only discovered if its `values/strings.xml` base file exists, so res folders without strings are ignored.
- Backward compatible: pointing the app directly at a single `res/` folder still works (it becomes a one-element module list).
- `changeFileCodes` in `ExtractionResult` maps from the on-disk code (e.g. `in`) to the canonical code (e.g. `id`) so written XML files use the right folder name.
