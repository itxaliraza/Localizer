# Fast Localizer

**Translate your Android app's `strings.xml` into 240+ languages in a few clicks.**

Fast Localizer is a Windows desktop app for Android developers. Point it at your project, pick the languages you want, and it creates or updates the `values-<lang>/strings.xml` files for you using Google Translate. Placeholders, markup and your existing translations are kept safe.

[**⬇ Download the latest Windows installer**](https://github.com/itxaliraza/Localizer/releases/latest)

---

## Why use it

Adding a new language to an Android app normally means copying `strings.xml`, pasting every string into a translator, and fixing the broken `%1$s` and `<b>` tags afterwards. With many languages or modules, that's hours of repetitive work. Fast Localizer does all of it in one run and writes the results straight into your project's `res/` folders, ready to build.

## Features

- **Whole-project support.** Give it a single `res/` folder or your full Android project root. Every module with a `values/strings.xml` is found automatically (`build/`, `.gradle/` and similar folders are skipped), and you can tick which modules to translate.
- **240+ languages**, with search and *Select all*. Languages that already exist in your project (for example `values-fr`, `values-pt-rBR`, `values-b+ms+Arab`) are detected and pre-selected.
- **Only translates what's missing.** Keys you already have in a language are never overwritten, so manual fixes survive. Run it again after adding new strings and only the new keys are translated.
- **Placeholders and markup are protected.** `%1$s`, `%d`, `{name}`, `<b>`, `<xliff:g>` and CDATA are shielded during translation and checked afterwards. If a translation damages a placeholder, it is retried or skipped, never written broken.
- **Keeps your XML intact.** Existing target files are merged, not replaced: comments, `translatable="false"` strings, string arrays and plurals are preserved. Resource references such as `@string/app_name` are left as they are.
- **Correct Android folder names.** Region locales are written as valid qualifiers (`pt-BR` → `values-pt-rBR`, `zh-CN` → `values-zh-rCN`), so Android Studio accepts them.
- **Language templates.** Save a set of languages (for example "Play Store top 20") and apply it with one click next time. Templates are kept between sessions.
- **Live progress.** One bar shows which module and language is being translated (`3/12`), and a second shows how many of that language's strings are done. You can stop at any time.
- **Resilient translation.** Requests rotate across several Google Translate endpoints, with retries and automatic cool-down when one is rate-limited. If one string fails, it is skipped and reported, and the rest of the run continues.

## Installation

1. Download `Fast.Localizer-<version>.exe` from the [Releases page](https://github.com/itxaliraza/Localizer/releases/latest).
2. Run the installer. It installs for your user only, so no admin rights are needed, and adds a Start menu and desktop shortcut.
3. The installer isn't code-signed, so Windows SmartScreen may show *"Windows protected your PC"*. Click **More info** → **Run anyway**.

Requirements: Windows 10/11 (64-bit) and an internet connection. Java does not need to be installed; it's bundled with the app.

## How to use

1. **Paste the path** to your Android project root (e.g. `D:\Projects\MyApp`) or to a single `res` folder (e.g. `D:\Projects\MyApp\app\src\main\res`), then click **Load File**.
2. **Choose modules.** If several modules are found, untick any you don't want translated. Each one shows how many strings it contains.
3. **Pick languages** in the left panel: search, tick individually, use *Select all*, or apply a saved template.
4. Click **Start Translation** and watch the progress bars.
5. When it's finished, click **Translation Completed, Open Now** to open the output folder. The translated `values-<lang>/strings.xml` files are already in each module's `res` folder. Open the project in Android Studio and build.

> **Tip:** commit your project to git before translating, so you can review the new and changed files with a diff.

## Good to know

- Translations are machine translations from Google Translate. For store listings or legal text, have a native speaker review them.
- Plurals are translated using your base quantities (`one`/`other`). Languages that need extra forms (for example Russian `few`/`many`) fall back to `other`.
- Strings that fail after all retries are listed at the end of the run and simply stay missing. Run the translation again later to fill them in.
- Very large runs can trigger temporary rate-limiting by Google. The app backs off automatically; if many strings fail, wait a few minutes and run again.

## Building from source

This is a Kotlin/JVM desktop app built with **Compose Multiplatform (Desktop)**. It is not an Android project, though it works on Android projects. You need JDK 17+ (JDK 21 recommended).

```bash
./gradlew run          # run the app
./gradlew test         # unit tests (offline)
./gradlew packageExe   # build the Windows installer → build/compose/binaries/main/exe/
```

**Tech stack:** Kotlin, Compose Desktop (Material 3), Ktor client, Koin, kotlinx.serialization, Java DOM XML.

**Project layout:**

```
src/main/kotlin/
├── Main.kt                 # window setup (custom title bar)
├── home_screen/            # main screen, ViewModel and state
├── languages_screen/       # language picker panel
├── data/
│   ├── translator/         # TranslationManager + Google endpoint implementations
│   ├── util/               # folder scanning, placeholder protection, templates
│   └── FilesHelper.kt      # strings.xml parsing and merging
└── di/                     # Koin module
```

Detailed feature documentation lives in [`wiki/`](wiki/index.md).

## Releases

Releases are automated. Pushing a version tag (for example `git tag 9.0.2 && git push origin 9.0.2`) makes GitHub Actions run the tests, build the Windows installer and attach it to that tag's release.

## Contributing

Issues and pull requests are welcome. Please run `./gradlew test` before opening a PR.
