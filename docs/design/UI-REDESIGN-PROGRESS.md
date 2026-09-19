# UI/UX redesign — progress

One screen at a time: design, build, review on the device, approve, next. Business logic and
the backend stay as they are unless a screen is blocked without a change.

| # | Screen | Status | Approved | Evidence |
|---|---|---|---|---|
| 01 | Splash | **APPROVED** | 2026-09-20 | `artifacts/evidence/android-screen01-splash-20260919.txt` |
| 02 | Welcome | in progress | — | — |
| 03 | Location permission | in progress | — | — |

## 01 — Splash (approved)

Shipped in `41f7c2f`: one launch theme on AndroidX SplashScreen, the brand mark on the system
splash and the app's splash at one size in one place, the name, a one-line tagline, a secondary
footer, soft shapes, a ping under the pin, and the system bars painted for the splash alone.

Decisions taken with the approval, to be respected by later screens:

* No minimum display time, and nothing delays start-up so that text can be read longer.
* The placeholder mark stands until an approved logo exists (`BRAND-ASSETS.md`).
* The splash copy is provisional until product copy is approved.
* Home's loading indicator, which shows while the splash hands over, waits for Home's redesign.
* The launcher icon waits for branding and the Play Store preparation.
* Tajawal is not bundled app-wide without a separate design-system decision; screens use the
  design-system styles, which follow whatever the theme carries.
