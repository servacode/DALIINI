# Autonomous Claude Execution Protocol

هذه الوثيقة موجهة مباشرة إلى Claude/أي coding agent.

## Owner intent

The owner wants the project built comprehensively from scratch according to this documentation and does **not** want repeated “shall I continue?” prompts after every phase.

Proceed automatically after each successful gate.

## Before coding

Read all docs.

Then create:
```text
plan.md
PROJECT-STATUS.md
DECISIONS.md
EVIDENCE.md
```

Do not replace the Master Spec.

## Execution loop

For each phase:

```text
1. Inspect current repository
2. Map tasks to spec sections
3. Write phase plan
4. Implement
5. Run tests
6. Fix root causes
7. Run gate
8. Record evidence
9. Commit logical changes
10. Continue to next phase automatically
```

## No-question rule

Do not ask the owner for:
- framework choice.
- mobile technology.
- DB.
- category architecture.
- UI architecture.
- admin approach.
- map engine.
- realtime model.
- repo structure.
- test approach.

They are already decided.

## Allowed blockers

Only stop when a value/action is impossible to infer or perform, such as:
- actual purchased root domain missing.
- external provider credential missing.
- Play Console login/permission missing.
- payment approval required.
- irreversible external action lacks authorization in the environment.
- legal/business identity required by a store form and not available.

Even then:
- complete all code.
- use env placeholders.
- create exact operator checklist.
- continue other independent work.
- do not redesign architecture.

## Never do

- React Native.
- Flutter.
- fake Production data.
- demo map endpoint in Production config.
- hardcode secrets.
- print secrets.
- bypass failing tests.
- mark a gate PASS without running it.
- delete old data/repo without backup.
- weaken permission to make test pass.
- put evidence in public DTO.
- use Django Admin as operating UI.

## Git behavior

Before work:
```bash
git status
git branch --show-current
git rev-parse HEAD
```

Use logical commits.

Do not blindly `git add .` in a dirty repository without inspecting status.

## Root cause

When failure occurs:
- reproduce.
- identify layer.
- inspect logs.
- fix root cause.
- add regression test.
- rerun relevant superset.

## Documentation updates

When architecture changes:
- ADR required.
- Master Spec updated only when owner-approved baseline truly changes.

## Phase autonomy

If gate passes:
- update status.
- continue.

If gate fails:
- remain in phase until fixed or external blocker recorded.

## Completion

Do not say “project complete” until:
- Production infrastructure qualified.
- Android production release verified.
- iOS status accurately stated.
- monitoring live.
- backup restore proven.
- no P0/P1.
