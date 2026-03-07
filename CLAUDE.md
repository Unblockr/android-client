# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NetBird Android client - a VPN client that connects Android devices to private resources in a NetBird network. Supports Android phones, tablets, Android TV, and ChromeOS.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug -PversionCode=123 -PversionName=1.2.3

# Build release APK (requires signing config)
./gradlew assembleRelease -PversionCode=123 -PversionName=1.2.3

# Build app bundle for Google Play
./gradlew bundleDebug -PversionCode=123 -PversionName=1.2.3
./gradlew bundleRelease -PversionCode=123 -PversionName=1.2.3

# Run unit tests (JVM)
./gradlew test

# Run instrumented tests (requires emulator/device)
./gradlew connectedDebugAndroidTest

# Build Go mobile library (run from project root)
./build-android-lib.sh [version]

# Clean build
./gradlew clean
```

## Architecture

### Module Structure

- **app/** - Main Android application module with UI layer (Activities, Fragments, ViewModels)
- **tool/** - Android library containing VPN service, engine management, network monitoring, and preferences
- **gomobile/** - Pre-compiled Go library bindings (netbird.aar) for the NetBird network engine
- **netbird/** - Git submodule containing the Go backend implementation

### Key Architectural Patterns

**Single Activity with Fragment Navigation**: MainActivity hosts all fragments via Navigation Component. Navigation graph defined in `app/src/main/res/navigation/mobile_navigation.xml`.

**Service-Based VPN**: VPNService (extends android.net.VpnService) manages the network engine. Uses bound service with LocalBinder pattern for IPC with MainActivity.

**Listener/Observer Pattern**: StateListener interface broadcasts engine state changes (connected, disconnected, peers changed). MainActivity implements StateListenerRegistry and broadcasts to registered fragments.

**ServiceAccessor Interface**: Contract for fragment-to-service communication. Implemented by MainActivity, checked by fragments in onAttach().

### Key Classes

- `MainActivity.java` - Central hub implementing ServiceAccessor, StateListenerRegistry. Handles VPN permissions, service binding, SSO authentication
- `VPNService.java` (tool module) - Core VPN service managing EngineRunner and network changes
- `EngineRunner.java` (tool module) - Manages Go network engine lifecycle, TUN device, DNS
- `HomeFragment.java` - Main connect/disconnect UI with Lottie animations
- `PeersFragment.java` / `NetworksFragment.java` - Lists connected peers and available routes

### Authentication Flows

1. **SSO via Custom Tabs** (standard Android) - Uses AndroidX Browser CustomTabsIntent
2. **Device Code Flow** (ChromeOS) - QR code dialog for platforms without proper browser support. Detected via PlatformUtils.requiresDeviceCodeFlow()

### Platform Support

PlatformUtils handles detection for:
- Android TV (D-pad navigation, drawer focus management)
- ChromeOS (device code flow for SSO)

## Build Configuration

- **Min SDK**: 26 (Android 8.0)
- **Target/Compile SDK**: 35
- **Java**: 11
- **NDK**: 23.1.7779620
- **Gradle Plugin**: 8.9.3

Firebase (Crashlytics, Analytics) conditionally included when `google-services.json` exists.

## Environment Setup

```bash
# Set environment variables (macOS example)
export ANDROID_HOME=/Users/<USERNAME>/Library/Android/sdk
export JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home

# Install NDK
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --install "ndk;23.1.7779620"
```

## Testing

Unit tests in `app/src/test/` and `tool/src/test/` run on JVM.
Instrumented tests in `app/src/androidTest/` and `tool/src/androidTest/` run on emulator/device.

CI uses GitHub Actions (`.github/workflows/build-debug.yml`) with Android emulator API 30.
