# XML Parsing & Writing

## What it does

Reads `strings.xml` files using the Java DOM API, extracts translatable key-value pairs, and writes new or updated `strings.xml` files back to disk. Handles non-translatable string filtering, UTF-8 encoding, indentation, and directory creation.

## Key files

- `src/main/kotlin/data/FilesHelper.kt` — all XML logic:
  - `parseXml(file: File): FileXmlData` — DOM parse; extracts `<string name="key">value</string>` pairs; filters out `translatable="false"` entries
  - `getFilesXmlContents(files: List<File>): List<FileXmlData>` — batch parse wrapper
  - `extractLanguageCode(folderName: String): Pair<String,String>` — regex to strip `values-` prefix; returns `rawCode to standardizedCode`. Normalizes Android qualifier forms via `fromAndroidResFolderCode`, applies legacy/Google remaps (`in`→`id`, `he`→`iw`, `zh`→`zh-CN`, …), then resolves to the exact `availableLanguages` code via `resolveAvailableCode`
  - `fromAndroidResFolderCode(code: String): String` — inverse of `toAndroidResFolderCode`: Android qualifier → locale code (`pt-rBR`→`pt-BR`, `zh-rCN`→`zh-CN`, `b+ms+Arab`→`ms-Arab`). Idempotent for plain codes
  - `resolveAvailableCode(code: String): String` (private) — matches a locale code against `availableLanguages` (case-insensitive exact, then base-language fallback so `pt-BR`→`pt`, `es-MX`→`es`)
  - `toAndroidResFolderCode(code: String): String` — converts a Google/Locale code into a valid Android resource qualifier on write (`pt-BR`→`pt-rBR`, `zh-CN`→`zh-rCN`; scripts/numeric regions like `ms-Arab`/`es-419`→`b+ms+Arab`). Idempotent (`zh-rCN`, `b+zh+CN` pass through unchanged)
  - `mergeEntriesIntoXml(existingXml: String, newEntries: Map<String, String>): String` — parses the existing target file, appends only `<string>` entries whose `name` is not already present, strips whitespace-only text nodes, and re-serializes with UTF-8 + indentation. **Preserves** existing strings, `<string-array>`, `<plurals>`, comments and `translatable="false"` strings. (Replaced `addNewEntriesToXmlNew`, which rebuilt from scratch and dropped all of those.)
  - `writeXmlToFile(xmlString: String, file: File)` — creates parent dirs if needed, writes UTF-8 file
  - `combineStringsWithLimit(list, limit)` — utility for chunking strings (used in batch requests if applicable)
  - `makeZipFile(...)` — present but unused in current flow

## State & data

- `FileXmlData` (data class in `FilesHelper.kt`): `contents: String`, `keyValuePairs: Map<String, String>`, `languageCode: String`
- Reads from: file system paths in `ExtractionResult.extractedFiles`
- Writes to: `<outputDir>/values-<lang>/strings.xml`; directories created if missing. The `<lang>` qualifier is sanitized via `toAndroidResFolderCode` so region-qualified locales produce Android-valid folders (`values-pt-rBR`, not `values-pt-BR`)

## Dependencies

- Java standard library: `javax.xml.parsers.DocumentBuilderFactory`, `javax.xml.transform.TransformerFactory`
- No third-party XML library

## Consumers

- `src/main/kotlin/data/util/FolderExtractor.kt` — calls `parseXml()` and `extractLanguageCode()`
- `src/main/kotlin/data/translator/TranslationManager.kt` — calls `getFilesXmlContents()`, `mergeEntriesIntoXml()`, `writeXmlToFile()`

## Notes

- DOM parsing means the entire XML is loaded into memory; acceptable for `strings.xml` files which are typically small (< 1 MB).
- `mergeEntriesIntoXml` merges into the existing DOM so non-`<string>` content survives a re-run; whitespace-only text nodes are removed before re-indenting to avoid ragged output. Duplicate `name`s are never appended (it checks all existing `<string>` names first).
- XML output encoding is always UTF-8 with `<?xml version="1.0" encoding="utf-8"?>` header.
- `translatable="false"` strings are skipped at parse time and never sent to the translation API, but they are **retained** in the written file because merging preserves the original DOM nodes.
- `parseXml` decodes the source with explicit UTF-8 (`toByteArray(StandardCharsets.UTF_8)`).
