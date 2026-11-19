# Aether Executor

- **Package:** `com.aether.executor`
- **Version:** `0.1.0` (M1)
- **Platform:** Native Android, non-root
- **Script runtime:** QuickJS via `io.github.dokar3:quickjs-kt-android`
- **Host language:** Kotlin
- **UI:** Jetpack Compose

Roblox Executor is used only as a UX/workflow reference. M1 runs JavaScript inside Aether itself. It does not inject into, inspect, or modify third-party game processes.

## M1 features

- Embedded QuickJS JavaScript runtime
- Execute / Stop controls
- `console.log`, `console.info`, `console.warn`, `console.error`
- Runtime error reporting
- Per-execution runtime lifecycle
- 10-second evaluation timeout
- Dark navy / electric cyan interface
- No root required

## Build requirements

- JDK 17
- Android SDK Platform 36
- Android Gradle Plugin 8.13.2
- Kotlin 2.2.20
- Gradle 8.13+
- Android device / emulator API 26+

## Build APK

Open this folder in Android Studio and allow Gradle sync. Then select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

Or, if Gradle is installed and configured:

```bash
gradle assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Android-only / Termux note

You can edit this project from Android. Building from Termux requires a compatible JDK, Android command-line SDK, platform/build tools, and enough RAM/storage. Android Studio is the easiest build environment. The project intentionally does not include a fake Gradle wrapper; use Android Studio's Gradle integration or an installed Gradle distribution.

## Test script

```javascript
console.log("Aether Executor initialized");

const values = [10, 20, 30];
const total = values.reduce((sum, value) => sum + value, 0);

console.info("Values:", values.join(", "));
console.log("Total:", total);
console.warn("M1 runtime is running locally.");
```

## JavaScript APIs exposed in M1

Only the console methods are exposed. No Android APIs, filesystem access, network access, reflection, process memory access, or cross-app execution APIs are exposed to scripts.

## Architecture boundary

QuickJS runs user-entered JavaScript inside Aether's own process. Android's application sandbox prevents an ordinary non-root app from freely injecting code into unrelated apps. Future target-app testing should use an explicit SDK/debug bridge integrated into apps you own or are authorized to test.

## Next milestone

M2: persistent script storage, script tabs, execution history, and editor improvements.
