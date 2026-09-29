# XML Parsing & Writing

## What it does

Reads `strings.xml` files using the Java DOM API, extracts translatable entries — `<string>`, `<string-array>` items and `<plurals>` items — with their inner markup intact, and merges translations back into target files. Handles non-translatable filtering, UTF-8, deterministic indentation, atomic writes and directory creation.

## Key files

- `src/main/kotlin/data/FilesHelper.kt` — all XML logic:
  - `parseXml(xmlContent: String): Map<String,String>` — DOM parse in document order. Keys: `name` for strings, `array:<name>:<index>` (`arrayItemKey`) for string-array items, `plurals:<name>:<quantity>` (`pluralItemKey`) for plurals items. **Value = the element's inner XML** (text with `&amp;/&lt;/&gt;` escaped, child markup like `<b>`/`<xliff:g>`/CDATA kept) — not `textContent`, which flattened markup. Skips any element with `translatable="false"`.
  - `groupOf(key): String?` — `array:<name>` / `plurals:<name>` for array/plurals keys, `null` for plain strings; used for all-or-nothing translation of a group
  - `isLanguageFolder(folderName): Boolean` — true for `values` and `values-<language>` folders the app supports (`values-fr`, `values-pt-rBR`, `values-b+ms+Arab`); false for configuration qualifiers (`values-night`, `values-v29`, `values-sw600dp`, `values-fr-night`, …) and for `values-en*` (must not shadow base `values`). Regex-checks the qualifier shape, then requires the resolved code to be in `availableLanguages`
  - `EMPTY_STRINGS_XML` — in-memory stand-in for a language folder with no `strings.xml`
  - `getFilesXmlContents(files: List<File>): List<FileXmlData>` — batch parse wrapper
  - `extractLanguageCode(folderName: String): Pair<String,String>` — regex to strip `values-` prefix; returns `rawCode to standardizedCode`. Normalizes Android qualifier forms via `fromAndroidResFolderCode`, then resolves any spelling to the `availableLanguages` code via `LanguageCodeResolver.resolve` (returns the locale code unchanged if nothing matches)
  - `fromAndroidResFolderCode(code: String): String` — inverse of `toAndroidResFolderCode`: Android qualifier → locale code (`pt-rBR`→`pt-BR`, `zh-rCN`→`zh-CN`, `b+ms+Arab`→`ms-Arab`). Idempotent for plain codes
  - `toAndroidResFolderCode(code: String): String` — converts a Google/Locale code into a valid Android resource qualifier on write (`pt-BR`→`pt-rBR`, `zh-CN`→`zh-rCN`; scripts/numeric regions like `ms-Arab`/`es-419`→`b+ms+Arab`). Idempotent (`zh-rCN`, `b+zh+CN` pass through unchanged)
  - `mergeEntriesIntoXml(existingXml: String, newEntries: Map<String, String>): String` — parses the existing target file and adds only what is missing: a `<string>` whose name is absent; a `<string-array>` item beyond the array's current length (new arrays are created whole; items are appended **only contiguously** — a gap would shift positions; an array left empty is removed); a `<plurals>` `<item>` whose quantity is absent. Values are inserted as real child nodes (markup survives); adds `xmlns:xliff` to the root if a value uses `<xliff:…>`. **Preserves** existing strings, arrays, plurals, comments and `translatable="false"` strings. A not-well-formed value becomes a last-resort literal text node. Throws `IllegalArgumentException` if the existing file is malformed.
  - `serializeResources(doc)` (private) — deterministic pretty printer: `<?xml version="1.0" encoding="utf-8"?>`, 4-space indent, one entry per line; `<string>`/`<item>` always inline so whitespace inside them is never altered. Replaced the JDK `Transformer`, whose `INDENT` corrupts whitespace in mixed-content strings and emitted `standalone="no"`
  - `writeXmlToFile(xmlString: String, filePath: String)` — creates parent dirs, writes UTF-8 to `strings.xml.tmp` then atomically moves it into place (falls back to a plain replace-move). **Throws** on failure (previously swallowed — a failed write looked like success)
  - `combineStringsWithLimit(list, limit)` — utility for chunking strings (used in batch requests if applicable)
  - `makeZipFile(...)` — present but unused in current flow

## State & data

- `FileXmlData` (data class in `FilesHelper.kt`): `contents: String`, `keyValuePairs: Map<String, String>`, `languageCode: String`
- Reads from: file system paths in `ExtractionResult.extractedFiles`
- Writes to: `<outputDir>/values-<lang>/strings.xml`; directories created if missing (only when a translation is actually written — never on load). The `<lang>` qualifier is sanitized via `toAndroidResFolderCode` so region-qualified locales produce Android-valid folders (`values-pt-rBR`, not `values-pt-BR`)

## Dependencies

- Java standard library: `javax.xml.parsers.DocumentBuilderFactory`, `org.w3c.dom` (no `Transformer` any more)
- No third-party XML library

## Consumers

- `src/main/kotlin/data/util/FolderExtractor.kt` — calls `parseXml()`, `extractLanguageCode()`, `isLanguageFolder()`, `EMPTY_STRINGS_XML`
- `src/test/kotlin/data/FilesHelperTest.kt` — parse/merge/write/folder-filter coverage
- `src/main/kotlin/data/translator/TranslationManager.kt` — calls `getFilesXmlContents()`, `groupOf()`, `mergeEntriesIntoXml()`, `writeXmlToFile()`

## Notes

- DOM parsing means the entire XML is loaded into memory; acceptable for `strings.xml` files which are typically small (< 1 MB).
- `mergeEntriesIntoXml` merges into the existing DOM so non-`<string>` content survives a re-run; whitespace-only text nodes are removed before re-indenting to avoid ragged output. Duplicate `name`s are never appended (it checks all existing `<string>` names first).
- XML output encoding is always UTF-8 with `<?xml version="1.0" encoding="utf-8"?>` header.
- `translatable="false"` entries are skipped at parse time and never sent to the translation API, but they are **retained** in the written file because merging preserves the original DOM nodes.
- `parseXml` decodes the source with explicit UTF-8 (`toByteArray(StandardCharsets.UTF_8)`).
- The JDK DOM keeps attributes **sorted by name**, so a re-serialized `<xliff:g id=".." example="..">` comes back as `example, id`. Semantically identical; only visible as a diff in files the tool rewrites.
- Only whitespace between structural elements (`resources`, `string-array`, `plurals`) is stripped/regenerated; whitespace inside `<string>` is content and is preserved (a `<string> </string>` survives).
- **Plurals limitation:** base quantities are translated as-is (`one`/`other`); languages needing more (`ru`: few/many, `ar`: zero/two/few/many) or fewer (`ja`) are not adapted. Android falls back to `other`; Lint may warn.
- `parseXml` is not hardened against XXE (unchanged; still open).
