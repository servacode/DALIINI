# ADR-001 — The root holds the project's entrance, not its working papers

**Status:** Accepted
**Date:** 2026-09-25
**Owners:** Serva Code

## Context

`docs/spec/26-REPOSITORY-STRUCTURE.md` placed nine documents at the repository root: the plan,
the status, the implementation log, the decisions, the blockers, the handover, the evidence
table, the README and the security policy. `scripts/check-governance.mjs` fails the build when
one of them is missing, so the layout was enforced rather than merely described.

Seven of the nine are working papers: a plan that changes weekly, a status, a log of what was
done, a register of decisions, a register of blockers, a handover, and a table of evidence.
They are read by whoever is building the thing. The other two — `README.md` and `SECURITY.md` —
are read by whoever arrives: GitHub itself looks for them, renders the first on the repository
page and links the second from the security tab.

Opening the repository showed fourteen files before the first folder, which is what prompted
this. A root that lists the project's diary is a root that answers "what is going on here?"
before it answers "what is this?".

## Decision

The working papers move to `docs/project/`. `README.md` and `SECURITY.md` stay at the root
with the files tooling reads there: the workspace manifests, the lock file, the editor and git
configuration, the environment example, the language pins and the deployment descriptor.

`scripts/check-governance.mjs` requires them at their new paths, so the set is still enforced —
this moves the documents, it does not make any of them optional.

`docs/spec/26-REPOSITORY-STRUCTURE.md` is amended to match, and its checksum in
`docs/spec/SHA256SUMS.txt` and `docs/spec/MANIFEST.md` regenerated, together with the copy of
that section inside `SERVA-CODE-DIRECTORY-V3-ALL-IN-ONE.md`. The checksums exist so that an
edit to the specification is deliberate and recorded; this ADR is that record.

## Alternatives considered

**Leave the layout alone.** The specification would stay untouched and the root would stay as
it is. Rejected: the question was asked about the root, and "the specification says so" is a
reason to record a decision, not a reason to keep a layout nobody would choose today.

**Move the documents and leave the specification stale.** The ADR would supersede §26 and the
checksums would still verify. Rejected: a specification that describes a tree the repository
does not have is worse than one that is edited in the open. The next reader trusts it, and it
lies to them.

**Rename the documents while moving them** (`STATUS.md`, `LOG.md`, and so on). Rejected: they
are cited by name in the specification, in the evidence and in code comments. The gain is
cosmetic and the cost is every one of those citations.

## Why

A repository root is an index. Everything in it is claiming to be something a newcomer, a build
tool or a platform needs immediately. Seven of these nine were claiming that and none of them
could support it.

## Consequences

### Positive

- The root lists six files and eight folders, and each one answers "what is this project" or
  "how is it built".
- The working papers sit together in `docs/project/`, beside `docs/spec`, `docs/adr`,
  `docs/design` and `docs/runbooks`, which is where a reader already looks for documents.
- The governance gate still fails if one of them disappears.

### Negative

- Every citation of the seven had to be rewritten, and links in old evidence files now point at
  paths that no longer exist. The evidence is a record of what was true when it was written and
  is left as it was; this ADR is what explains the difference.
- The specification package no longer matches the bytes that were delivered. Its checksums were
  regenerated deliberately, and this file is the reason they changed.
