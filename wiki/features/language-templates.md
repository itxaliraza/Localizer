# Language Templates

## What it does

Lets users save their current language selection as a named, reusable **template** and apply it later in one click. Replaces the old import/export-to-JSON-in-Downloads flow with in-app, persistent sets. Templates survive app restarts (stored on disk) and are loaded once at startup. Applying a template **replaces** the current selection with exactly that template's languages. The template whose languages exactly match the current selection is highlighted as **Active**.

## Key files

- `src/main/kotlin/data/model/LanguageTemplate.kt` — `@Serializable LanguageTemplate(id, name, langCodes)`; stores `langCode`s (not full models) for a small, resilient file
- `src/main/kotlin/data/util/TemplatesRepository.kt` — Koin `single`; loads/saves the template list as pretty JSON at `~/.fast-localizer/templates.json`. Crash-safe: missing/corrupt file → empty list, write failures swallowed (templates are convenience data). On load, codes are migrated to the current spelling via `LanguageCodeResolver` (`id`→`in`, `itg`→`ltg`, `pt`→`pt-PT`) and the file is rewritten if anything changed
- `src/main/kotlin/home_screen/components/TemplatesCard.kt` — the UI: header with **Import** (secondary pill, opens [language-list-import.md](language-list-import.md)) and **Save** (primary pill, enabled only when ≥1 language selected), template list (name, language count, code preview, Active highlight with a ✓ Active badge, Apply as an accent `PillButton`, delete as an `IconActionButton` with tooltip), the "Save as template" dialog, and the delete-confirm dialog
- `src/main/kotlin/common_components/Buttons.kt` — `PillButton` (Primary / Secondary / Accent styles, hover state) and `IconActionButton` (32dp, muted until hover, then red with a soft background; tooltip)
- `src/main/kotlin/common_components/AppIcons.kt` — `Upload` and `DeleteOutline` vectors (not in `material-icons-core`)
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — `createTemplate(name, codes = current selection)`, `applyTemplate(template)` (delegates to `selectLanguagesByCode`), `deleteTemplate(id)`; loads templates in `init`
- `src/main/kotlin/home_screen/HomeScreenState.kt` — `templates: List<LanguageTemplate>` state field

## State & data

- **State holder:** `HomeScreenViewModel`; `HomeScreenState.templates` is the single source of truth for the UI
- **Create reads:** `HomeScreenState.selectedLanguages` (→ their `langCode`s)
- **Apply writes:** `HomeScreenState.selectedLanguages` (replaced with template languages matched against `availableLanguages` by code)
- **Persistence:** `~/.fast-localizer/templates.json` (user home), written on every create/delete, read once in `init`
- **File format:** JSON array of `{id, name, langCodes}`

## Dependencies

- `kotlinx-serialization-json` for JSON encode/decode
- `TemplatesRepository` (Koin `single`, injected into `HomeScreenViewModel`)
- `java.util.UUID` for template ids
- Common components: `RectangleWithShadow`, `EditText`, `PillButton`, `IconActionButton`, `AppIcons`, spacers; theme colors (`MutedTextColor`)

## Consumers

- `src/main/kotlin/home_screen/HomeScreenNew.kt` — renders `TemplatesCard(state, viewModel, onMessage)` in the right control column; `onMessage` drives the shared snackbar
- `src/main/kotlin/di/SharedModule.kt` — registers `TemplatesRepository` and passes it to `HomeScreenViewModel`

## Notes

- **Apply replaces, not merges** (the old import was additive). This makes "load this template" predictable — you get exactly the template's languages.
- Apply resolves each code through `LanguageCodeResolver`, so old spellings still select their language; codes matching no supported language are dropped. Unresolvable codes are kept in the file in case a later version supports them.
- Saving twice with the same name creates two distinct templates (each gets a fresh UUID); the user can delete one.
- Delete asks for confirmation (`ConfirmDeleteDialog`) — deletion is not undoable.
- Outcomes (saved / applied / deleted) are surfaced via the shared snackbar through the `onMessage` callback, reusing `HomeScreenNew`'s `showFileLoadedSnackBar` plumbing.
