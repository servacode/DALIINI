# Custom Admin — Next.js Specification

## Philosophy

The Admin is the operating system for employees.

Do not expose Django Admin in Production.

## Routing

```text
/login
/dashboard
/reviews
/reviews/[id]
/facilities
/facilities/[id]
/users
/users/[id]
/taxonomy/groups
/taxonomy/categories
/provinces
/verification
/ads
/audit
/analytics
/settings
/system
```

## Layout

Arabic RTL default:
- right sidebar desktop.
- responsive drawer.
- top bar: search, role/context, notifications/system indicator.
- main content.
- breadcrumbs only when useful.

## Design

Avoid AI-dashboard look:
- no excessive glowing cards.
- no decorative gradients without function.
- data density appropriate for staff.
- tables readable.
- primary actions clear.
- destructive actions separated.

## Authentication

Recommended browser pattern:
- Next.js same-origin BFF.
- refresh cookie HttpOnly.
- access state short-lived server/in-memory.
- backend origin validation.
- Secure cookie Production.

No refresh in localStorage.

## Permission-aware UI

Hide/disable actions according to permissions, but backend rechecks.

Create central:
```text
can(permission)
```

## Dashboard

Widgets by role:
- pending reviews.
- reverification.
- facilities by status.
- duty currently active.
- recent operational actions.
- user registrations.
- content issues.
- system warnings.
- analytics summary.

## Review Queue

Filters:
- kind.
- province.
- category.
- submitted date.
- status.
- evidence completeness.

Review page:
- applicant summary.
- facility data.
- map.
- images.
- private evidence.
- checklist.
- duplicate warnings.
- previous state / diff.
- audit.
- approve/reject.

Reject requires reason.

## Facilities

Table:
- name.
- category.
- province.
- status.
- owner.
- availability if useful.
- updated.

Actions:
- open.
- suspend.
- reactivate.
- close.
- audit.

## Users

Search:
- name.
- phone.
- status.
- role.

Actions permission-gated.

Never show password hashes or session secret material.

## Taxonomy

Group/category editor:
- Arabic/English name.
- icon.
- order.
- activation.
- move category.
- capabilities.
- per-province switches.
- verification policy.

Prevent changing immutable code/slug silently.

## Provinces

- activate/deactivate.
- cities/neighborhood reference data.
- rollout checklist display later.

## Ads

Editor:
- image.
- copy.
- target.
- schedule.
- duration.
- order.
- preview.
- status.

## Audit

Filters:
- actor.
- action.
- resource.
- date.
- requestId.

No raw secrets.

## Analytics

KPIs:
- facilities active.
- pending review.
- approval time.
- searches.
- zero-result search.
- facility views.
- directions.
- map use.
- category usage.

## System status

Privileged view:
- API version.
- deployed commit.
- DB status.
- Redis.
- Celery heartbeat.
- storage.
- schema hash.
- environment.

## Forms

Use typed schema validation.

Server error codes map to field/global messages centrally.

## Testing

Playwright golden paths:
- admin login.
- review approve/reject.
- taxonomy activation.
- user role guard.
- ad creation.
- audit access.
