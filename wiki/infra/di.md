# Dependency Injection

## Framework

Koin 4.0.0-RC2 with `io.insert-koin:koin-compose` integration.

## Module

`src/main/kotlin/di/SharedModule.kt`

```kotlin
val SharedModule = module {
    factory { HomeScreenViewModel(get()) }           // get() = TranslationManager
    factory { MyTranslatorRepoImpl(get(), get(), get()) } // get()×3 = Api1, Api2, Api3
    factory { TranslationManager(get()) }            // get() = MyTranslatorRepoImpl
    factory { TranslatorApi1Impl() }
    factory { TranslatorApi2Impl() }
    factory { TranslatorApi3Impl() }
}
```

## Initialization

In `src/main/kotlin/Main.kt` inside the `App()` Composable:

```kotlin
startKoin {
    modules(SharedModule)
}
```

## Dependency Graph

```
HomeScreenViewModel
  └── TranslationManager
        └── MyTranslatorRepoImpl
              ├── TranslatorApi1Impl
              ├── TranslatorApi2Impl
              └── TranslatorApi3Impl
```

## Injection Site

- `src/main/kotlin/home_screen/HomeScreenNew.kt` line ~45: `val viewModel: HomeScreenViewModel = koinInject()`
- All other classes receive dependencies via constructor injection (wired by Koin).

## Scope

All registrations are `factory` — a new instance is created on each injection. Since `koinInject()` in `HomeScreenNew` is called once (on first composition), the `HomeScreenViewModel` is effectively a singleton for the app's lifetime.

## Not Injected (direct object access)

- `FilesHelper` — Kotlin `object`, accessed directly from `TranslationManager` and `FolderExtractor`
- `FolderExtractor` — Kotlin `object`, accessed directly from `HomeScreenViewModel`
- `LocalizationUtils` — Kotlin `object`, accessed directly from `MyTranslatorRepoImpl`
- `LangImportExportHelper` — top-level functions, called directly from `HomeScreenNew`
- `NetworkClient` — Kotlin `object`, accessed directly from API impls
- `AvailableLanguages` — top-level lazy val, read directly in `HomeScreenViewModel.init`
