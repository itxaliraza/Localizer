# Languages Screen

## What it does

Left panel of the main window. Displays a searchable, scrollable grid of all 250+ supported languages. Users check/uncheck individual languages or use Select All / Unselect All. A counter shows how many are selected out of total available (after search filter).

## Key files

- `src/main/kotlin/languages_screen/LanguagesScreen.kt` — root Composable (`LanguagesScreen`), search bar, select-all button, lazy grid with custom scrollbar (`LazyVerticalGridWithScrollIndicator`), individual item (`LanguagesItems`)
- `src/main/kotlin/data/AvailableLanguages.kt` — lazy `List<LanguageModel>` of 250+ language definitions
- `src/main/kotlin/domain/model/LanguagesModel.kt` — `LanguageModel(langName, nativeName, langCode, onlyWebTranslate)`

## State & data

- **State holder:** `HomeScreenViewModel` (passed as parameter — this screen owns no ViewModel of its own)
- **Reads from state:** `state.filteredList`, `state.selectedLanguages`, `state.availableLanguages`, `state.searchedText`
- **Writes via:** `viewModel.searchLanguage(text)`, `viewModel.updateSelectedLanguages(model)`, `viewModel.updateSelectedLanguages(null, selectAll = true/false)`

## Dependencies

- `HomeScreenViewModel` (parameter, injected by parent)
- `HomeScreenState` (parameter, collected by parent)
- `RectangleWithShadow` and `RoundedCard` (common components) for item cards
- `EditText` (common component) for search bar

## Consumers

- `src/main/kotlin/home_screen/HomeScreenNew.kt` — renders `LanguagesScreen` in the left column, passes `viewModel` and `state`

## Notes

- Grid uses `LazyVerticalGrid` with `GridCells.Adaptive(145.dp)`.
- Custom scrollbar is a `Box` with a draggable thumb overlay (not Compose's built-in `VerticalScrollbar`). Drag updates scroll position via `LazyGridState.scrollToItem()`.
- `onlyWebTranslate` flag on `LanguageModel` indicates that only `TranslatorApi1Impl` (mobile web endpoint) can handle that language; the API layer routes accordingly.
- Search filters by both `langName` and `langCode` (case-insensitive contains).
