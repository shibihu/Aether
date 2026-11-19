# Aether Executor

- **Package:** `com.aether.executor`
- **Version:** `0.2.0` (M2)
- **Platform:** Native Android, non-root
- **Script runtime:** QuickJS via `io.github.dokar3:quickjs-kt-android:1.0.15`
- **Editor:** Sora Editor `0.23.6` with a bundled Aether JavaScript analyzer
- **Host language:** Kotlin
- **UI:** Jetpack Compose (Material 3)

Roblox Executor is used only as a UX/workflow reference. Aether runs JavaScript inside its own process. It does not inject into, inspect, or modify third-party game processes.

## M2 features

### Editor

- Multi-tab editing with tab rename (long-press a tab), close, and unsaved-change indicators
- Session restore: open tabs, active tab, and unsaved drafts survive process death
- JavaScript syntax highlighting (keywords, strings, template literals, numbers,
  comments, function calls, operators)
- Line numbers, current-line highlight, bracket-pair highlight, matching-symbol pairs
- Auto-indent after `{`, `[`, `(` and closing-bracket handling
- Horizontal scrolling, configurable tab size, optional word wrap, adjustable font size
- Full-document tokenization runs on a dedicated background thread; stale analyses are dropped

### Script manager

- Persistent `.js` scripts in app-private storage (`filesDir/scripts/<uuid>.js`)
- Atomic JSON index with reconciliation against the script directory
- Create, open, rename, and delete scripts
- Names are normalized, collision-safe, and stripped of path separators

### Execution

- Single-owner execution manager: only one run at a time
- `Execute` / `Stop` with `RUNNING`, `STOPPING`, `COMPLETED`, `STOPPED`, `FAILED` states
- Stop calls `QuickJs.interruptEvaluation()` and cancels the execution coroutine
- Configurable timeout (1–60 seconds, default 10) enforced by QuickJS
- 64 MB QuickJS memory limit
- Per-run QuickJS instance always closed in `finally`
- JavaScript errors include line/column and stack when available

### Console

- `console.log`, `console.debug`, `console.info`, `console.warn`, `console.error`
- Millisecond timestamps and level colors
- Auto-scroll while the user is at the bottom
- Clear current output
- Selectable, wrapped text for long messages
- Late callbacks from a stopped run cannot write into a newer run's console

### History

- Persistent history of the last 100 runs with source, status, duration, and error
- Reopen a history entry into a new editor tab
- Delete one entry or clear all history

### Settings

- Font size, tab size, word wrap, line numbers
- Execution timeout
- Stored locally in `SharedPreferences`

### Navigation

Bottom navigation: **Editor** (primary), **Scripts**, **History**, **Settings**.
Dark navy surfaces and electric cyan accents are retained.

## Architecture

```text
ui/
  AetherApp.kt              Scaffold, navigation, brand bar
  AetherViewModel.kt        UI state, tabs, orchestration
  editor/
    JsLanguage.kt           Language + background tokenizer + newline handling
    CodeEditorView.kt       Sora CodeEditor AndroidView + Aether color scheme
  screens/                  Editor, Scripts, History, Settings
  components/               Console panel, tab bar, formatters
core/
  runtime/
    QuickJsExecutor.kt      QuickJS lifecycle, console bridge, interrupt
    LogLevel.kt
  execution/
    ExecutionManager.kt     Single-run coordinator and outcome reporting
    ExecutionStatus.kt
  console/
    ConsoleManager.kt       Bounded console buffer scoped by execution id
data/
  model/Models.kt           Tabs, scripts, records, settings
  repository/               Script, history, settings, and tab repositories
  storage/AtomicJsonStore.kt
```

All state flows through `StateFlow`; repositories and storage use coroutines and
`Dispatchers.IO`. No root, cross-app private-data access, process injection,
hidden background execution, or external app integration is used.

## Build

Requirements: JDK 17, Android SDK Platform 36, AGP 8.13.2, Kotlin 2.4.10,
Gradle 8.13.

```bash
ANDROID_HOME=/path/to/android-sdk ./gradlew :app:assembleDebug
```

Verified in this repository with:

```bash
./gradlew -p /root/AetherExecutor :app:assembleDebug :app:testDebugUnitTest \
  --no-daemon -Pkotlin.compiler.execution.strategy=in-process
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Tests

Unit tests cover console execution scoping, stale-callback rejection, buffer
capping, clear semantics, and execution status helpers:

```bash
./gradlew :app:testDebugUnitTest
```

Instrumented/device tests were not run in the build environment because no
usable device or emulator was available. Stop interruption has therefore not
been verified on-device. See **Known limitations**.

## Test script

```javascript
console.log("Aether Executor initialized");

const values = [10, 20, 30];
const total = values.reduce((sum, value) => sum + value, 0);

console.info("Values:", values.join(", "));
console.log("Total:", total);
console.warn("This runtime is running locally.");
```

## JavaScript APIs exposed

Only the console methods are exposed. No Android APIs, filesystem access,
network access, reflection, process-memory access, or cross-app execution APIs
are exposed to scripts.

## Known limitations

- **Stop is implemented but not device-verified.** Stop requests a native
  QuickJS interrupt through `interruptEvaluation()` and cancels the coroutine.
  An instrumented infinite-loop test requires a device/emulator; the build
  environment had none. Do not treat cancellation as proven until that test runs.
- Editor analysis is full-document. It is off-main-thread and discards stale
  results, but very large scripts above several thousand lines will still cost
  more CPU per edit than an incremental analyzer.
- The tokenizer is a lightweight custom analyzer, not a full JavaScript parser.
  Regex literals and JSX are not specially highlighted. Inline template
  expression interpolation is highlighted as part of the template string.
- History stores full source up to 100 runs; there is no compression.
- Scripts use app-private UUID file names. There is no user-facing file picker,
  import, or export, by design for this milestone.
- No device UI test was executed, so responsive behavior on small screens and
  keyboard insets remains unverified.

## Architecture boundary

QuickJS runs user-entered JavaScript inside Aether's own process. Android's
application sandbox prevents an ordinary non-root app from injecting code into
unrelated apps. Future target-app testing should use an explicit SDK/debug
bridge integrated into apps you own or are authorized to test.
