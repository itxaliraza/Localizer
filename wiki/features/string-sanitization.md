# String Sanitization

## What it does

Protects Android string placeholders, escape sequences, and special characters before sending text to Google Translate, then restores them after receiving the translated string. Prevents the translator from mangling `%1$s`, `\n`, `\"`, and similar tokens.

## Key files

- `src/main/kotlin/data/util/LocalizationUtils.kt` — two top-level functions:
  - `sanitizeForTranslation(text: String): String` — replaces `%1$s` → `XXPH1_sXX`, `%2$d` → `XXPH2_dXX`, etc.; escapes `\n` and `\"`; returns modified string
  - `restoreAfterTranslation(original: String, translated: String): String` — reverses placeholders back; uses flexible regex to handle spacing changes the translator may introduce

## State & data

- Stateless utility functions (no class, no state)
- Called per-string inside `MyTranslatorRepoImpl.getTranslation()`

## Dependencies

- Kotlin standard library only (regex, string replacement)

## Consumers

- `src/main/kotlin/data/translator/MyTranslatorRepoImpl.kt` — calls `sanitizeForTranslation()` before the HTTP request and `restoreAfterTranslation()` on the response string

## Notes

- The regex in `restoreAfterTranslation` is deliberately flexible (allows surrounding spaces around placeholder tokens) because some translators insert spaces around them.
- `restoreAfterTranslation` escapes real `'` → `\'` and `"` → `\"` for Android (a string resource requires these escaped). This is uniform across all three endpoints because API1 now HTML-unescapes its scraped text, so the repo receives plain `"`/`&`/`'` from every endpoint (previously API1 leaked `&quot;`, which the restore step special-cased).
- Empirically (verified by hitting the live endpoints), Google preserves `%1$s`-style placeholders untranslated even without sanitization, so sanitize/restore is primarily defensive against the cases where a translator does reorder or space them.
- `&` in translated text is left as a literal `&`; the XML write layer (`FilesHelper`) re-escapes it to `&amp;` via DOM `textContent`. Do not pre-escape entities here or they double-escape on write.
- If a placeholder regex doesn't match after translation (e.g. translator completely dropped it), the original placeholder from the source string is re-inserted at best effort.
