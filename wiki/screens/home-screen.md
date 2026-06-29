# Home Screen

## What it does

The main application screen. Left column holds the language selection grid; right column has the folder path input, file-load button, import/export controls, start/stop translation button, and a progress/completion area. All user interaction in the app flows through this screen.

## Key files

- `src/main/kotlin/home_screen/HomeScreenNew.kt` — root Composable; orchestrates left/right layout, collects state and one-time events, renders snackbars and dialogs
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — all business logic: file loading, translation, cancellation, language management
- `src/main/kotlin/home_screen/HomeScreenState.kt` — immutable data class; the single source of truth for all UI state
- `src/main/kotlin/home_screen/HomeScreenOneTimeEvents.kt` — sealed interface for fire-once events (FileLoadedSuccess, FileLoadedFail)
- `src/main/kotlin/home_screen/components/RectangleWithShadow.kt` — elevated card with optional blink animation; used for status/result boxes
- `src/main/kotlin/home_screen/components/RoundedCard.kt` — material Card button with optional stroke; used for Load File, Import, Export, Start buttons

## State & data

- **State holder:** `HomeScreenViewModel` — plain Kotlin class, `CoroutineScope(Dispatchers.IO)` for IO work
- **State flow:** `viewModel.state: StateFlow<HomeScreenState>` collected in the Composable
- **One-time events:** `viewModel.oneTimeUiEvents: Flow<HomeScreenOneTimeEvents>` consumed via `LaunchedEffect`
- **Key state fields:**
  - `folderPath: String` — current text in path input
  - `loadedPath: String` — path of successfully loaded file (cleared by clear button)
  - `selectedLanguages: MutableSet<LanguageModel>` — currently checked languages
  - `translationResult: TranslationResult` — Idle / UpdateProgress / TranslationCompleted / TranslationFailed
  - `availableLanguages`, `filteredList: List<LanguageModel>` — full list and search-filtered list
  - `parallelTranslation: Boolean` — async vs sequential (not exposed in current UI, default true)

## Dependencies

- `HomeScreenViewModel` (Koin `koinInject()`)
- `LanguagesScreen` (left panel, receives viewModel + state as params)
- AWT `FileDialog` for Import Languages file picker
- `LangImportExportHelper` (called directly from ViewModel)

## Consumers

- `src/main/kotlin/Main.kt` — `App()` Composable renders `HomeScreenNew()` as the main content area

## Notes

- `JsonGuideDialog` (an `AlertDialog`) is shown when user needs help with the JSON import format; triggered by a local `showImportGuideDialog` boolean.
- Snackbars (`showSnackBar`, `showFileLoadedSnackBar`) are local state in the Composable, set in response to one-time events.
- The "Open Now" button (shown on `TranslationCompleted`) calls `openDownloadsFolder(loadedPath)` via `Desktop.getDesktop().open()`.
- "Stop Translation" button replaces "Start Translation" when `translationResult is UpdateProgress`.
