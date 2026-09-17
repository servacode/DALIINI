# Testing, QA and Evidence

## Test principle

Source test pass != production readiness.

## Backend

Run:
```bash
uv run ruff check .
uv run mypy .
uv run pytest
uv run python manage.py check
uv run python manage.py makemigrations --check --dry-run
```

Connected:
- PostGIS.
- Redis.
- Celery.
- object storage.

## Admin

```bash
pnpm lint
pnpm typecheck
pnpm test
pnpm build
pnpm playwright test
```

## Android

```bash
./gradlew lint
./gradlew test
./gradlew :app:assembleDebug
./gradlew connectedDebugAndroidTest
./gradlew :app:bundleRelease
```

## iOS

- Xcode build.
- XCTest.
- UI tests.
- simulator.
- physical device.

## Backend critical test suites

- phone normalization.
- OTP.
- sessions.
- refresh concurrency.
- RBAC.
- owner IDOR.
- lifecycle.
- evidence gates.
- media privacy.
- hours.
- duty overlap.
- temporary closure.
- PostGIS nearest.
- search.
- ratings.
- admin concurrency.
- realtime after-commit.
- account deletion.

## Android critical QA

- cold start.
- warm start.
- no location permission.
- approximate.
- precise.
- offline startup.
- online recovery.
- process death.
- session expiry.
- map.
- route.
- upload.
- push.
- RTL.
- large font.
- Android system bars.

## Golden path

Staging must prove:
```text
Owner registration
→ facility draft
→ evidence
→ submit
→ admin approval
→ public discovery
→ duty
→ map/directions
→ rating
→ sensitive edit
→ reverification
```

## Evidence ledger

Each gate records:
- timestamp.
- git commit.
- environment.
- command.
- result.
- log path.
- artifact hash where relevant.

Example:
```json
{
  "gate": "ANDROID_PUBLIC_DEVICE_PASS",
  "commit": "...",
  "device": "SM-A525F",
  "android": "14",
  "timestamp": "...",
  "evidence": ["artifacts/..."]
}
```

## Release blocker severity

P0:
- security compromise/data loss/core app unusable.

P1:
- major workflow broken/no reasonable workaround.

P2:
- significant but workaround exists.

P3:
- minor/polish.

No P0/P1 at RC.
