# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Fast Localizer** — Kotlin/JVM desktop app (Compose Desktop, Windows EXE) that auto-translates Android `strings.xml` files via Google Translate. **Not an Android APK project** — there is no `com.android.*` plugin; `local.properties` is leftover scaffolding. Single Gradle module, source root is `src/main/kotlin` (no `app/` submodule). See [wiki/infra/build.md](wiki/infra/build.md).

## Commands

```bash
./gradlew run                 # launch the desktop app
./gradlew compileKotlin       # fastest compile check (no linter is configured)
./gradlew test                # JUnit 5 unit tests in src/test/kotlin (offline)
./gradlew test --tests "*FilesHelperTest*"          # single test class
./gradlew packageExe          # Windows EXE installer (build/compose/binaries/)
./gradlew packageReleaseExe   # release EXE
```

- Releases: pushing a tag like `10.0.0` triggers `.github/workflows/release.yml`, which runs tests, builds the EXE with `-PappVersion=<tag>` and attaches it to the GitHub Release.

- Unit tests make no internet calls: they use fakes of `TranslatorApis` / `TranslationRepository` and a local `HttpServer`. `LiveLanguageSweepTest` is skipped unless `LIVE_SWEEP=1`. It sends every language to the real Google endpoints (about 500 requests, and it can trigger rate limiting), so only run it on purpose.
- JUnit 5 silently ignores test methods that don't return `Unit`. With a `= runBlocking { … }` body, make sure the last expression is `Unit`.
- `gradlew.bat` forces `TEMP=TMP=C:\tmp` to work around a JDK 21 AF_UNIX bug with spaces in the temp path. On Windows, prefer `gradlew.bat` (or keep that fix) — don't remove it.
- Plugin versions live only in `gradle.properties` (`kotlin.version`, `compose.version`) and are applied through `settings.gradle.kts` `pluginManagement`. `build.gradle.kts` declares its plugins without versions. The installer version is `packageVersion` in `build.gradle.kts`.

## Architecture (big picture)

Full detail in [wiki/architecture.md](wiki/architecture.md). The essentials:

- **One window, one screen.** `Main.kt` builds an undecorated AWT-driven window hosting `HomeScreenNew`; `LanguagesScreen` is a left panel that receives the same `HomeScreenViewModel`. No navigation graph; everything else is a Compose `Dialog`. The only other ViewModel is `AboutViewModel` (title-bar info icon → About dialog + GitHub update check). The app version comes from the generated `buildinfo.BuildInfo.VERSION`.
- **State:** `HomeScreenViewModel` (plain Kotlin class, manual `CoroutineScope(Dispatchers.IO)`) exposes an immutable `HomeScreenState` via `StateFlow`; fire-once UI events go through a `Channel`. Composables are stateless.
- **Pipeline:** `FolderExtractor` scans a `res/` folder or a whole project root (discovering every module with translatable strings) → `TranslationManager.translate()` returns a `channelFlow<TranslationResult>` that loops modules × languages, computes missing keys (`englishKeys - languageKeys`), translates them, and merges results into the existing target `strings.xml` via `FilesHelper.mergeEntriesIntoXml` (preserves arrays/plurals/comments/`translatable="false"`).
- **Translation network layer:** `TranslationManager` depends on the `TranslationRepository` interface, which Koin binds to `MyTranslatorRepoImpl`. That class rotates between the two JSON Google endpoints (`TranslatorApi2/3Impl`) and falls back to the HTML scraper (`TranslatorApi1Impl`) last, never as part of the rotation. An endpoint that fails is put on a cool-down that doubles with each consecutive failure. `LocalizationUtils` protects placeholders and escapes: text is sanitized once before the request, and every response is checked when they are restored. A semaphore caps requests at 8 in flight, each key gets a few retries, and a key that fails on every endpoint is skipped instead of aborting the run.
- **Language codes:** `LanguageCodeResolver` maps any spelling (`id`↔`in`, `itg`→`ltg`, `pt`→`pt-PT`, `cmn-Hans`→`zh-CN`, …) to the code in `AvailableLanguages.kt`; folder detection, list import and saved templates all go through it, so never compare raw codes directly. Each existing language folder is merged and written back to itself, even when two folders resolve to one language (`values-in` + `values-id`), and new ones are converted to valid Android qualifiers (`pt-BR` → `values-pt-rBR`) by `FilesHelper.toAndroidResFolderCode`.
- **DI:** Koin, single module in `di/SharedModule.kt`. Mostly `factory`; `TemplatesRepository` is `single`. `FilesHelper`, `FolderExtractor`, `LocalizationUtils` are Kotlin `object`s called directly, not injected.
- **Persistence:** only language templates, stored at `~/.fast-localizer/templates.json`. Everything else is in-memory.
- **Layering rule:** UI packages (`home_screen/`, `languages_screen/`, `about_screen/`, `common_components/`) must not import from `data/`; they depend on `domain/model/` and the ViewModel.

## Wiki

Feature knowledge base lives in [wiki/index.md](wiki/index.md).

**MUST** read `wiki/index.md` before reading source, and update every affected wiki page (+ add an entry to `wiki/log.md`) in the same change as any code edit.

Full maintenance protocol:

**MUST — Before touching or reading any source:**
1. Read `wiki/index.md` first.
2. Open the linked wiki page for EVERY area you will touch (every affected screen/feature page + relevant `wiki/infra/*.md`).
3. Only open source files after orienting in those wiki pages.
4. Trust the wiki first. Do NOT re-explore the codebase to confirm what a wiki page already states. Open source only when the wiki is missing, stale, or silent on the question — and if you find it stale, update it before continuing.

**MUST — After EVERY code change:**
1. Update EVERY wiki page covering a file you changed (screen/feature pages, `infra/navigation.md`, `infra/di.md`, etc.). Keep **Key files** and **Consumers** accurate.
2. Add one entry to `wiki/log.md`: date, what changed, files touched. Entries are **newest-first** — insert directly below the `---` header separator, format `## YYYY-MM-DD — <summary>`.
3. If you added a screen/feature, create its `wiki/<screens|features>/<name>.md` from the template AND add its one-line entry to `wiki/index.md` in the SAME change — no exceptions.
4. If you removed a screen/feature, delete/archive its wiki page and remove its line from `wiki/index.md` in the same change.

**MUST — Coverage invariant:** every screen/view directory in the project has a corresponding page in `wiki/screens/` linked from `wiki/index.md`. A new screen without a wiki page in the same change is a hard error — treat it like a failing build.

**MUST — Before removing any feature:** read that feature's wiki page first. Its **Consumers** section is the authoritative list of files to change.

### Wiki page template (screen or feature)

```
# <Name>

## What it does
<1–3 sentences, user-facing purpose>

## Key files
- `path/to/File` — <role in one line>

## State & data
<state holder, data sources read/written>

## Dependencies
<what it injects/needs>

## Consumers
<every file that references this — exhaustive>

## Notes
<gotchas, edge cases, non-obvious decisions>
```
