# Launch Runbook

From a server that answers to the first week in public (phase 7, DECISION-084). Each step names
who does it, because most of what is left needs the owner, not the code.

## 1. Before anyone outside sees it

| Step | Who | Where |
|---|---|---|
| Domain bought, five DNS names pointed at the server (EXT-001) | Owner | `docs/runbooks/dns-tls.md` |
| Server up, first start, first operator, map built (EXT-007) | Owner with the README | `infrastructure/production/README.md` |
| Support and privacy addresses chosen (`SUPPORT_EMAIL`, `PRIVACY_CONTACT_EMAIL`) | Owner | env file and the GitHub environment's variables |
| Privacy, terms, instructions and FAQ read and approved, or rewritten in the console | Owner and a lawyer | console → المحتوى; the second draft is migration `content_services/0007` |
| Emergency numbers confirmed against an official source | Owner | console → المحتوى → أرقام الطوارئ |
| Pharmacy verification requirements decided (LAUNCH_POLICY_PENDING) | Owner | console → متطلبات التحقق |
| Store listing text and feature graphic approved | Owner | `apps/android/play/store-listing/ar.json`, `docs/design/brand/play-feature-graphic.png` |
| Backups, restore drill and map timers enabled, and the first drill passed | Operator | README, "Backups" and "The map" |
| Outside uptime checks on the five names | Operator | `docs/runbooks/monitoring.md` |

On the console's «حالة النظام» page every card should be green, «الخريطة والمسارات» included.

## 2. Play: internal, then closed testing

Needs the Play developer account and the upload key (EXT-003).

1. Build the release from the exact commit, with `DIRECTORY_VERSION_CODE` above every upload
   and the production map, routing and API addresses (`apps/android/play/release.env.example`).
   `validatePlayRelease` refuses a placeholder.
2. Internal testing first, installed from Play, then closed testing
   (`apps/android/play/closed-testing-runbook.md`).
3. Screenshots from that build, on a real phone, never mocked
   (`apps/android/play/store-listing/ar.json`, "rule").
4. Data safety and app content, reconciled with the build
   (`apps/android/play/data-safety.md`, `app-content-checklist.md`). The foreground location
   service for trips needs its declaration and a short video of a trip with the screen locked.

## 3. Public

1. Promote the tested build. Keep the previous build's tag on the server
   (`docs/runbooks/rollback.md`).
2. Point `PLAY_STORE_URL` at the listing, and release the site's images again so its download
   links appear (`docs/runbooks/deploy.md`).

## 4. The first week

Every day:

- **The console's system page**, every card. A yellow «الخريطة والمسارات» or «النسخ الاحتياطي»
  card is a timer that did not run.
- **Play Console → Android vitals**: crash and ANR rates against Play's thresholds. Sentry has
  each crash's stack, if a DSN is set.
- **Reviews and the support inbox**: answer each one. A wrong opening hour or a missing pharmacy
  becomes a fix in the console the same day.
- **Owners' applications and claims**: reviewed within a day, because a pharmacy waiting for
  approval is a pharmacy not on the map.
- **Tonight's duty roster** for each province that has launched, before evening: a roster with
  no pharmacy is the first thing people will notice.

At the end of the week, record what was fixed and what is still open in
`docs/project/HANDOFF.md`.
