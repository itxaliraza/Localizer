# Language Selection

## What it does

Allows the user to pick which target languages to translate into. Supports individual toggle, Select All / Unselect All, and a live search/filter by name or code. Selection is held as a `MutableSet<LanguageModel>` in the ViewModel state.

## Key files

- `src/main/kotlin/languages_screen/LanguagesScreen.kt` — UI grid; handles click → `viewModel.updateSelectedLanguages()`
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — `updateSelectedLanguages(model, selectAll)`: adds/removes from `selectedLanguages`; `searchLanguage(text)`: filters `availableLanguages` into `filteredList`
- `src/main/kotlin/data/AvailableLanguages.kt` — lazy `List<LanguageModel>` with 250+ entries loaded once at startup
- `src/main/kotlin/domain/model/LanguagesModel.kt` — `LanguageModel(langName, nativeName, langCode, onlyWebTranslate)`

## State & data

- **State holder:** `HomeScreenViewModel`
- **State fields:** `selectedLanguages: MutableSet<LanguageModel>`, `availableLanguages: List<LanguageModel>`, `filteredList: List<LanguageModel>`, `searchedText: String`
- **Init:** `availableLanguages` and `filteredList` both set to `availableLanguages` in `HomeScreenViewModel.init {}`

## Dependencies

- `AvailableLanguages` (global lazy list, accessed directly in ViewModel init)
- `LanguagesModel.kt` (domain model)

## Consumers

- `src/main/kotlin/languages_screen/LanguagesScreen.kt` — renders selection state
- `src/main/kotlin/home_screen/HomeScreenNew.kt` — passes selection count to header; provides Import/Export buttons that read/write `selectedLanguages`
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — `translate()` reads `selectedLanguages` to determine which languages to generate
- `src/main/kotlin/data/util/LangImportExportHelper.kt` — export writes `selectedLanguages.map { it.langCode }` to JSON

## Notes

- `MutableSet` inside an immutable `HomeScreenState` data class is a subtle pattern: the set is mutated in-place, then `state.update { it.copy(...) }` is called to trigger recomposition. This means reference equality of `selectedLanguages` doesn't change, but set membership does.
- `onlyWebTranslate` on `LanguageModel` affects which API endpoint is used during translation (see [translation-api.md](translation-api.md)).
