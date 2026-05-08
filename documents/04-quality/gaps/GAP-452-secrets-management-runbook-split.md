# GAP-452: Split `secrets-management-runbook.md` into seeding + rotation per deployment-naming-convention §8

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Documentation / Governance
**Found:** 2026-05-09 (deployment-naming cleanup PR — partial deferral)
**Affects:** `documents/05-guides/operations/secrets-management-runbook.md` (single 362-line file covering both lifecycle phases)

## Problem

Per `.claude/rules/deployment-naming-convention.md` §8 edge case row "Runbook covers BOTH initial setup + recurring rotation", the file should be split:

> Split into 2 files; `deploy/<topic>-setup-runbook.md` + `operations/<topic>-rotation-runbook.md` với cross-link

Current `documents/05-guides/operations/secrets-management-runbook.md` covers both:

| Section | Scope | Target folder per rule §2 |
|---|---|---|
| §1 Architecture overview | Reference | shared (both) |
| §2 Secrets inventory | Reference | shared (both) |
| §3 Provisioning — first-time setup | One-time per release | `deploy/` |
| §4 IAM policy template — service access | Reference | shared (both) |
| §5 Rotation procedures | Recurring per cadence | `operations/` |
| §6 Audit + compliance | Recurring | `operations/` |
| §7 Spring Boot integration | Reference | shared (both) |
| §8 Cost estimate | Reference | shared (both) |
| §9 Acceptance criteria | One-time | `deploy/` |

Today, anyone provisioning secrets first-time has to read past rotation/audit sections; anyone doing rotation has to read past §3 setup that doesn't apply to ongoing rotation. Mix of audiences = drift on both sides.

## Root Cause

Original runbook (Wave 33 Bucket B closure GAP-379) shipped before `deployment-naming-convention.md` rule existed (2026-05-08). Rule §8 added explicit edge-case guidance for split. Cleanup PR 2026-05-09 deferred this split to follow-up because substantive editorial scope > simple file relocation.

## Proposed Fix

### Phase 1 — Create `deploy/secrets-seeding-runbook.md`

Extract sections relevant to one-time seeding:
- New §1 Audience + Scope — "first-time provisioning during release deploy"
- Inline reference §2 Architecture (1-paragraph summary + link to operations/secrets-rotation-runbook.md for full architecture)
- Original §2 Secrets inventory (full)
- Original §3 Provisioning — first-time setup (full)
- Original §4 IAM policy template (full — applies at seeding too)
- Original §9 Acceptance — first-time seed verification

### Phase 2 — Rename + edit `operations/secrets-management-runbook.md` → `operations/secrets-rotation-runbook.md`

Sections kept:
- §1 Architecture overview (full — canonical home for arch ref)
- §2 Secrets inventory (kept — both runbooks need; cross-link to seeding)
- §4 IAM policy template (kept — applies to rotation too)
- §5 Rotation procedures (full)
- §6 Audit + compliance (full)
- §7 Spring Boot integration (full)
- §8 Cost estimate (full)
- New §9 Acceptance — rotation cadence verification

Sections REMOVED (moved to `deploy/secrets-seeding-runbook.md`):
- §3 Provisioning first-time setup
- §9 first-time seed AC

### Phase 3 — Cross-link both files

Each file's frontmatter + §1 Audience explicitly cross-links the sister:
- `deploy/secrets-seeding-runbook.md` says "for ongoing rotation, see `operations/secrets-rotation-runbook.md`"
- `operations/secrets-rotation-runbook.md` says "for first-time seeding during release deploy, see `deploy/secrets-seeding-runbook.md`"

### Phase 4 — Update consumers

Find references to the old file:
```bash
grep -rln "operations/secrets-management-runbook" --include="*.md"
```

Each reference re-routed to the appropriate sister: deploy (if context is initial deploy) or operations (if context is rotation/audit).

## Acceptance Criteria

- [ ] `documents/05-guides/deploy/secrets-seeding-runbook.md` exists, covers §3 Provisioning + §4 IAM seeding scope + frontmatter
- [ ] `documents/05-guides/operations/secrets-rotation-runbook.md` exists (renamed from `secrets-management-runbook.md`), covers §5+§6+§7+§8 + retains §1+§2+§4 references
- [ ] Both files cross-link in frontmatter + §1 Audience
- [ ] All references to old `secrets-management-runbook.md` redirected (zero `operations/secrets-management-runbook` greps)
- [ ] `.claude/rules/deployment-naming-convention.md` §6 Other drift candidates row 2 updated from ⏳ → ✅ DONE
- [ ] Verification artifact saved per `agent-aws-access.md` §5

## Effort estimate

~45-60min — substantive editorial work (not pure file relocation). Read full source, partition sections, write 2 new file frontmatters, cross-link, sweep references.

## Related

- Rule: `.claude/rules/deployment-naming-convention.md` §8 edge case (split mandate)
- Parent cleanup PR: 2026-05-09 cleanup batch (deferred this scope)
- Original runbook: GAP-379 Wave 33 Bucket B closure
- Sister rule: `.claude/rules/release-deploy-standard.md` §3.1 Secrets management

## Log

- **2026-05-09:** Filed by deployment-naming cleanup PR — Phase 1-3 of cleanup batch shipped (3 file moves + 14 link updates). Secrets-management split deferred here due to editorial scope. Tagged P2 (governance hygiene, not blocking Phase 1 BETA).
