---
id: GAP-782
title: Wave meta-6 post-merge follow-ups — state-coverage drift + audit suite + test coverage + business doc
status: OPEN
priority: P1
domain: Meta
phase: phase-1-beta
found_date: 2026-05-27
last_verified: 2026-05-27
---

# GAP-782 — Wave meta-6 post-merge follow-ups (audit suite + state-coverage drift + missing test/business-doc)

## Problem

Wave meta-6 merged 4 PRs (#1900 plan, #1902 plan patch, #1903 rule v1.0.1, #1904 BE staff invite, #1901 RST HTML dashboard pending) but multiple post-merge governance items deferred:

### Bucket A — PRs #1904 + #1901 audit-gate hook violations (post-merge)

Hook flagged 6 violations on `gh pr merge 1904` completion + 4 on `gh pr merge 1901 --admin`:

| # | Violation | Required action |
|---|---|---|
| 1 | CI status unknown | N/A (transient Trivy NEUTRAL — info-only per `release-fix-retry-budget.md` staging.7 redesign) |
| 2 | 11 Java files, 0 test files | File follow-up: integration test cho `StaffInvitationServiceImpl` + `StaffInvitationController` (P1) |
| 3 | Business logic changed, no `01-business/` docs updated | Add `documents/01-business/kiteclass/staff-invitation/{rules,use-cases,api-contract}.md` per Living Docs rule (P1) |
| 4 | Missing audits: `api-contract-audit` | Run `.claude/skills/quality/api-contract-audit/SKILL.md` on new endpoints `POST /api/v1/staff/invitations` + `POST /api/v1/staff/invitations/{token}/accept` (P1) |
| 5 | Gap doc drift — GAP-772 PR refs gap but doesn't update gap file Log | Add Log entry to GAP-772 referencing PR #1904 closure (Bucket A this gap covers) |
| 6 | Wave merge — run `/wave-completion-check` + audit suite within 3 days | Per `post-wave-audit-mandate.md` §3 day-3 hard stop — deadline 2026-05-30 |
| 7 | PR #1901 — 1 script syntax review (`scripts/render-rst-screenshots.sh`) | Run shellcheck + peer review per `output-review-mandate` §3 row "Scripts" |
| 8 | PR #1901 — UI kits integration smoke test confirmation missing (3 files under ui_kits/) | Per `GAP-265` requirement: open landing → click each card → sample 3 screens per kit. Confirm post-merge OR add `INTEGRATION_OK_NO_LANDING_CHANGE` rationale to closure note. |
| 9 | PR #1901 — Wave merge audit suite (same as #6) | Already covered by item 6 — single 3-day SLA deadline |

### Bucket B — Pre-existing state-coverage drift unmasked by PR #1901

`.claude/skills/quality/ui-review-prototype/scripts/state-coverage.sh` reports 4 kits missing minimum state coverage (pre-existing on main HEAD `50481e63`, NOT caused by Wave meta-6 PRs):

- `kiteclass-student` — missing `default` state file
- `kitehub-admin` — missing `default` state file
- `kitehub-story-v2` — missing `default` state file + missing all of {loading, empty, error}

Drift was latent because previous PRs' landing-parity Tier 2 step always failed first (script short-circuits → state-coverage step skipped). PR #1901's landing-parity fix unmasked the state-coverage failure.

## Proposed Fix

### Bucket A — within 3 days (2026-05-30)

1. Run `.claude/skills/quality/api-contract-audit/SKILL.md` on Wave meta-6 endpoints; file findings as audit report under `documents/04-quality/audits/api-contract/2026-05-30-wave-meta-6-staff-invite.md`
2. Add `documents/01-business/kiteclass/staff-invitation/{rules,use-cases,api-contract}.md` 3-layer
3. Write `StaffInvitationServiceImplTest` (Testcontainers Postgres per `postgres-specific-type-testcontainers.md`) + `StaffInvitationControllerIT`
4. Update GAP-772 `## Log` section with `- 2026-05-27 DONE via PR #1904`
5. Run `/wave-completion-check` + Audit suite per `post-wave-audit-mandate.md` (security + ops + quality + UI + business + performance)

### Bucket B — Wave-future (P2 scope)

For each missing-state kit, add 1+ state files matching `<screen>-default.html` or `default.html` pattern in `screens/`:
- `kiteclass-student/screens/dashboard-default.html` (or rename existing closest file)
- `kitehub-admin/screens/dashboard-default.html`
- `kitehub-story-v2/screens/loading.html` (covers the {loading|empty|error} requirement)

Defer to dedicated wave (no Mảng A RST scope overlap).

## Acceptance Criteria

- [ ] GAP-772 `## Log` updated with PR #1904 reference
- [ ] api-contract-audit shipped (Bucket A item 1)
- [ ] 3-layer business docs added (Bucket A item 2)
- [ ] Service + Controller IT shipped (Bucket A item 3)
- [ ] /wave-completion-check + audit suite within 3 days
- [ ] State-coverage drift fix scope filed as separate gap or Wave-future scheduled

## References

- PR #1904 (merged 2026-05-27 — Bucket A staff invite)
- PR #1901 (in review — Bucket C RST HTML, state-coverage drift unmasked)
- `documents/03-planning/waves/wave-2026-05-27-meta-6-fix-p0-meta-update-rst-html.md`
- `.claude/rules/post-wave-audit-mandate.md` (3-day hard stop)
- `.claude/rules/audit-to-gap-pipeline.md`
- `documents/03-planning/pr-logs/PR-1904.json` (hook output 0/5 compliance)

## Log

- 2026-05-27 GAP filed — consolidated follow-up post-Wave-meta-6 merge per audit-gate.py hook flag on PR #1904 + state-coverage pre-existing drift unmasked by PR #1901 parity fix.
