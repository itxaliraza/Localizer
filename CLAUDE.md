# Localizer — Claude Instructions

## Project

**Fast Localizer** — Kotlin/JVM desktop app (Compose Desktop, Windows EXE) that auto-translates Android `strings.xml` files via Google Translate. **Not an Android APK project.** Build with `./gradlew run` or `./gradlew packageExe`. See [wiki/infra/build.md](wiki/infra/build.md).

## Wiki

Feature knowledge base lives in [wiki/index.md](wiki/index.md).

**MUST** read `wiki/index.md` before reading source, and update every affected wiki page (+ append to `wiki/log.md`) in the same change as any code edit.

Full maintenance protocol:

**MUST — Before touching or reading any source:**
1. Read `wiki/index.md` first.
2. Open the linked wiki page for EVERY area you will touch (every affected screen/feature page + relevant `wiki/infra/*.md`).
3. Only open source files after orienting in those wiki pages.
4. Trust the wiki first. Do NOT re-explore the codebase to confirm what a wiki page already states. Open source only when the wiki is missing, stale, or silent on the question — and if you find it stale, update it before continuing.

**MUST — After EVERY code change:**
1. Update EVERY wiki page covering a file you changed (screen/feature pages, `infra/navigation.md`, `infra/di.md`, etc.). Keep **Key files** and **Consumers** accurate.
2. Append one entry to `wiki/log.md`: date, what changed, files touched.
3. If you added a screen/feature, create its `wiki/<screens|features>/<name>.md` from the template AND add its one-line entry to `wiki/index.md` in the SAME change — no exceptions.
4. If you removed a screen/feature, delete/archive its wiki page and remove its line from `wiki/index.md` in the same change.

**MUST — Coverage invariant:** every screen/view directory in the project has a corresponding page in `wiki/screens/` linked from `wiki/index.md`. A new screen without a wiki page in the same change is a hard error — treat it like a failing build.

**MUST — Before removing any feature:** read that feature's wiki page first. Its **Consumers** section is the authoritative list of files to change.

### Wiki page template (screen or feature)

```
# <Name>

## What it does
<1–3 sentences, user-facing purpose>

## Key files
- `path/to/File` — <role in one line>

## State & data
<state holder, data sources read/written>

## Dependencies
<what it injects/needs>

## Consumers
<every file that references this — exhaustive>

## Notes
<gotchas, edge cases, non-obvious decisions>
```
