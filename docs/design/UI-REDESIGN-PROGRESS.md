# UI/UX redesign — progress

The whole interface is built first, against the approved references in `docs/design/ui-reference/`,
and the device review comes once at the end over a single APK. Business logic and the backend stay
as they are: the redesign changes how the app looks and reads, not what it does.

| # | Screen | Reference | State |
|---|---|---|---|
| 01 | Splash | `screen-01-splash.png` | **APPROVED** on device, 2026-09-20 |
| 02 | Welcome | `screen-02-welcome.png` | built |
| 03 | Location permission | `screen-03-location-permission.png` | built |
| 04 | Home / directory list | `screen-04-home-directory-list.png` | built |
| 05 | Map | `screen-05-home-map.png` | built |
| 06 | Search | `screen-06-search.png` | built |
| 07 | Filters | `screen-07-filters.png` | built, minus what the API does not filter on |
| 08 | Facility details | `screen-08-facility-details.png` | built |
| 09 | Ratings | `screen-09-*.png` | built as stars only — see below |
| 10 | Photos | `screen-10-facility-photos.png` | built |
| 11 | Navigation | `screen-11-navigation-route.png` | built, never tried on a device |
| 12 | Sign in | `screen-12-login.png` | built, phone and password |
| 13 | Create account | `screen-13-create-account.png` | built, phone and password |
| 14 | Password recovery | `screen-14-password-recovery.png` | built |
| 15 | Profile | `screen-15-profile.png` | built |
| 16 | Favourites | `screen-16-favorites.png` | built — the backend now has it |
| 17 | Notifications | `screen-17-*.png` | built — the backend now has an inbox |
| 18 | Owner dashboard | `screen-18-owner-dashboard.png` | built, minus counts the API does not return |
| 19 | Add / edit facility | `screen-19-add-edit-facility.png` | built |
| 20 | Location picker | `screen-20-location-picker.png` | built |
| 21 | Verification evidence | `screen-21-verification-documents.png` | built |
| 22 | Submission status | `screen-22-submission-status.png` | built |
| 23–28 | Loading, empty, error, offline, permission, dialogs | `screen-23…28` | one set of components, used by every screen |

Screens the app has that the list does not name, and which were kept: the province picker, the
owner's duty shifts, and the owner's screen for one facility.

## What the references ask for and the product does not have

* **Favourites.** Built end to end on 2026-09-24: a saved facility is a row against the account,
  so it follows the account to any phone. Reached from the profile; the bottom bar still carries
  three destinations, because a saved list is not a place one lives in.
* **Notifications.** Built end to end on 2026-09-24: the inbox is the record, a push is only an
  announcement of it, and the first real producer is the review decision on an owner's
  application. FCM provider delivery remains externally unverified.
* **Written reviews.** A rating is a number of stars. There is no review text, no reviewer and no
  public list of other people's ratings, so Screen 09 is the user's own ratings.
* **Owner metrics.** The dashboard reference shows views and reviews per facility; the owner API
  returns no such figures.
* **A distance filter.** The directory query takes open-now, on-duty-now, a search term and the
  province. There is no radius, so the filters screen does not offer one.
* **Pictures in a list row.** The list contract carries no image for a facility, only the detail
  does, so rows show the brand's own mark.
* **An account picture.** The account API has no avatar upload (INT-017).

## 01 — Splash (approved)

Shipped in `41f7c2f`: one launch theme on AndroidX SplashScreen, the brand mark on the system
splash and the app's splash at one size in one place, the name, a one-line tagline, a secondary
footer, soft shapes, a ping under the pin, and the system bars painted for the splash alone.

Decisions taken with the approval, and respected by every screen since:

* No minimum display time, and nothing delays start-up so that text can be read longer.
* The placeholder mark stands until an approved logo exists (`BRAND-ASSETS.md`).
* The copy is provisional until product copy is approved. Every screen keeps its words in one
  `…Copy` object so the approved wording can be dropped in without touching a layout.
* Home's loading indicator now comes from the design system's own loading state.
* The launcher icon waits for branding and the Play Store preparation.

## The design system

`core:designsystem` carries the whole visual language: the brand faces (Tajawal then, Alexandria and IBM Plex Sans Arabic since identity v2) set once in the
theme, the tokens that give every measurement, colour, radius and elevation, the app's own icon
set, and the components every screen is assembled from. No screen sets a font, a colour or a
corner of its own.
