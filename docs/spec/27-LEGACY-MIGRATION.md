# Legacy V2 and Rebuild Strategy

The project may be started entirely from zero. Old code is not required as a runtime dependency.

If V2 exists, treat it as reference.

## Do not port blindly

Extract:
- product rules.
- Django domain logic that passes audit.
- migrations/data strategy if reusing DB.
- API lessons.
- tests.
- user flows.

Do not copy:
- React Native architecture.
- ad-hoc UI.
- demo map endpoints.
- temporary scripts.
- stale dependencies.

## Two valid execution modes

### Greenfield
New repository `directory-platform-v3`.

Use when:
- clean rebuild desired.
- existing production data not yet authoritative.
- less migration complexity.

### In-place platform evolution
Keep repository but replace mobile and refactor.

Use only if existing history/deploys are valuable.

The owner preference in this documentation is **greenfield-quality architecture** even if code is physically built in existing repository.

## Data migration

If old production/staging DB contains valuable data:
1. inventory.
2. map schemas.
3. write one-way import commands.
4. run validation counts.
5. preserve source backup.
6. never mutate source DB during trial import.

## React Native

Freeze as legacy reference.

Kotlin must be feature-parity tested before old mobile is deleted.
