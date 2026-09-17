# API Contract Specification

Base:
```text
/api/v1/
```

## Conventions

JSON:
- camelCase at API boundary baseline.
- backend internal Python snake_case.
- one convention only.

Dates:
- ISO-8601 UTC timestamps.

IDs:
- UUID strings.

Coordinates:
```json
{"latitude":35.95,"longitude":39.01}
```

Error:
```json
{
  "code": "VALIDATION_ERROR",
  "message": "تعذر حفظ البيانات.",
  "details": {
    "phone": ["INVALID_PHONE"]
  },
  "requestId": "..."
}
```

Clients branch on `code`.

## Public

### GET `/public/provinces/`
Returns active public province metadata.

### GET `/public/provinces/{provinceId}/cities/`

### GET `/public/provinces/{provinceId}/categories/`
Safe category + capabilities only.

Example:
```json
{
  "items": [
    {
      "id": "...",
      "nameAr": "صيدليات",
      "nameEn": "Pharmacies",
      "iconKey": "pharmacy",
      "capabilities": {
        "hours": true,
        "ratings": true,
        "duty": true,
        "specialtyFilter": false,
        "serviceFilter": false
      }
    }
  ]
}
```

### GET `/public/home/`
Query:
```text
provinceId
latitude?
longitude?
```

Returns:
- ads.
- categories.
- nearby.
- openNearby.
- dutyNow if relevant.
- serverTime.

### GET `/public/facilities/`
Query:
```text
provinceId required
categoryId required
latitude?
longitude?
cityId?
neighborhoodId?
openNow?
dutyNow?
specialtyId?
serviceId?
search?
cursor?
limit?
```

### GET `/public/facilities/{id}/`

### GET `/public/map/facilities/`
Query:
- provinceId.
- bbox.
- category/filter context.

Return compact marker DTOs.

### GET `/public/search/`
Search across allowed scopes.

### GET `/public/ads/`
Optional if not embedded in home.

## Auth

### POST `/auth/register/start/`
Input:
```json
{
  "displayName": "...",
  "phone": "09...",
  "provinceId": "..."
}
```
Output challenge metadata.

### POST `/auth/register/verify/`

### POST `/auth/register/complete/`
Sets password and creates session.

Alternative combined flow allowed if OTP provider architecture prefers, but keep explicit state.

### POST `/auth/login/`
### POST `/auth/refresh/`
### POST `/auth/logout/`
### POST `/auth/logout-all/`
### GET `/auth/sessions/`
### DELETE `/auth/sessions/{id}/`

### Recovery
```text
POST /auth/recovery/start/
POST /auth/recovery/verify/
POST /auth/recovery/reset/
```

## Account

```text
GET   /account/profile/
PATCH /account/profile/
PUT   /account/profile-image/
DELETE /account/profile-image/
GET   /account/ratings/
POST  /account/deletion-request/
```

## Rating

```text
PUT    /facilities/{id}/rating/
DELETE /facilities/{id}/rating/
```

## Owner config

`GET /owner/config/?provinceId=...`

Returns:
- enabled categories.
- capabilities.
- verification requirements safe descriptors.

## Owner facilities

```text
GET    /owner/facilities/
POST   /owner/facilities/
GET    /owner/facilities/{id}/
PATCH  /owner/facilities/{id}/
POST   /owner/facilities/{id}/submit/
```

Subresources:
```text
PUT    /owner/facilities/{id}/location/
PUT    /owner/facilities/{id}/hours/
GET    /owner/facilities/{id}/images/
POST   /owner/facilities/{id}/images/
DELETE /owner/facilities/{id}/images/{imageId}/
POST   /owner/facilities/{id}/evidence/
DELETE /owner/facilities/{id}/evidence/{evidenceId}/
POST   /owner/facilities/{id}/temporary-closures/
DELETE /owner/facilities/{id}/temporary-closures/{closureId}/
GET    /owner/facilities/{id}/duty/
POST   /owner/facilities/{id}/duty/
PATCH  /owner/facilities/{id}/duty/{shiftId}/
DELETE /owner/facilities/{id}/duty/{shiftId}/
GET    /owner/facilities/{id}/members/
POST   /owner/facilities/{id}/members/
DELETE /owner/facilities/{id}/members/{userId}/
```

## Admin

Dashboard:
`GET /admin/dashboard/`

Applications:
```text
GET  /admin/applications/
GET  /admin/applications/{id}/
POST /admin/applications/{id}/approve/
POST /admin/applications/{id}/reject/
```

Evidence:
`GET /admin/evidence/{id}/content/`

Facilities:
```text
GET  /admin/facilities/
GET  /admin/facilities/{id}/
POST /admin/facilities/{id}/suspend/
POST /admin/facilities/{id}/reactivate/
POST /admin/facilities/{id}/close/
```

Users/roles:
```text
GET   /admin/users/
GET   /admin/users/{id}/
POST  /admin/users/{id}/block/
POST  /admin/users/{id}/unblock/
GET   /admin/roles/
PUT   /admin/users/{id}/roles/
```

Taxonomy:
```text
/admin/category-groups/
/admin/categories/
/admin/categories/{id}/capabilities/
/admin/provinces/
/admin/categories/{id}/provinces/
/admin/verification-requirements/
```

Ads:
`/admin/ads/`

Settings:
`/admin/settings/`

Audit:
`GET /admin/audit/`

Analytics:
`GET /admin/analytics/`

System:
`GET /admin/system/status/`

## Pagination

Baseline cursor:
```json
{
  "items": [],
  "nextCursor": "opaque-or-null",
  "hasMore": false
}
```

Admin data grids may use page-number pagination if better for operations; keep public API consistent within its family.

## OpenAPI generation

Django generates:
```text
openapi/schema.yaml
openapi/schema.sha256
```

Generated packages are read-only artifacts.

CI:
- regenerate.
- compare hash.
- fail drift.
