# TypeScript API client

Generated from `openapi/schema.yaml` by openapi-generator 7.15.0.

Everything under `generated/` is a build artefact and is committed on purpose. Do not edit
it by hand: the next regeneration discards the change and the CI drift gate fails.

Regenerate with:

```bash
./scripts/generate-openapi.sh
./scripts/generate-api-clients.sh
```

This package is the only transport layer Admin and Public Web may use. Hand-written
DTOs that duplicate these models are a contract violation.
