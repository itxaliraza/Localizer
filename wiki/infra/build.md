# Build

## Type

Kotlin/JVM desktop application. **Not an Android APK.** Builds a Windows EXE installer via the Compose Desktop Gradle plugin.

## Key Build Files

- `build.gradle.kts` — single-module build config (no `app/` submodule; source root is `src/`)
- `settings.gradle.kts` — project name only
- `gradle.properties` — single source of truth for plugin versions: `kotlin.version=2.4.20`, `compose.version=1.9.3`. `settings.gradle.kts` `pluginManagement` applies them to the Kotlin JVM, Compose, Compose Compiler and Kotlin Serialization plugins; `build.gradle.kts` declares those plugins **without** versions
- `gradle/wrapper/gradle-wrapper.properties` — Gradle 9.7.1
- `gradlew.bat` — includes the `TEMP=C:\tmp` / `TMP=C:\tmp` fix for the JDK21 AF_UNIX spaces bug on this machine

## Plugin Versions

| Plugin | Version |
|--------|---------|
| Kotlin JVM | 2.4.20 |
| Compose Desktop | 1.9.3 |
| Kotlin Compose Compiler | 2.4.20 (`kotlin.plugin.compose`, follows `kotlin.version`) |
| Kotlin Serialization | 2.4.20 (follows `kotlin.version`) |

## Key Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `compose.desktop.currentOs` | — | Compose Desktop for current OS |
| `io.ktor:ktor-client-cio` | 3.6.0 | HTTP client (CIO engine) |
| `io.ktor:ktor-client-content-negotiation` | 3.6.0 | JSON content negotiation |
| `io.ktor:ktor-serialization-kotlinx-json` | 3.6.0 | Ktor + kotlinx.serialization bridge |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.11.0 | JSON serialization |
| `io.insert-koin:koin-compose` | 4.2.2 | Koin DI with Compose integration |
| `compose.material3` | — | Material 3 Composables |
| `org.jetbrains.compose.material:material-icons-core` | 1.7.3 | `Icons.Default.Info/Add/Delete`. Compose 1.8+ no longer bundles icons; 1.7.3 is the final release of this artifact |
| `compose.components.resources` | — | Compose Multiplatform resource loading |
| `org.json:json` | 20260814 | JSON parsing for API responses |
| `com.github.junrar:junrar` | 8.1.1 | RAR archive extraction (not used in main flow) |
| `org.slf4j:slf4j-api` | 2.0.19 | Logging facade |
| `ch.qos.logback:logback-classic` | 1.6.3 | Logging implementation |

## Build Targets

| Task | Output |
|------|--------|
| `./gradlew run` | Run app directly from Gradle |
| `./gradlew package` | Package as native distribution |
| `./gradlew test` | Run the unit tests (JUnit 5 via `kotlin("test")`) |
| `./gradlew packageExe` | Build Windows EXE installer (`build/compose/binaries/main/exe/Fast Localizer-<version>.exe`; the Compose plugin downloads WiX itself) |
| `./gradlew createDistributable` | Build the app image (no installer) — `build/compose/binaries/main/app/Fast Localizer/` |
| `./gradlew suggestRuntimeModules` | Ask Compose which JDK modules the dependencies reference |
| `./gradlew packageReleaseExe` | Build release EXE |

## Distribution Config

```kotlin
compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            modules("java.instrument", "java.management", "jdk.unsupported")
            packageName = "Fast Localizer"
            packageVersion = "5.0.0"
            windows {
                perUserInstall = true
                shortcut = true
                menuGroup = "Fast Localizer"
            }
        }
    }
}
```

## Tests

`src/test/kotlin/` — 92 JUnit 5 tests, no internet: `LocalizationUtilsTest`, `FilesHelperTest`, `FolderExtractorTest` (temp dirs), `MyTranslatorRepoImplTest` (fake `TranslatorApis`), `TranslationManagerTest` (fake `TranslationRepository`), `NetworkClientTest` (local `HttpServer` on 127.0.0.1). Plus `LiveLanguageSweepTest` — **skipped by default**; `LIVE_SWEEP=1 ./gradlew test --tests "*LiveLanguageSweepTest*"` sends every language in the app through the real sanitizer and Google endpoints (~2.5 min, ~500 requests, can trigger Google rate-limiting) and writes `build/reports/live-language-sweep.txt`. Build wiring: `testImplementation(kotlin("test"))`, `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` (Gradle 9 no longer supplies the launcher), `tasks.test { useJUnitPlatform() }`. Test methods must return `Unit` (a `= runBlocking { … }` body ending in a non-Unit expression is silently ignored by JUnit 5).

## Packaging notes

- `modules(...)` adds the three JDK modules Compose's `suggestRuntimeModules` reports. Checked by running a real Ktor request in a jlink image built with and without them — it worked both ways, so this is **precautionary**, not a fix for an observed failure. `packageExe` and the launched distributable were verified on this machine.

## No Android Config

This project has `local.properties` pointing at an Android SDK path (legacy from project scaffolding) but the build does not use `com.android.application` plugin. `compileSdk`, `minSdk`, and APK signing are not applicable.

## No Flavors / Signing

Single build target. No product flavors, no signing config.
