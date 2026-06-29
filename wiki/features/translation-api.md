# Translation API Layer

## What it does

Provides translation of individual strings from English to a target language by routing requests across three distinct Google Translate endpoints in round-robin order. If one endpoint fails, the next is tried automatically.

## Key files

- `src/main/kotlin/data/translator/MyTranslatorRepoImpl.kt` — `getTranslation(text, langCode): NetworkResponse<String>`; maintains `lastCalledIndex` for rotation; routes `onlyWebTranslate` languages to API1 only; applies `LocalizationUtils` sanitize/restore around each call
- `src/main/kotlin/data/translator/api_interface/TranslatorApis.kt` — interface with `getTranslation(text, langCode): NetworkResponse<String>`
- `src/main/kotlin/data/translator/apis/TranslatorApi1Impl.kt` — `translate.google.com/m` (mobile web, HTML scraping)
- `src/main/kotlin/data/translator/apis/TranslatorApi2Impl.kt` — `translate.google.com/translate_a/single` (JSON, `client=gtx`)
- `src/main/kotlin/data/translator/apis/TranslatorApi3Impl.kt` — `clients4.google.com/translate_a/t` (JSON, `client=dict-chrome-ex`)
- `src/main/kotlin/data/network/client/NetworkClient.kt` — Ktor CIO `HttpClient` singleton; `makeStringNetworkRequest(url, type): NetworkResponse<String>`, 90s timeout
- `src/main/kotlin/data/network/NetworkResponse.kt` — sealed class: `Success<T>`, `Failure`, `Loading`, `Idle`; extension funs `doIfSuccess {}`, `doIfSuccessOrFailure {}`
- `src/main/kotlin/data/network/client/RequestTypes.kt` — `Get` / `Post(body)`

## State & data

- All three API impls make HTTP GET requests (URL-encoded query parameter `q`)
- Response parsing differs: API1 scrapes HTML, API2/3 parse JSON arrays
- No caching — every string hits the network

## Dependencies

- `NetworkClient` (object, used by all three API impls)
- `LocalizationUtils` (used by `MyTranslatorRepoImpl` before/after each call)
- Ktor CIO engine (`io.ktor:ktor-client-cio:2.3.12`)

## Consumers

- `src/main/kotlin/data/translator/TranslationManager.kt` — calls `getTranslation()` per missing key

## Notes

- Round-robin index (`lastCalledIndex`) is a **private instance field** on `MyTranslatorRepoImpl` (previously a process-global top-level `var`). Koin registers the repo as `factory`, so the index resets to 0 per translation session. Under parallel translation the increment is a benign racy write — it only nudges which endpoint is tried first.
- A per-call `ensureActive()` runs at the top of each rotation iteration so user cancellation is observed promptly.
- If `LanguageModel.onlyWebTranslate == true`, only API1 (mobile web) is attempted; APIs 2 and 3 are skipped for that language.
- Fallback order when an API fails: rotate to the next index and try the next endpoint. If all endpoints fail for a key, the failure surfaces to `TranslationManager`, which skips that key (per-key tolerance) rather than failing the run.
- **API1 scraping:** `getTranslationData` searches for `class="result-container">` (current Google markup) then `class="t0">` (legacy) explicitly, and HTML-unescapes the extracted text (`&quot;`, `&amp;`, etc.). Previously it depended on a byte-offset coincidence in `<!DOCTYPE html>` and left entities escaped (which double-escaped `&` on write).
- String sanitization/restoration (`LocalizationUtils`) happens in this layer, not in `TranslationManager`, so the repo always receives raw English text and returns raw translated text. Because API1 now unescapes HTML entities, all three endpoints return the same plain-text shape; `restoreAfterTranslation` escapes real `'` and `"` for Android.
