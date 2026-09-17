# Repository Structure and Tooling

```text
directory-platform-v3/
├── apps/
│   ├── backend/
│   ├── admin/
│   ├── web/
│   ├── android/
│   └── ios/
├── packages/
│   ├── design-tokens/
│   ├── api-typescript/
│   ├── api-kotlin/
│   └── api-swift/
├── openapi/
├── infrastructure/
│   ├── docker/
│   ├── render/
│   ├── scripts/
│   └── runbooks/
├── docs/
│   ├── product/
│   ├── architecture/
│   ├── security/
│   ├── design/
│   ├── operations/
│   ├── release/
│   └── adr/
├── artifacts/
│   └── evidence/
├── .github/
│   └── workflows/
├── plan.md
├── PROJECT-STATUS.md
├── README.md
└── SECURITY.md
```

## Monorepo tooling

Web packages:
- pnpm workspace.

Python:
- uv inside backend.

Android:
- Gradle wrapper owned in `apps/android`.

iOS:
- Xcode project/workspace + SPM.

No requirement that Kotlin/Swift participate in pnpm workspace.

## Root commands

Optional wrapper scripts:
```text
scripts/dev-backend
scripts/dev-admin
scripts/dev-web
scripts/test-all
scripts/qualify-staging
scripts/build-android-release
```

Windows PowerShell equivalents if development primarily Windows.

## Files

`plan.md`:
current autonomous execution plan.

`PROJECT-STATUS.md`:
phase state and evidence.

`docs/adr/`:
architecture decisions.

`artifacts/evidence/`:
generated logs not necessarily all committed; manifest/hashes can be committed.

## Formatting

`.editorconfig` controls:
- UTF-8.
- LF baseline.
- final newline.
- indentation.

Arabic files must remain UTF-8 without accidental encoding conversion.
