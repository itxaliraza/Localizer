# Dependency Injection

## Framework

Koin 4.2.2 with `io.insert-koin:koin-compose` integration.

## Module

`src/main/kotlin/di/SharedModule.kt`

```kotlin
val SharedModule = module {
    factory { HomeScreenViewModel(get(), get()) }    // get()×2 = TranslationManager, TemplatesRepository
    single  { TemplatesRepository() }                // persists language templates to ~/.fast-localizer/templates.json
    factory { AboutViewModel() }                     // About dialog; injected once in CustomTitleBar (Main.kt)
    factory<TranslationRepository> { MyTranslatorRepoImpl(get<TranslatorApi1Impl>(), get<TranslatorApi2Impl>(), get<TranslatorApi3Impl>()) } // explicit types: MyTranslatorRepoImpl takes the TranslatorApis interface
    factory { TranslationManager(get()) }            // get() = TranslationRepository
    factory { TranslatorApi1Impl() }
    factory { TranslatorApi2Impl() }
    factory { TranslatorApi3Impl() }
}
```

## Initialization

In `src/main/kotlin/Main.kt` inside `main()`, **before** `application { … }` — i.e. exactly once, outside composition:

```kotlin
fun main() {
    startKoin { modules(SharedModule) }
    application { Window(…) { App(window) { exitApplication() } } }
}
```

(It used to be inside the `App()` composable, where any recomposition of `App` would have called `startKoin` again and thrown "Koin already started".)

## Dependency Graph

```
HomeScreenViewModel
  ├── TranslationManager
  │     └── TranslationRepository  (bound to MyTranslatorRepoImpl)
  │           └── MyTranslatorRepoImpl
  │           ├── TranslatorApi1Impl
  │           ├── TranslatorApi2Impl
  │           └── TranslatorApi3Impl
  └── TemplatesRepository  (single)
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
- `NetworkClient` — Kotlin `object`, accessed directly from API impls
- `AvailableLanguages` — top-level lazy val, read directly in `HomeScreenViewModel.init`
