# Navigation

## Pattern

Single-window, single-screen application. There is no NavHost, NavGraph, or multi-screen navigation of any kind.

## Structure

```
main() [Main.kt]
  └── Compose Window (undecorated AWT window)
        └── App() Composable
              ├── CustomTitleBar (Column top)
              └── HomeScreenNew() (fills remaining space)
                    ├── LanguagesScreen (left column, LazyVerticalGrid)
                    └── Right panel (folder input, controls, progress)
```

## Overlays / Dialogs

| Overlay | Type | Trigger | Dismiss |
|---------|------|---------|---------|
| `JsonGuideDialog` | Compose `AlertDialog` | "?" icon next to Import Languages | "Close" button sets local bool to false |
| File picker | AWT `FileDialog` | "Import Languages" button | OS dialog cancel |
| Snackbar (file loaded) | Custom Compose overlay | `HomeScreenOneTimeEvents.FileLoadedSuccess/Fail` | Auto-dismiss after timeout |
| Snackbar (export) | Custom Compose overlay | Export button success | Auto-dismiss |

## Entry Point

`main()` function in `src/main/kotlin/Main.kt`. Creates the Compose `Window`, calls `startKoin`, renders `App()`.

## Back Navigation

Not applicable. No back stack exists.
