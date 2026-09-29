# Language List Import

## What it does
Lets users bring in a language list they already have (a file or pasted text) from the **Import** pill in the Language Templates card. Only language codes and names are picked out; everything else in the list is ignored. A live preview shows what was found, lets the user drop individual languages, then **Select these** (replace the current selection) or **Save as template**.

## Key files
- `src/main/kotlin/data/util/LanguageListParser.kt` — `parse(text, languages): LanguageImportResult`; tokenizes the text and resolves each entry to an available language
- `src/main/kotlin/data/util/LanguageCodeResolver.kt` — shared code resolution (also used by folder detection and templates)
- `src/main/kotlin/domain/model/LanguageImportResult.kt` — `LanguageImportResult(matched: List<LanguageModel>, unrecognized: List<String>)`
- `src/main/kotlin/home_screen/components/ImportLanguagesDialog.kt` — the dialog: Choose file… (AWT `FileDialog`), paste area, live preview chips (click to exclude), "Not recognised" line, template name, Cancel / Select these / Save as template
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — `parseLanguageList(text)`, `readLanguageListFile(path)` (≤ 512 KB, else null), `selectLanguagesByCode(codes)`, `createTemplate(name, codes)`
- `src/main/kotlin/home_screen/components/TemplatesCard.kt` — Import pill and dialog wiring; success messages via `onMessage`

## State & data
- **Dialog-local state:** `text`, `fileName`, `fileError`, `name`, `excluded` (codes the user clicked off); the parse result is `remember(text)`, recomputed on every edit
- **Writes:** `HomeScreenState.selectedLanguages` (Select these) or `HomeScreenState.templates` + `~/.fast-localizer/templates.json` (Save as template)
- **Reads:** the chosen file (UTF-8 text)

## Dependencies
- `availableLanguages` (default for `parse(text, languages)`; tests pass their own list), `FilesHelper.fromAndroidResFolderCode`
- Common components: `PillButton`, `PillStyle`, `AppIcons.Upload`, `EditText`, spacers

## Consumers
- `src/main/kotlin/home_screen/components/TemplatesCard.kt` — only entry point
- `src/test/kotlin/data/util/LanguageListParserTest.kt` — parser tests
- `src/test/resources/import/kotlin_language_list.txt` — fixture: rows of a real app's Kotlin `LanguageModel` list

## Notes
- **Dialog layout:** only the title and the action row are fixed; everything between them is in a `weight(1f, fill = false)` + `verticalScroll` column, so a long preview can't push Cancel / Select these / Save as template off a small window.
- **Parsing:** the text is split into *cells* on newlines, `, ; tab | " ' `` ` `` [ ] { }` (not parentheses, so "Chinese (Simplified)" stays whole). A cell is matched as a whole first: by English or native name (case-insensitive), then as a code. If that fails and the cell has ≤ 4 words, each word (split on whitespace `( ) < > = / :`) is tried; longer cells are treated as prose and skipped, since short words like "is", "it", "no", "hi" are also language codes.
- **Code resolution** does not depend on which spelling the app's list uses. After stripping `values-` / `/strings.xml`, turning `_` into `-` and undoing Android qualifiers (`pt-rBR`, `b+ms+Arab`) via `FilesHelper.fromAndroidResFolderCode`, `LanguageCodeResolver.resolve` tries in order: the exact code; an **equivalent** code (`id`↔`in`, `he`↔`iw`, `yi`↔`ji`, `jv`↔`jw`, `fil`↔`tl`, `nb`↔`no`, `ltg`↔`itg`, `zh`/`zh-CN`/`zh-Hans`/`cmn`/`cmn-Hans`, `zh-TW`/`zh-Hant`/`cmn-Hant`); then the only **regional variant** the list has (`pt` → `pt-PT` when there is no plain `pt`). If all fail, subtags are dropped from the end one at a time and retried (`cmn-Hans-CN` → `cmn-Hans` → `cmn`; `en-US` → `en`; `pt-BR` → `pt` or `pt-PT`). The same resolver backs folder detection (`FilesHelper.extractLanguageCode`) and templates, so all three agree.
- `jv` is Javanese (Google Translate calls it `jw`), not Japanese (`ja`).
- Latgalian is `ltg` (ISO 639-3 and Google Translate). Older versions used the typo `itg`, which Google echoed back untranslated; the `ltg`↔`itg` equivalence keeps old lists, folders and templates working.
- The paste box has a **Clear** link (shown when it has text) that empties it and resets exclusions; Ctrl+A / drag-select also work since it's a normal `OutlinedTextField`.
- JSON object keys (`"id":`) are stripped before parsing; otherwise `id` would match Indonesian. JSON values that happen to be codes (e.g. a template named "EU") can still match; the preview lets the user click them off.
- **Unrecognized** lists single-entry cells that look like codes (`xx`, `qq-ZZ`, also `Yes`) but match nothing, so the user can see what was dropped. Non-code words (headers like "Language") are silently ignored.
- Spreadsheet files (`.xlsx/.xls/.ods/.numbers`) are rejected with a hint to save as CSV; they're binary.
- Output order is first-seen with duplicates removed.
