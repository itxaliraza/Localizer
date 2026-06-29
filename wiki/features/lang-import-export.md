# Language Import / Export

## What it does

Lets users save their current language selection to a JSON file (in Downloads) and reload it later via a file picker. The file format is a JSON array of language code strings, e.g. `["fr","de","es"]`.

## Key files

- `src/main/kotlin/data/util/LangImportExportHelper.kt` — `exportLanguageCodesToJson(selectedLanguages)`: serializes codes to JSON, writes to `Downloads/<timestamp>.txt`; `importLanguageCodesFromJson(file)`: reads JSON, returns `List<String>`
- `src/main/kotlin/home_screen/HomeScreenNew.kt` — "Export Languages" button calls `exportLanguageCodesToJson()`, shows snackbar; "Import Languages" button opens AWT `FileDialog`, calls `importLanguageCodesFromJson()`, then `viewModel.importLanguageCodesFromJson()` to update selection
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — `importLanguageCodesFromJson(codes)`: matches codes against `availableLanguages`, adds matching models to `selectedLanguages`

## State & data

- **Export reads:** `HomeScreenState.selectedLanguages`
- **Import writes:** `HomeScreenState.selectedLanguages` (via ViewModel method)
- **File location (export):** user's `Downloads/` folder, filename includes timestamp
- **File format:** plain text file containing a JSON array of lang code strings

## Dependencies

- `kotlinx-serialization-json` for JSON encode/decode
- `LangImportExportHelper` (top-level functions, no class)
- AWT `FileDialog` for file picker (called from Composable, bridged to AWT Frame)

## Consumers

- `src/main/kotlin/home_screen/HomeScreenNew.kt` — both buttons and all call sites

## Notes

- The `FileDialog` is opened with the AWT window reference obtained via `LocalWindow.current` in the Composable.
- Import is additive: it merges newly imported codes into the existing `selectedLanguages` set rather than replacing it.
- Exported files use `.txt` extension (not `.json`) for broad compatibility.
