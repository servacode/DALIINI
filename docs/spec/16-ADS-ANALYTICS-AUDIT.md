# Ads, Analytics and Audit

## Ads

Model:
- image.
- Arabic/English copy.
- action.
- target.
- schedule.
- active.
- order.
- duration.

Action types:
```text
NONE
FACILITY
CATEGORY
EXTERNAL_URL (allow-list / validation)
IN_APP_ROUTE
```

External URLs validated; avoid arbitrary dangerous schemes.

## Ad analytics

Optional events:
- impression.
- click.

No third-party ad SDK required for first-party/admin-managed slider.

If commercial/paid ads are displayed, Play declarations must accurately reflect app behavior.

## Analytics

Central event registry.

Fields must be documented for each event.

Baseline events:
```text
app_open
home_view
province_selected
location_permission_result
category_open
search_submitted
search_zero_results
facility_view
map_open
marker_open
phone_tap
directions_start
rating_submit
owner_draft_create
owner_submit
application_status_view
```

## Privacy

Avoid:
- raw precise location in generic analytics.
- evidence identifiers.
- OTP.
- token.
- full phone.

## Audit

Different from analytics.

Audit is security/operations record.

Must audit:
- role changes.
- blocks.
- evidence view.
- approvals/rejections.
- suspend/reactivate.
- taxonomy changes.
- province activation.
- settings.
- ads changes.
- retention purge.

## Retention

Analytics retention configurable.

Audit retention longer according to security/operations policy.

Purge action permission-separated and audited.
