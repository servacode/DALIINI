# OpenAPI contract

Django and DRF are the executable source of truth. This document is generated from them,
and the three client packages are generated from this document. No client hand-writes a
transport DTO.

```
Django + DRF  ->  openapi/schema.yaml  ->  packages/api-typescript
                                           packages/api-kotlin
                                           packages/api-swift
```

## Canonical artefacts

| Path | What it is |
|---|---|
| `openapi/schema.yaml` | The contract. Generated, committed, never hand-edited. |
| `openapi/schema.sha256` | Digest of the document above. Generated, committed. |
| `packages/api-typescript/generated/` | Generated TypeScript client. |
| `packages/api-kotlin/generated/` | Generated Kotlin client. |
| `packages/api-swift/generated/` | Generated Swift client. |

Everything in the table is a build artefact. Editing any of it by hand is a defect: the
next regeneration silently discards the edit and CI fails on drift.

## Generated code is committed

The clients are committed rather than produced during each consumer build. That way CI can
prove the committed clients still match the schema, a reviewer can see a contract change in
the diff of the pull request that causes it, and Admin, Android and iOS can build without
running a code generator. See DECISION-010 in `DECISIONS.md`.

## Official commands

```bash
./scripts/generate-openapi.sh        # regenerate schema.yaml and schema.sha256
./scripts/generate-api-clients.sh    # regenerate the three clients from schema.yaml
./scripts/check-openapi-drift.sh     # fail if the committed contract is stale
```

`generate-api-clients.sh` pins openapi-generator `7.15.0` and runs it from the official
image when no local CLI of that exact version is present, so the output is reproducible on
any machine and in CI.

Generation requires the GeoDjango native libraries (`gdal-bin`, `libgdal-dev`,
`libgeos-dev`), because the models are spatial. CI installs them; locally the backend
container already has them.

## Rule when the contract changes

1. Change the Django code.
2. Run `./scripts/generate-openapi.sh`.
3. Run `./scripts/generate-api-clients.sh`.
4. Commit the source change together with the regenerated artefacts.

`./scripts/check-openapi-drift.sh` and the CI `contract-drift` job fail otherwise.

## Contract conventions

**Operation ids** follow `<area><Resource><Action>` in lowerCamelCase, for example
`publicFacilitiesList`, `ownerFacilitySubmit`, `adminReviewApprove`. They are declared
explicitly on every operation, so renaming a Python class never changes the public
contract. A contract test rejects duplicates and hash-suffixed names.

**Authentication** is the `bearerAccessToken` HTTP bearer scheme carrying the short-lived
access token. The opaque rotating refresh secret is deliberately not described as a bearer
credential; it travels in the refresh request body.

**Errors** are one component, `ApiError`, for every failure: validation, authentication,
permission, not found, method, media type, domain conflict, throttling and unexpected
server errors alike. It carries `code`, `message`, `details` and `requestId`, exactly as
`08-API-CONTRACT.md` specifies. Clients branch on `code` and never on `message`. `details`
is a map of field path to messages, empty when the error is not field-scoped. `requestId`
is the same value as the `X-Request-ID` response header. Every endpoint can also answer
`500` with this component; it is not listed per operation to keep the document readable.

`DetailError`, `DomainError` and `DomainErrorBody` no longer exist. They described shapes
the runtime emitted before `core.exceptions.exception_handler` was installed as the DRF
`EXCEPTION_HANDLER`.

**Pagination** is one envelope, `{items, nextCursor, hasMore}`, produced by
`core.pagination.CursorPage`. `nextCursor` is an opaque token, not a URL; it is sent back
unchanged as the `cursor` query parameter and must not be parsed. A malformed cursor is a
`400` with `details.cursor`, not a `404`.

**Casing** is camelCase at the boundary without exception, including endpoints that read
through `QuerySet.values()`. A wire name that differs from its database column is
translated in the serializer with `source=`; a column is never renamed to change a wire
name. `AdminCapabilities*` is the one remaining snake_case pair and is tracked as INT-039.
