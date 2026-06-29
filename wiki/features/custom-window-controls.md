# Custom Window Controls

## What it does

Provides a custom title bar with "Fast Localizer" title, minimize, maximize/restore, and close buttons on an undecorated (borderless) AWT window. Supports window dragging by clicking anywhere in the top 40px strip.

## Key files

- `src/main/kotlin/Main.kt`:
  - `CustomTitleBar(frame, isMaximized, onMinimize, onMaximize, onClose)` — Composable rendering title + three control buttons with hover icons
  - `setupWindowDragging(frame, titleBarHeight)` — `MouseAdapter` listens on the AWT frame; records mouse-down position; `mouseDragged` updates `frame.location`
  - `maximizeWindow(frame)` — reads `Toolkit.getDefaultToolkit().screenInsets()` to avoid the taskbar; sets `frame.setBounds()` to available screen area
  - `restoreWindow(frame)` — resets to 800×600, centers on screen

## State & data

- **Local state:** `isMaximized: Boolean` (Compose `mutableStateOf`) in `App()` Composable
- **No ViewModel involvement** — pure window-management side effects

## Dependencies

- `java.awt.Frame`, `java.awt.Toolkit`, `java.awt.event.MouseAdapter` (JVM stdlib)
- Compose Desktop `Window` with `undecorated = true`

## Consumers

- `src/main/kotlin/Main.kt` — `App()` calls `CustomTitleBar` and `setupWindowDragging`; these are self-contained inside `Main.kt`

## Notes

- The window is created with `undecorated = true` so the OS title bar is hidden entirely; all OS window-management features (snap, taskbar thumbnail) still work via the AWT frame.
- `setupWindowDragging` uses a `titleBarHeight` threshold (40dp): dragging only triggers if the mouse-down Y position ≤ 40. This prevents the content area from accidentally moving the window.
- `maximizeWindow` accounts for the Windows taskbar by reading `screenInsets`; a naive `setBounds(screenSize)` would hide content behind the taskbar.
