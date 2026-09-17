# iOS App Store Release Plan

This is a later phase but included so project architecture is complete.

## Requirements

- Apple Developer Program.
- App Store Connect access.
- final bundle ID `com.servacode.directory`.
- signing certificates/profiles managed via modern Xcode/App Store workflow.
- privacy disclosures.
- support/privacy URLs.
- screenshots.
- App Review information.
- account deletion behavior consistent with app policy.

## Build

- archive release.
- TestFlight.
- device testing.
- crash-free staging/beta.
- submit.

## Privacy

Review actual:
- location.
- contact info/phone.
- identifiers.
- user content/images.
- diagnostics.
- analytics.

## TestFlight

Use internal then external beta where useful.

## Review

Public browsing without login reduces review friction.

Provide credentials/instructions for owner/admin-linked user features if reviewer needs them.

## Rollout

Use phased release where suitable.

## Parity rule

iOS must match Product Spec, not Android implementation quirks.
