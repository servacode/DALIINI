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
│   ├── project/
│   ├── spec/
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
├── README.md
└── SECURITY.md
```

The working papers — `docs/project/plan.md`, `docs/project/PROJECT-STATUS.md`, `docs/project/IMPLEMENTATION-LOG.md`, `docs/project/DECISIONS.md`,
`docs/project/BLOCKERS.md`, `docs/project/HANDOFF.md` and `docs/project/EVIDENCE.md` — live in `docs/project/`. They were at the root
until ADR-001 (`docs/adr/ADR-001-repository-root-layout.md`) moved them: the root answers what
the project is and how it is built, and the diary of building it belongs with the other
documents. `scripts/check-governance.mjs` requires all nine, at these paths.

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

`docs/project/plan.md`:
current autonomous execution plan.

`docs/project/PROJECT-STATUS.md`:
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
