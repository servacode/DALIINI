# Kotlin Multiplatform API client

Generated from `openapi/schema.yaml` by openapi-generator 7.15.0, with the `multiplatform`
library: Ktor for the transport, kotlinx.serialization for the bodies, kotlinx-datetime for
days and times, and `kotlin.time.Instant` for every moment (DECISION-090). Its package is
`com.servacode.directory.api.multiplatform`, so it never collides with the JVM client's. Only the sources are
generated; the docs and test stubs the generator would write beside them are switched off.

Everything under `generated/` is a build artefact and is committed on purpose. Do not edit it
by hand: the next regeneration discards the change and the CI drift gate fails.

Regenerate with:

```bash
./scripts/generate-openapi.sh
./scripts/generate-api-clients.sh
```

`apps/android/core/api` compiles it for Android and iOS. It is the transport the code shared
with the iPhone app is built on; the Android app still talks to the server through the JVM
client in `packages/api-kotlin`. Hand-written DTOs that duplicate these models are a contract
violation here too.
