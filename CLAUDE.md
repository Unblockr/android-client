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

## Netbird API documentation

Documentaion for the Netbird REST api may be viewed at
https://docs.netbird.io/api

## Testing

Unit tests in `app/src/test/` and `tool/src/test/` run on JVM.
Instrumented tests in `app/src/androidTest/` and `tool/src/androidTest/` run on emulator/device.

CI uses GitHub Actions (`.github/workflows/build-debug.yml`) with Android emulator API 30.

# CLAUDE.md - Guidelines


Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

