# Open Output Folder

## What it does

After a successful translation, presents a "Translation Completed, Open Now" button that opens the translated output directory in Windows Explorer using the Java Desktop API.

## Key files

- `src/main/kotlin/data/util/openDownloadsFolder.kt` — `openDownloadsFolder(path: String)`: calls `Desktop.getDesktop().open(File(path))`
- `src/main/kotlin/home_screen/HomeScreenNew.kt` — renders the button when `translationResult is TranslationCompleted`; onClick calls `openDownloadsFolder(state.loadedPath)`

## State & data

- **Input path:** `HomeScreenState.loadedPath` (set when file is successfully loaded)
- **No state change:** this is a fire-and-forget side effect

## Dependencies

- `java.awt.Desktop` (JVM stdlib)

## Consumers

- `src/main/kotlin/home_screen/HomeScreenNew.kt` — sole call site

## Notes

- Opens the *source* directory (where the `values/` folders were read from), not a separate output directory. Translated files are written back in-place next to the existing `values-*/` folders.
- `Desktop.getDesktop().open()` is platform-dependent; on non-Windows platforms this may open a file manager or fail silently.
