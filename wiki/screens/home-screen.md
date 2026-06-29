# Home Screen

## What it does

The main application screen. Left column holds the language selection grid; right column has the folder path input (a `res/` folder or an Android project root), file-load button, a Language Templates card, a selectable module list, start/stop translation button, and a progress/completion area. When a project root is loaded, each discovered module is shown with a checkbox and its base-string count so the user can include/exclude individual modules before translating. All user interaction in the app flows through this screen.

## Key files

- `src/main/kotlin/home_screen/HomeScreenNew.kt` — root Composable; orchestrates left/right layout, collects state and one-time events, renders snackbars and dialogs
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — all business logic: file loading, translation, cancellation, language management
- `src/main/kotlin/home_screen/HomeScreenState.kt` — immutable data class; the single source of truth for all UI state
- `src/main/kotlin/home_screen/HomeScreenOneTimeEvents.kt` — sealed interface for fire-once events (FileLoadedSuccess, FileLoadedFail)
- `src/main/kotlin/home_screen/components/RectangleWithShadow.kt` — elevated card with optional blink animation; used for status/result boxes
- `src/main/kotlin/home_screen/components/RoundedCard.kt` — material Card button with optional stroke; used for Load File and Start buttons
- `src/main/kotlin/home_screen/components/TemplatesCard.kt` — Language Templates card: save current selection, apply/delete templates (see [language-templates.md](../features/language-templates.md))

## State & data

- **State holder:** `HomeScreenViewModel` — plain Kotlin class, `CoroutineScope(Dispatchers.IO)` for IO work
- **State flow:** `viewModel.state: StateFlow<HomeScreenState>` collected in the Composable
- **One-time events:** `viewModel.oneTimeUiEvents: Flow<HomeScreenOneTimeEvents>` consumed via `LaunchedEffect`
- **Key state fields:**
  - `folderPath: String` — current text in path input (a res folder OR a project root)
  - `loadedPath: String` — path of successfully loaded folder (cleared by clear button)
  - `modules: List<ModuleSelection>` — discovered modules, each with `name`, `resPath`, `stringCount` (base `values/strings.xml` count) and `selected`; rendered as a checkbox list (`ModulesSelectionCard`). Only `selected` modules are translated.
  - `selectedLanguages: MutableSet<LanguageModel>` — currently checked languages
  - `translationResult: TranslationResult` — Idle / UpdateProgress / TranslationCompleted / TranslationFailed
  - `availableLanguages`, `filteredList: List<LanguageModel>` — full list and search-filtered list
  - `parallelTranslation: Boolean` — async vs sequential (not exposed in current UI, default true)

## Dependencies

- `HomeScreenViewModel` (Koin `koinInject()`)
- `LanguagesScreen` (left panel, receives viewModel + state as params)
- `TemplatesCard` (right column; calls `viewModel.createTemplate/applyTemplate/deleteTemplate`, reports outcomes via `onMessage` → snackbar)

## Consumers

- `src/main/kotlin/Main.kt` — `App()` Composable renders `HomeScreenNew()` as the main content area

## Notes

- The Language Templates card (`TemplatesCard`) replaces the former Import/Export Languages controls. Save a named set from the current selection, apply one in a click (replaces selection), delete with confirmation. Persisted at `~/.fast-localizer/templates.json`. See [language-templates.md](../features/language-templates.md).
- The `showFileLoadedSnackBar` / `fileLoadingStatus` snackbar is local state in the Composable, set in response to file-load one-time events and reused by `TemplatesCard`'s `onMessage` callback (save/apply/delete confirmations).
- The "Open Now" button (shown on `TranslationCompleted`) calls `openDownloadsFolder(loadedPath)` via `Desktop.getDesktop().open()`.
- "Stop Translation" button replaces "Start Translation" when `translationResult is UpdateProgress`.
- The `UpdateProgress` branch renders a `Column` of `ProgressRow`s (private helper: label left, count right, full-width rounded bar below — so the two bars line up identically): an overall language-level bar showing a **count** `<n>/<total>` (e.g. `2/10`, not a percentage) and, when `totalStrings > 0`, a per-language string bar showing `<done>/<total>`. See [progress-reporting.md](../features/progress-reporting.md).
- The right-hand control column is wrapped in `verticalScroll(rememberScrollState())` so its cards (path, templates, module list, controls, progress) stay reachable when content exceeds the window height. The nested `ModulesSelectionCard` keeps its own bounded `heightIn(max = 220.dp)` scroll, so same-direction nesting is valid.
- `ModulesSelectionCard` (private Composable in `HomeScreenNew.kt`) lists each module with a `Checkbox`, its name, `<n> strings`, and a **View** action. A "Select all / Unselect all" toggle flips every module. `viewModel.toggleModule(resPath)` toggles one; `toggleModule("", selectAll = true)` toggles all. Start is disabled if no module is selected.
- **View** opens `ModuleStringsDialog` — a custom in-app Compose `Dialog` with a **fixed-size `Surface` (720×560.dp)**, showing the module's base `values/strings.xml` in a monospace `SelectionContainer` (`softWrap = false`). The scroll area is bounded with `weight(1f)` inside the fixed-height column, so it never grows the window while scrolling; it has a desktop `VerticalScrollbar` on the right and `HorizontalScrollbar` along the bottom (`rememberScrollbarAdapter`). Content comes from `viewModel.moduleStringsXml(resPath)` (already in memory — no disk re-read, no OS "open with"). The viewed module is tracked by a local `viewingModule: ModuleSelection?` state. (Material `AlertDialog` was avoided here because its `text` slot leaves the scroll area unbounded, which made the dialog re-measure and grow while scrolling.)
