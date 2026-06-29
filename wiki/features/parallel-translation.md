# Parallel Translation

## What it does

Controls whether missing string keys within each language are translated concurrently (faster, higher network load) or sequentially (slower, gentler). Toggled via `HomeScreenState.parallelTranslation`.

## Key files

- `src/main/kotlin/data/translator/TranslationManager.kt` — checks `parallelTranslation` flag inside `processTranslation()`; if true, wraps each key translation in `async {}` and calls `awaitAll()` then `filterNotNull()`; if false, uses a sequential `mapNotNull {}` with suspension. Either path goes through `translateKeyOrNull()`, which is bounded by a shared `Semaphore(8)` so even parallel mode never exceeds 8 concurrent requests.
- `src/main/kotlin/home_screen/HomeScreenState.kt` — `parallelTranslation: Boolean = true` (default on)
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — `toggleParallel(checked: Boolean)`: updates state flag

## State & data

- **State field:** `HomeScreenState.parallelTranslation: Boolean`
- **Default:** `true`
- **Note:** the toggle UI is defined in `HomeScreenViewModel` but not currently wired to a visible UI control in `HomeScreenNew.kt`

## Dependencies

- Kotlin coroutines `async` / `awaitAll` (stdlib)

## Consumers

- `src/main/kotlin/data/translator/TranslationManager.kt` — sole consumer of the flag at runtime
- `src/main/kotlin/home_screen/HomeScreenViewModel.kt` — `toggleParallel()` method exists but is not called from UI currently

## Notes

- Parallelism is per-language's missing keys, not per-language. Languages themselves are always processed sequentially (one at a time) so progress percentage increments cleanly.
- Even in parallel mode the `Semaphore(8)` concurrency cap and per-key retry/backoff make rate-limiting far less likely than the previous unbounded fan-out, so `false` is rarely needed for that reason now.
- Setting this to `false` is useful for reproducing deterministic output or step-debugging a single request at a time.
