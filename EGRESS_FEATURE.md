# Egress Node Selection Feature - Implementation Guide

## Overview
This document summarizes the decisions and strategy for implementing egress node selection in the Netbird Android client. The feature allows users to select from available egress nodes (represented as Netbird groups) to route their Internet traffic through specific egress points.

## Feature Requirements
- **Authentication**: Users authenticate using one-time setup keys provided by an admin system
- **Egress Selection**: App displays list of available egress nodes (groups) for user selection
- **Routing Update**: Selection updates the Netbird control plane to move the peer from default routing group to selected egress group
- **Persistence**: Selected egress node persists across app restarts and reconnections
- **Scope**: Per-peer selection (each device can have different egress)

## Architecture Decisions

### Authentication
- Use existing `Auth.loginWithSetupKeyAndSaveConfig()` method for one-time token authentication
- Setup keys are 36-character UUIDs (standard Netbird format)
- Store JWT token and management URL in profile config after successful auth

### Egress Node Representation
- Egress nodes = Netbird groups with specific naming convention (e.g., "egress-*")
- Peer membership in egress group controls routing policies
- Use Netbird REST API for group management:
  - `GET /api/groups` - List available egress groups
  - `PUT /api/groups/{groupId}` - Update group to add/remove peer

### Persistence
- Store selected egress group ID in Android SharedPreferences
- Key: `selected_egress_group_id` (scoped per management server/profile)
- Load persisted selection on app startup and display in UI
- Re-apply selection if connection state changes

### API Integration
- Add HTTP client (OkHttp) for control plane API calls
- Extract JWT token from profile config for authentication
- Handle API errors, token expiration, and network issues
- Cache group list to minimize API calls

## Implementation Plan

### Phase 1: Authentication Enhancement — Setup Key Entry
1. Add setup key input UI to login flow, with three input methods (see below)
2. `ChangeServerFragmentViewModel` auth/validation logic is unchanged
3. Update config storage for API access

#### Setup Key Input Methods

Typing a 36-character UUID is impractical, especially on Android TV (D-pad only). Three methods
are provided, all populating the existing `editTextSetupKey` field:

| Method | Phone/Tablet | ChromeOS | Android TV |
|---|---|---|---|
| **Short code** (primary) | Yes | Yes | Yes — 6 chars, D-pad viable |
| **QR scan** (convenience) | Yes | Yes | Hidden — no camera |
| **Clipboard paste** (convenience) | Yes | Yes | Yes (TV Remote keyboard) |

##### Short Code Flow

The admin system owns a short-code → UUID mapping cache:

```
Admin system:
  1. Creates setup key via Netbird management API → gets full UUID
  2. Generates a short code (e.g. B7K2XN, 6–8 alphanumeric chars)
  3. Stores  short_code → UUID  with TTL + single-use flag
  4. Exposes: GET /api/redeem?code=B7K2XN → {"setup_key":"0EF79C2F-..."}

Android app:
  1. User types B7K2XN
  2. App calls resolution endpoint (URL derived from management server URL)
  3. Gets full UUID, auto-fills setup key field, proceeds with normal auth
```

**Backend contract:**
- `GET {management_host}/api/redeem?code={shortCode}`
- Success: `200 {"setup_key":"<UUID>"}`
- Failure: `404 {"error":"invalid or expired code"}`
- Endpoint must be unauthenticated; codes should be single-use with 15–30 min TTL

**Resolver URL:** Derived from the management server URL (same host + `/api/redeem`). No extra config field needed.

##### Files Changed for Phase 1

| File | Change |
|---|---|
| `app/src/main/AndroidManifest.xml` | Add `CAMERA` permission + `uses-feature android:required="false"` |
| `app/src/main/res/layout/fragment_server.xml` | Short code field + Resolve button; QR scan button; clipboard paste button |
| `app/src/main/java/io/netbird/client/ui/server/ChangeServerFragment.java` | Short code HTTP resolution; ZXing QR scan launcher; clipboard UUID detection; hide scan on TV |
| `app/src/main/res/values/strings.xml` | New string resources for UI labels and error messages |
| `app/src/main/res/drawable/ic_qr_code_scanner_24.xml` | Material vector icon for scan button |

No new library dependencies:
- HTTP resolution uses `java.net.HttpURLConnection` (no OkHttp needed for this call)
- QR scanning via `zxing-android-embedded` v4.3.0 (already declared)

### Phase 2: API Client Development
1. Create `NetbirdApiClient` class with HTTP methods
2. Implement group listing and peer update endpoints
3. Add error handling and retry logic

### Phase 3: UI Development
1. Create `EgressSelectionFragment` with group selection UI
2. Add to navigation graph and menu
3. Integrate with ViewModel for state management

### Phase 4: Persistence & State Management
1. Extend `PreferenceUI` with egress selection storage
2. Add ViewModel methods for save/load operations
3. Handle profile switching and server changes

### Phase 5: Integration & Testing
1. Connect UI to API calls via ViewModel
2. Test with self-hosted Netbird instance
3. Handle edge cases (offline, invalid groups, token expiry)

## Key Technical Details

### Dependencies
- OkHttp for HTTP client (Phase 2+ API calls; Phase 1 short-code resolution uses `HttpURLConnection`)
- Existing gomobile bindings for auth
- Android SharedPreferences for persistence
- `zxing-android-embedded` v4.3.0 (already declared) for QR scanning

### Error Handling
- Network errors: Show retry options
- Invalid tokens: Trigger re-authentication
- Missing groups: Clear preference and show warning
- API rate limits: Implement backoff and caching

### Security
- Store JWT tokens securely (consider Android Keystore for sensitive data)
- Validate all API responses
- Handle token expiration gracefully

### UI/UX Considerations
- Loading states during API calls
- Clear indication of current selection
- Confirmation dialogs for changes
- Support for Android TV D-pad navigation

## Testing Strategy
- Unit tests for API client and ViewModel logic
- Integration tests with mock API responses
- End-to-end testing with real Netbird server
- UI tests for selection flow and persistence

## Open Questions
- Exact naming convention for egress groups (to be confirmed with admin system)
- Whether to show all groups or filter by permissions/metadata
- Handling of multiple concurrent selections (edge case)
- Confirm short-code resolver endpoint path with admin system team (assumed `/api/redeem`)

## Success Criteria
- Users can authenticate with setup keys
- Egress selection persists across sessions
- API calls successfully update peer group membership
- UI provides clear feedback for all operations
- Feature works on all supported Android platforms (phone, tablet, TV, ChromeOS)