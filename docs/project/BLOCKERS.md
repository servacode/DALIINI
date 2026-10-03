# Blockers

Last reviewed: 2026-10-03

What the work is waiting on that code cannot supply: an account, a purchase, a credential or the
owner's decision. Bugs and local toolchain limits are not recorded here. The register's history
up to 2026-09-29 (EXT-001 to EXT-006, INT-084) is `docs/archive/BLOCKERS-2026-09-29.md`.

## Open

| ID | What is missing | Needed for | What is ready meanwhile |
|---|---|---|---|
| EXT-001 | A domain the project owns | phase 6: DNS, TLS, the site's and the app's links, store listings | Every host is a setting; nothing names a domain |
| EXT-007 | A VPS (sized and paid for by the owner) | phase 6: the production stack | The compose stack the VPS will run is the development one, already proven end to end (DECISION-068) |
| EXT-002 | Production credentials: the WhatsApp account or Cloud API number that sends codes, FCM for each environment, S3 storage, optionally Sentry | sign-in, push, photos in production | Each provider is behind its adapter. FCM was proven on a phone on 2026-10-01 (`NOTIFICATIONS-SETUP.md`). Production refuses to start half-configured |
| EXT-003 | The Google Play developer account and the app signing key | phase 7: closed testing, then release | The release build, App Links and store material are prepared (`apps/android/play/`) |

## Owner actions (no code waits on them, but they must not be forgotten)

- **Make `main` the repository's default branch.** It is still `android-build-verification-20260919`, an old working branch: pull requests open against it by default, and the README a visitor sees is that branch's. GitHub → Settings → General → Default branch.
- **The repository is public.** Nothing secret is committed, and CI checks that. If it should be private before launch, change it under Settings → General → Danger zone.
- **Emergency numbers** (110, 113, 112) were seeded from commonly cited lists. The console shows them under a warning until each is confirmed against an official source («تأكيد صحة الرقم»).
- **Legal texts and the support address**, which the owner approves in phase 7.

## Closed since the last register

| ID | How it was closed |
|---|---|
| EXT-004 — no repository | The project lives in `servacode/DALIINI` |
| EXT-005 — Render staging cost | Render is not used, by the owner's decision of 2026-10-03 (`ROADMAP.md`, «القرارات المعتمدة»); hosting is a VPS, EXT-007 |
| EXT-006 — GitHub Actions ran no job | Actions run: CI, CodeQL, the security scans and the Android build are green on every pull request since #27 |
| INT-084 — members added by account id | Invitations by phone number, answered the same whether or not the number has an account (DECISION-064) |
