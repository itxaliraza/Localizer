# Localizer — Wiki Index

## Overview

**Fast Localizer** is a Kotlin/JVM desktop application (Compose Desktop, Windows EXE) that automates translation of Android string resources. A developer points it at an Android `res/` folder, selects target languages from a list of 250+, and it generates localized `strings.xml` files by fetching translations from Google Translate via three rotating API endpoints. Stack: Kotlin 2.0.0, Compose Desktop 1.8.0-beta01, Ktor 2.3.12, Koin 4.0.0-RC2, Kotlinx Serialization 1.6.0. Single-module Gradle project (no `app/` submodule — source root is `src/`).

---

## UI Screens

| Screen | Wiki Page | Description |
|--------|-----------|-------------|
| Home Screen | [wiki/screens/home-screen.md](screens/home-screen.md) | Main window: folder input, translation controls, progress |
| Languages Screen | [wiki/screens/languages-screen.md](screens/languages-screen.md) | Left panel: searchable language grid with select-all |

---

## Features

| Feature | Wiki Page | Description |
|---------|-----------|-------------|
| File Loading & Extraction | [wiki/features/file-loading.md](features/file-loading.md) | Scan `values/` dirs, parse `strings.xml`, extract key-value pairs |
| Language Selection | [wiki/features/language-selection.md](features/language-selection.md) | 250+ language grid with search, select all, selection state |
| Language Import / Export | [wiki/features/lang-import-export.md](features/lang-import-export.md) | Save/restore language selections as JSON |
| Translation Orchestration | [wiki/features/translation-orchestration.md](features/translation-orchestration.md) | Coordinate multi-language translation workflow |
| Translation API Layer | [wiki/features/translation-api.md](features/translation-api.md) | Three rotating Google Translate endpoints with fallback |
| XML Parsing & Writing | [wiki/features/xml-parsing-writing.md](features/xml-parsing-writing.md) | DOM-based parse and write of `strings.xml` |
| String Sanitization | [wiki/features/string-sanitization.md](features/string-sanitization.md) | Protect placeholders and escapes during translation |
| Parallel Translation | [wiki/features/parallel-translation.md](features/parallel-translation.md) | Toggle async vs sequential key translation |
| Progress Reporting | [wiki/features/progress-reporting.md](features/progress-reporting.md) | Real-time progress bar and language label |
| Translation Cancellation | [wiki/features/translation-cancellation.md](features/translation-cancellation.md) | Stop in-flight translation coroutine |
| Open Output Folder | [wiki/features/open-output-folder.md](features/open-output-folder.md) | Open translated output directory in Explorer |
| Custom Window Controls | [wiki/features/custom-window-controls.md](features/custom-window-controls.md) | Draggable undecorated window with min/max/close |

---

## Infrastructure

| Topic | Wiki Page |
|-------|-----------|
| Navigation | [wiki/infra/navigation.md](infra/navigation.md) |
| Dependency Injection | [wiki/infra/di.md](infra/di.md) |
| Data Layer | [wiki/infra/data.md](infra/data.md) |
| Build | [wiki/infra/build.md](infra/build.md) |

---

## Architecture

[wiki/architecture.md](architecture.md)

---

## Changelog

[wiki/log.md](log.md)
