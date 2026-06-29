# Build

## Type

Kotlin/JVM desktop application. **Not an Android APK.** Builds a Windows EXE installer via the Compose Desktop Gradle plugin.

## Key Build Files

- `build.gradle.kts` — single-module build config (no `app/` submodule; source root is `src/`)
- `settings.gradle.kts` — project name only
- `gradle.properties` — `kotlin.version=2.0.0`, `compose.version=1.6.10`
- `gradlew.bat` — includes the `TEMP=C:\tmp` / `TMP=C:\tmp` fix for the JDK21 AF_UNIX spaces bug on this machine

## Plugin Versions

| Plugin | Version |
|--------|---------|
| Kotlin JVM | 2.0.0 |
| Compose Desktop | 1.8.0-beta01 |
| Kotlin Compose Compiler | (bundled with kotlin.plugin.compose) |
| Kotlin Serialization | 2.0.0 |

## Key Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `compose.desktop.currentOs` | — | Compose Desktop for current OS |
| `io.ktor:ktor-client-cio` | 2.3.12 | HTTP client (CIO engine) |
| `io.ktor:ktor-client-content-negotiation` | 2.3.4 | JSON content negotiation |
| `io.ktor:ktor-serialization-kotlinx-json` | 2.3.4 | Ktor + kotlinx.serialization bridge |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.6.0 | JSON serialization |
| `io.insert-koin:koin-compose` | 4.0.0-RC2 | Koin DI with Compose integration |
| `compose.material3` | — | Material 3 Composables |
| `compose.components.resources` | — | Compose Multiplatform resource loading |
| `org.json:json` | 20210307 | JSON parsing for API responses |
| `com.github.junrar:junrar` | 7.5.5 | RAR archive extraction (not used in main flow) |
| `org.slf4j:slf4j-api` | 2.0.9 | Logging facade |
| `ch.qos.logback:logback-classic` | 1.4.11 | Logging implementation |

## Build Targets

| Task | Output |
|------|--------|
| `./gradlew run` | Run app directly from Gradle |
| `./gradlew package` | Package as native distribution |
| `./gradlew packageExe` | Build Windows EXE installer |
| `./gradlew packageReleaseExe` | Build release EXE |

## Distribution Config

```kotlin
compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe)
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

## No Android Config

This project has `local.properties` pointing at an Android SDK path (legacy from project scaffolding) but the build does not use `com.android.application` plugin. `compileSdk`, `minSdk`, and APK signing are not applicable.

## No Flavors / Signing

Single build target. No product flavors, no signing config.
