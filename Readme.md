# NetBird Android Client

An Android VPN client that connects devices to private resources in a [NetBird](https://netbird.io) network. Supports phones, tablets, Android TV, and ChromeOS.

---

## How It Works

The app is a thin Android UI layer over a Go network engine. The Go engine handles the WireGuard tunnel, peer discovery, and management-server communication. It is compiled as an Android AAR library (`gomobile/netbird.aar`) using [gomobile](https://pkg.go.dev/golang.org/x/mobile/cmd/gomobile) and consumed via JNI bindings in the `gomobile` module.

### Module structure

| Module | Purpose |
|--------|---------|
| `app/` | UI layer — Activities, Fragments, ViewModels, navigation graph |
| `tool/` | Android library — `VPNService`, `EngineRunner`, profile management, preferences |
| `gomobile/` | Prebuilt AAR wrapper — exposes the Go engine to Java |
| `netbird/` | Git submodule — Go source for the network engine |

### Runtime architecture

```
MainActivity
  └─ binds to ──► VPNService  (foreground service, survives activity lifecycle)
                    └─ owns ──► EngineRunner
                                  └─ calls ──► gomobile / Go engine
                                                (WireGuard tunnel, peer comms)
```

`MainActivity` implements two interfaces that fragments depend on:

- **`ServiceAccessor`** — lets fragments trigger connect/disconnect, query peers/routes
- **`StateListenerRegistry`** — broadcasts engine state changes (connected, disconnected, peer list changed) to any registered fragment

### Authentication

Two flows are supported, chosen at runtime:

| Platform | Flow |
|----------|------|
| Standard Android | SSO via Android Custom Tabs (browser redirect) |
| ChromeOS / headless | Device code flow — QR code displayed in-app, user authenticates on another device |

First-time registration uses a **setup key** (UUID format). The key is submitted to the management server via the Go `Auth` binding, which writes an encrypted config file to the app's private files directory. Subsequent launches skip the setup screen once a valid config file is present.

### Navigation

Single-activity with Jetpack Navigation Component. `HomeFragment` is the start destination. On first launch (or when no valid registration exists), the app navigates to `SetupFragment` and locks all other navigation until registration succeeds.

---

## Required Tooling

### Android development

| Tool | Version | Notes |
|------|---------|-------|
| Android Studio | Meerkat (2024.3) or newer | Recommended IDE |
| JDK | 17 | Bundled with Android Studio; set `JAVA_HOME` if building from CLI |
| Android SDK | API 35 | Install via SDK Manager |
| Android NDK | 23.1.7779620 | Required for the Go library; install via SDK Manager |
| ADB | Any | Included with Android SDK platform-tools |

Set environment variables if building from a terminal:

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk           # macOS
export JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

Install the NDK:

```bash
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --install "ndk;23.1.7779620"
```

### Go toolchain (only needed to rebuild the native library)

| Tool | Notes |
|------|-------|
| Go 1.22+ | [golang.org/dl](https://golang.org/dl) |
| gomobile | `go install golang.org/x/mobile/cmd/gomobile@latest` |

---

## Building

### Step 1 — Clone with submodules

```bash
git clone --recurse-submodules <repo-url>
cd android-client
```

If you already cloned without submodules:

```bash
git submodule update --init --recursive
```

### Step 2 — Build the Go library (when Go source changes)

The prebuilt `gomobile/netbird.aar` is committed to the repository. You only need to rebuild it if you change the Go engine in the `netbird/` submodule.

```bash
./build-android-lib.sh          # uses latest git tag or "dev-<hash>"
./build-android-lib.sh 1.2.3    # explicit version
```

This runs `gomobile bind` inside the `netbird/` submodule and writes the output to `gomobile/netbird.aar`.

### Step 3 — Build the Android app

Both `versionCode` and `versionName` are required parameters.

```bash
# Debug APK
./gradlew assembleDebug -PversionCode=1 -PversionName=0.0.1

# Release APK (requires signing config — see below)
./gradlew assembleRelease -PversionCode=1 -PversionName=1.0.0

# App Bundle (for Google Play)
./gradlew bundleRelease -PversionCode=1 -PversionName=1.0.0

# Clean
./gradlew clean
```

### Release signing

Provide signing credentials as Gradle properties or environment variables:

| Property | Env var | Description |
|----------|---------|-------------|
| `NETBIRD_UPLOAD_STORE_FILE` | same | Path to `.jks` / `.keystore` file |
| `NETBIRD_UPLOAD_STORE_PASSWORD` | same | Keystore password |
| `NETBIRD_UPLOAD_KEY_ALIAS` | same | Key alias |
| `NETBIRD_UPLOAD_KEY_PASSWORD` | same | Key password |

### Firebase (optional)

Drop a valid `app/google-services.json` into the repo before building. Gradle detects the file automatically and enables Crashlytics and Analytics. The build succeeds without it — Firebase is simply omitted.

---

## Installing on a device or emulator

```bash
# List connected devices
adb devices

# Install (add -s <device-id> if multiple devices are attached)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Testing

```bash
# Unit tests (JVM, no device needed)
./gradlew test

# Instrumented tests (requires a connected device or running emulator)
./gradlew connectedDebugAndroidTest
```

Test results are written to:
- `app/build/reports/tests/` and `tool/build/reports/tests/` (unit)
- `app/build/reports/androidTests/` and `tool/build/reports/androidTests/` (instrumented)

---

## CI

GitHub Actions workflows are in `.github/workflows/`:

| Workflow | Trigger | What it does |
|----------|---------|--------------|
| `build-debug.yml` | Push to `main`, all PRs | Builds debug APK + AAR, runs unit and instrumented tests (API 30 emulator) |
| `build-release.yml` | Manual / tag | Builds signed release APK and App Bundle |

---

## Platform notes

| Platform | Detection | Behaviour difference |
|----------|-----------|----------------------|
| Android TV | `UiModeManager` | D-pad navigation, drawer opens on long-press left, QR scanner hidden |
| ChromeOS | User-agent heuristic | Device code flow for SSO instead of Custom Tabs |
| Phones / tablets | Default | Full feature set |

---

## NetBird API

Management server REST API documentation: https://docs.netbird.io/api
