# Data Model and Constraints

Use UUID primary keys for externally visible domain entities.

## User
Fields:
- id UUID.
- phone canonical unique.
- display_name.
- profile_province FK.
- status.
- phone_verified_at.
- profile_image_key private nullable.
- created_at.
- updated_at.

Indexes:
- unique normalized phone.
- status.
- profile province.

## UserSession
Fields:
- id.
- user.
- refresh_digest.
- previous_refresh_digest.
- previous_valid_until.
- platform.
- device_name.
- created_at.
- last_seen_at.
- revoked_at.
- compromised_at.

Indexes:
- user + revoked.
- last_seen.

## OTPChallenge
Fields:
- id.
- phone.
- purpose.
- otp_digest.
- attempt_count.
- max_attempts.
- expires_at.
- consumed_at.
- created_at.

No raw OTP storage.

## Province
- id.
- code unique.
- name_ar.
- name_en.
- is_active.
- sort_order.
- geometry optional MultiPolygon.

## City
- id.
- province.
- name_ar/en.
- is_active.
- geometry optional.

## Neighborhood
- id.
- city.
- name_ar/en.
- is_active.
- geometry optional.

## DirectoryCategoryGroup
- id.
- slug unique.
- name_ar/en.
- icon_key nullable.
- is_active.
- sort_order.

## DirectoryCategory
- id.
- group.
- code unique immutable.
- slug unique immutable.
- name_ar/en.
- description_ar/en.
- icon_key.
- specialization.
- is_active.
- sort_order.

## DirectoryCategoryProvince
- category.
- province.
- public_enabled.
- owner_registration_enabled.
- sort_order.

Constraint unique(category, province).

## CategoryCapabilities
One-to-one category:
- supports_hours.
- supports_photos.
- supports_ratings.
- supports_duty.
- supports_specialty_filter.
- supports_service_filter.
- supports_temporary_closure.
- supports_owner_onboarding.

Invariant:
- supports_duty allowed only for approved specialization/policy.

## VerificationRequirement
- id.
- category.
- label_ar/en.
- instructions_ar/en.
- required.
- active.
- min_files >= 0.
- max_files >= min_files.
- sort_order.

## Specialty
- id.
- category or specialization scope.
- name_ar/en.
- active.
- sort_order.

## ServiceTag
- id.
- category.
- name_ar/en.
- active.
- sort_order.

## Facility
- id.
- category.
- province.
- city nullable.
- neighborhood nullable.
- name_ar.
- name_en.
- description_ar/en.
- phone.
- address_ar/en.
- location Point geography/geometry SRID 4326.
- status.
- activated_at.
- created_at.
- updated_at.

Indexes:
- category/status/province.
- city.
- GIST location.
- normalized searchable text strategy.

## FacilityMembership
- facility.
- user.
- role OWNER|MANAGER.
- created_at.

Unique facility/user.

Protect last owner.

## FacilityApplication
- id.
- facility.
- kind INITIAL|REVERIFICATION.
- status DRAFT|SUBMITTED|APPROVED|REJECTED.
- submitted_at.
- reviewed_at.
- reviewed_by.
- rejection_reason.
- snapshot/diff references if used.
- timestamps.

Only one active submitted application of applicable kind per facility.

## FacilityPublicImage
- id.
- facility.
- storage_key.
- sort_order.
- width/height.
- created_at.

Public API never returns raw storage key; returns protected public media route or CDN URL created by service.

## VerificationEvidence
- id.
- facility.
- requirement.
- storage_key.
- uploaded_by.
- created_at.

Private.

## BusinessHour
Recommended normalized:
- facility.
- weekday 0..6.
- sequence.
- start_time nullable.
- end_time nullable.
- is_24h.
- is_closed record strategy.

Validate:
- no same-day overlaps.
- overnight semantics.
- deterministic order.

## TemporaryClosure
- id.
- facility.
- starts_at.
- ends_at.
- reason.
- created_by.
- cancelled_at.

## PharmacyDutyShift
- id.
- facility.
- starts_at.
- ends_at.
- created_by.
- cancelled_at.
- ended_early_at.

Use PostgreSQL exclusion constraint/range to prevent overlapping effective shifts for same facility.

## Rating
- id.
- user.
- facility.
- stars CHECK 1..5.
- timestamps.

Unique(user, facility).

## Advertisement
- id.
- image_key.
- title_ar/en optional.
- subtitle_ar/en optional.
- action_type.
- action_payload validated.
- target_scope GLOBAL|PROVINCE|CATEGORY.
- province nullable.
- category nullable.
- starts_at.
- ends_at.
- enabled.
- sort_order.
- slide_duration_ms bounded.
- timestamps.

## Notification
- id.
- user.
- type.
- payload safe JSON.
- read_at.
- created_at.

## DevicePushToken
- id.
- user/session.
- platform.
- token encrypted or protected.
- active.
- last_seen.
- created_at.

## PlatformSetting
Typed setting:
- key.
- type.
- serialized value.
- updated_by.
- timestamps.

## AuditLog
- id.
- actor.
- action.
- resource_type.
- resource_id.
- before_snapshot redacted.
- after_snapshot redacted.
- request_id.
- metadata redacted.
- created_at.

Append-oriented.

## SearchAnalytics / ProductAnalytics
Prefer aggregated/event pipeline with privacy minimization rather than coupling core DB to analytics forever.

## AccountDeletionRequest
- id.
- user nullable.
- phone/email lookup information minimized.
- channel IN_APP|WEB.
- status.
- requested_at.
- verified_at.
- completed_at.
- retention_notes.
- audit linkage.

## Referential/lifecycle rules

- deleting province/category with referenced facilities should be disallowed; deactivate instead.
- evidence deletion policy depends on application lifecycle/retention.
- suspending facility removes it from public discovery.
- closed facility removes it unless archival public policy later added.
- inactive category/province removes public eligibility without deleting facility data.
