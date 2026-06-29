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
| Save-as-template dialog | Compose `Dialog` (in `TemplatesCard`) | "Save" pill in Language Templates card | "Cancel" / "Save" |
| Delete-template confirm | Compose `Dialog` (in `TemplatesCard`) | trash icon on a template row | "Cancel" / "Delete" |
| `ModuleStringsDialog` | Compose `Dialog` | "View" on a module row | "Close" |
| Snackbar (file loaded / templates) | Custom Compose overlay | `HomeScreenOneTimeEvents.FileLoadedSuccess/Fail`, or `TemplatesCard` save/apply/delete | Auto-dismiss after timeout |

## Entry Point

`main()` function in `src/main/kotlin/Main.kt`. Creates the Compose `Window`, calls `startKoin`, renders `App()`.

## Back Navigation

Not applicable. No back stack exists.
