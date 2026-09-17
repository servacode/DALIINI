# iOS Native — Swift + SwiftUI

## Bundle ID

```text
com.servacode.directory
```

## Architecture

```text
App
Core/
  Networking
  Storage
  Auth
  DesignSystem
  Location
  Maps
  Analytics
  Observability
Features/
  Home
  Search
  Directory
  Facility
  Map
  Navigation
  Auth
  Account
  Owner
  Onboarding
  Duty
```

## State

SwiftUI Views do not own networking.

Use observable ViewModels/feature state.

## Networking

- generated OpenAPI Swift client or typed URLSession layer.
- async/await.
- central auth refresh.
- central error mapping.

## Session

Refresh material in Keychain.

## Local cache

SwiftData baseline:
- reference data.
- selected public cached content.
- owner draft safe metadata.

If SwiftData deployment target or migration constraints are unsuitable, ADR may choose Core Data.

## Location

Core Location:
- When In Use.
- approximate accuracy handling.
- no Always permission in Core.

## Maps

MapLibre Native iOS.

## Push

APNs:
- device token registration.
- deep links.
- privacy-safe payload.

## UX

Same product rules/design tokens, adapted to iOS conventions.

## Testing

- unit.
- state/view-model.
- networking.
- storage.
- UI tests.
- real device.
- permissions.
- maps.
- RTL Arabic.
