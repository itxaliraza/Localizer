# Architecture

## Layering

```
UI (Composables)
  └── ViewModel (HomeScreenViewModel)
        ├── Domain (TranslationManager, FolderExtractor, LangImportExportHelper)
        └── Data
              ├── Network (MyTranslatorRepoImpl → TranslatorApi1/2/3 → NetworkClient)
              ├── File I/O (FilesHelper, FolderExtractor)
              └── Utilities (LocalizationUtils, openDownloadsFolder)
```

## Patterns

- **MVI-ish**: `HomeScreenState` (immutable data class) is the single source of truth, exposed as `StateFlow` and updated via `_state.update {}`.
- **One-time events**: A Kotlin `Channel` (`_oneTimeUiEvents`) emits fire-once events (e.g., file-load success/failure snackbars) consumed via `receiveAsFlow()` in the Composable.
- **Flow for streaming**: `TranslationManager.translate()` returns `Flow<TranslationResult>` so the ViewModel can collect incremental progress updates.
- **Coroutines for I/O**: All file and network operations run on `Dispatchers.IO`. The translation job is stored as `Job?` to support cancellation.
- **No LiveData or Android ViewModel base class**: This is a JVM desktop app. `HomeScreenViewModel` is a plain Kotlin class; coroutine scope is created manually with `CoroutineScope(Dispatchers.IO)`.

## Conventions

- **Stateless composables**: All composables receive state and callbacks; no local mutable state except ephemeral UI flags (scroll position, dialog visibility).
- **Single ViewModel**: The entire app has one ViewModel (`HomeScreenViewModel`). `LanguagesScreen` receives it as a parameter from `HomeScreenNew`.
- **Object utilities**: `FilesHelper`, `FolderExtractor`, `LocalizationUtils` are Kotlin `object`s (singletons with no injected dependencies). They are called directly, not injected.
- **Koin factory scope**: All DI registrations are `factory` (new instance per injection point). The ViewModel is effectively a singleton because Koin is only asked for it once (at `koinInject()` call site in `HomeScreenNew`).
- **No repository interface for files**: File I/O goes through `FolderExtractor` and `FilesHelper` objects directly — no interface abstraction.
- **Translation API interface**: `TranslatorApis` interface exists so `MyTranslatorRepoImpl` can hold references to all three API impls polymorphically.

## Dependency Rules

- UI layer (`home_screen/`, `languages_screen/`, `common_components/`, `Main.kt`) depends on `domain/model/` and the ViewModel. It does **not** import from `data/`.
- `HomeScreenViewModel` depends on `TranslationManager`, `FolderExtractor`, `LangImportExportHelper`, and `AvailableLanguages`.
- `TranslationManager` depends on `MyTranslatorRepoImpl` and `FilesHelper`.
- `MyTranslatorRepoImpl` depends on the three API impls and `LocalizationUtils`.
- `data/` has no dependency on `home_screen/` or `languages_screen/`.

## Key Architectural Constraints

- **No persistence**: No database, no SharedPreferences, no disk cache. All state is in-memory and lost on close.
- **Single window, single screen**: No navigation graph. The entire app is one Compose window containing one screen.
- **AWT integration**: Window management (dragging, maximize, minimize) uses `java.awt` directly, bypassing Compose Desktop's built-in window decorations.
- **Network-only translation**: Requires internet; there is no offline fallback.
