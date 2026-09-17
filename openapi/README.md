# OpenAPI contract

`schema.yaml` and `schema.sha256` are generated artifacts. They must be produced from Django/drf-spectacular with `scripts/generate-openapi.sh`; do not hand-edit them.

Client packages under `packages/api-*` are generated-only outputs. Human-authored domain/UI code must not duplicate transport DTOs.

P10 cannot be closed until schema generation and all three client generators run successfully from the same commit and CI reports zero drift.
