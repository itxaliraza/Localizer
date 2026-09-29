# String Sanitization

## What it does

Protects everything in a `strings.xml` value that the translator must not touch — printf placeholders, `{name}` placeholders, inline markup (`<b>`, `<xliff:g>`, CDATA markers) — by swapping each for a numbered, **letter-free** token before the HTTP call, then restores them afterwards. It also **validates** the result: if a token was lost, duplicated, invented, or reordering made the markup malformed, the translation is rejected (returns `null`) so a damaged string is never written. Tokens come in several *styles*; a damaged result is retried with the next style (a ladder).

## Key files

- `src/main/kotlin/data/util/LocalizationUtils.kt` — `object LocalizationUtils`:
  - `enum TokenStyle { AT "@n@", PERCENT "%n%", BRACKETS "[[n]]", MATH "⟦n⟧" }` — the ladder, in the order tried. `render(index)` writes a token; `regex` reads it back (tolerates spaces and digits in **any script**: `@٠@`, `@०@`).
  - `sanitizeForTranslation(value, style = AT): SanitizedText(text, tokens, translatable, style)` — value is **inner XML** (see [xml-parsing-writing.md](xml-parsing-writing.md)). Splits out tags/CDATA/comments (`markupRegex`) and format placeholders (`placeholderRegex`: `%s %d %1$s %.2f %02d %-10s %%`, `{name}`/`{0}`), replacing each with a token. Text between tokens is un-escaped for the translator (`&amp;`→`&`, `\'`→`'`, `\"`→`"`, `\@`→`@`, `\?`→`?`).
  - `restoreAfterTranslation(translated, sanitized): String?` — puts the original tokens back (using `sanitized.style`), re-escapes text (`&`→`&amp;`, `<`→`&lt;`, `'`→`\'`, `"`→`\"`), returns `null` on any token mismatch or malformed markup.
  - `hasTranslatableText(value): Boolean` — false for values with nothing to translate.
  - `Token(original, padBefore, padAfter)`, `SanitizedText` — data classes.

## State & data

- Stateless. The token list and style travel in `SanitizedText` from sanitize to restore.

## Dependencies

- Kotlin stdlib regex; `javax.xml.parsers` (well-formedness check only).

## Consumers

- `src/main/kotlin/data/translator/MyTranslatorRepoImpl.kt` — walks `TokenStyle.entries`: sanitize with a style, send, restore; `null` ⇒ damaged ⇒ next style.
- `src/main/kotlin/data/translator/TranslationManager.kt` — `hasTranslatableText` for the "unchanged output" check.
- Tests: `src/test/kotlin/data/util/LocalizationUtilsTest.kt`; live: `src/test/kotlin/data/translator/LiveLanguageSweepTest.kt`.

## Notes

- **Why letter-free tokens (the Serbian/Icelandic bug).** The first design used `XXPH1_dXX`, then `XXPH1XX`. Google **transliterates or truncates letters** per language: Serbian returned `КСКСПХ1_дКСКС`, Icelandic `XXH2_dXX` (a `P` lost), and Hindi/Amharic also broke `XXPH`. Digits and symbols are not transliterated. Measured on 21 hard-script languages, `XXPH` failed 4 (sr, is, hi, am); `@n@`, `%n%`, `[[n]]`, `<n>`, `⟦n⟧` all passed 21/21.
- **Why a ladder, not one "best" style.** No single style survives every language, because the damage is Google *deleting a token that sits next to a word it drops* (Tatar/Uyghur/Odia/Gujarati/Kyrgyz drop "now"), and which token goes depends on the style. Measured over the 126 languages served by the JSON endpoints: `[[n]]` alone failed 6 languages, `@n@` alone failed 1 (tt), `%n%` alone failed 1 (ug). The endpoints return identical damage for the same input, so a damaged result is retried with the **next style on the same endpoint**, not the next endpoint. With the ladder all 126 pass; `or` and `tt` needed `%n%`, and `ug` needed the last rung (`⟦n⟧`) for one string.
- **Verified for every language (live, real code path, 2026-09-20): 242/242 pass** on two strings (`%1$d`/`%2$d` sentence; `<b>…</b>` + `%1$s` + "now, please") with each placeholder/tag returned exactly once. 126 were JSON-endpoint languages and 116 were `onlyWebTranslate` languages, which now also go through the JSON endpoints (see [translation-api.md](translation-api.md)). 7 needed a fallback style: `dz`, `new`, `or`, `ty`, `tt`, `sah` needed `%n%`; `ug` needed all four (last rung `⟦n⟧`). Re-run: `LIVE_SWEEP=1 ./gradlew test --tests "*LiveLanguageSweepTest*"` (~2.5 min); report `build/reports/live-language-sweep.txt`. The sweep checks placeholder/markup integrity, **not translation quality** (e.g. `itg` returns the source text unchanged and still "passes" here; the manager's unchanged-output check handles that when translating real files). The HTML scraper's handling of `⟦ ⟧` is still unchecked (`Api1Impl` unescapes only `&lt; &gt; &quot; &#39; &#x27; &amp;`) — it is only a fallback now.
- **Not translatable ⇒ returned verbatim, no network call:** only tokens/digits/punctuation, or a resource reference (`@string/x`, `?attr/x`, `@android:string/ok`). Prevents `@array/other` etc. being "translated" into a broken reference.
- **Token padding:** tokens glued to words (`Click<b>here</b>`) would be read by the translator as one word and left untranslated, so a space is added beside such tokens when sending (`padBefore`/`padAfter`) and removed again on restore. Tokens that already had a space next to them are untouched, so spacing round-trips exactly.
- **Reordering:** Ja/Ko/Hi often move a token. Plain placeholders may move freely; but if a closing tag lands before its opening tag the result isn't well-formed XML and is rejected (`isWellFormedFragment`), triggering the next style.
- **Known limit — inline tag placement is best-effort.** Tokens survive but the translator may move words out of a tag (e.g. `<i>terms</i>` → `<i/>` plus loose text). The XML is valid and the text is translated; emphasis may be lost or shifted. Not rejected on purpose: an untranslated string is worse than a translated one with lost italics.
- **Cost of a stubborn string:** up to 4 styles per attempt, and `TranslationManager` retries a failed key 3×, so a string no style can carry is tried ~12 times before being skipped and reported. Styles are only advanced when an endpoint *answered* with a damaged result; if no endpoint answered (down/blocked) the repo gives up at once (see [translation-api.md](translation-api.md)).
- `\n` (backslash-n) gets a space after it when sending so it isn't glued to the next word; a translator-produced `\ n` is fixed back to `\n` on restore.
- A restored value starting with `@` or `?` is prefixed with `\` (Android would otherwise read it as a resource reference).
- `&` etc. are escaped **here** (values are inner XML) and inserted as real XML by `FilesHelper.mergeEntriesIntoXml`; do not escape again downstream.
