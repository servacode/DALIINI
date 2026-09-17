# Google Play Release Plan — Current Baseline 2026-09-17

This document must be rechecked against official Play Console policy immediately before submission.

## 1. Target API

As of 31 Aug 2026, new apps and updates submitted to Google Play must target Android 16 / API 36 or higher.

Baseline:
```text
targetSdk = 36
```

## 2. Artifact

Publish Android App Bundle:
```text
.aab
```

APK is for local/testing, not normal Play production upload.

## 3. Signing

Use Play App Signing.

Maintain:
- Play-managed app signing key.
- developer upload key.

Upload key:
- create dedicated keystore.
- store outside repo.
- encrypted backup.
- secret password outside source.
- CI signing through secure secrets if used.

## 4. Package identity

Baseline:
```text
com.servacode.directory
```

Before first Play app creation, verify it is the final desired applicationId. Once published, it cannot be casually changed.

## 5. Store listing assets

Prepare:
- App icon 512×512 PNG, max 1024 KB.
- Feature graphic 1024×500 JPEG/24-bit PNG.
- At least 2 screenshots meeting Play size rules.
- Prefer at least 4 high-resolution phone screenshots, portrait 1080×1920 or better.
- Arabic listing.
- English listing optional when ready.
- short description.
- full description.
- contact email.
- privacy URL.
- support URL.

Screenshots must show actual app, not fabricated AI UI.

## 6. App content forms

Complete accurately:
- Data safety.
- App access.
- Ads declaration if applicable.
- Content rating.
- Target audience.
- Permissions declarations.
- News/health categories if Play requests relevant disclosures.
- account deletion URL.

## 7. Data Safety expected inventory

Final form must be based on actual shipped SDKs/code.

Expected categories to review:
- name.
- phone.
- user ID.
- approximate/precise location if sent to server.
- profile photos.
- owner-uploaded photos/evidence.
- app interactions.
- diagnostics/crash data.
- device/session data.

Do not copy this list blindly; inspect final app and SDKs.

## 8. Account deletion

Because app creates accounts, provide:
- in-app deletion request.
- external deletion web page.
- deletion of associated data subject to documented legitimate retention.

External URL:
```text
https://www.<ROOT_DOMAIN>/delete-account
```

## 9. Permissions

Expected:
- INTERNET.
- location while-in-use.
- notifications where required.
- photo picker rather than broad storage permissions where possible.

Avoid:
- background location.
- contacts.
- SMS reading.
unless a future approved feature absolutely requires them.

## 10. Foreground service

If built-in navigation uses a foreground service, ensure:
- valid use case.
- correct service type.
- correct permission.
- Play declarations/policy compliance.

## 11. Testing tracks

Recommended:
1. Internal.
2. Closed.
3. Production.

For personal developer accounts created after 13 Nov 2023, if production access is not yet granted, Google requires a closed test with at least 12 testers continuously opted in for at least 14 days before applying for production access.

Do not assume this requirement if Play Console already shows Production access; inspect actual account/app eligibility.

## 12. Test release checklist

- signed AAB.
- versionCode incremented.
- versionName.
- mapping file if minified.
- no debug endpoints.
- Production API.
- no test OTP provider.
- no staging domain.
- release notes.
- smoke test installed from Play track, not only sideload.

## 13. Production access application

When required:
- explain app purpose.
- explain testing.
- provide honest tester engagement.
- describe improvements from feedback.
- confirm readiness.

## 14. Production rollout

Do staged rollout.

Example:
```text
5% → observe
20% → observe
50% → observe
100%
```

Exact percentages can be adjusted operationally.

Monitor:
- crashes.
- ANRs.
- login.
- API errors.
- location.
- map.
- review feedback.

## 15. Country availability

The product is Syria-focused.

Play Console availability must use territories actually offered by Google for the developer account. If Syria is unavailable in the selector, record the constraint and choose the approved distribution set intentionally; do not falsify store metadata.

## 16. Review access

If reviewers need authenticated flows:
- provide valid demo credentials/OTP bypass method strictly for review if Play supports instructions.
- public browsing should remain reviewable without account.

## 17. Release evidence

Record:
- AAB SHA-256.
- version.
- commit SHA.
- Play release ID.
- rollout status.
- QA report.
- security report.
