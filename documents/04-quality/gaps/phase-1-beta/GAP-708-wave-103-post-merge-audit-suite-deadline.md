---
gap_id: GAP-708
title: Wave 103 post-merge audit suite — api-contract + ops-readiness within 3-day deadline 2026-05-25
status: OPEN
priority: P1
domain: Meta
phase: phase-1-beta
completion_pct: 0
filed_date: 2026-05-22
last_updated: 2026-05-22
filed_by: audit-gate.py post-merge hook flagged Wave 103 PR #1709
deadline: 2026-05-25
---

# GAP-708 — Wave 103 post-merge audit suite deadline

## Problem

Wave 103 PR #1709 merged 2026-05-22 04:51 UTC. `audit-gate.py` post-merge hook flagged:

> "Missing audits: api-contract-audit, ops-readiness-audit"
> "Wave merge — run /wave-completion-check + audit suite within 3 days (post-wave-audit-mandate.md)"

Per `post-wave-audit-mandate.md` §2.2 freshness window: 3 days after wave merge → audit suite MUST run. Deadline: **2026-05-25**.

## Context

Wave 103 touched:
- BE Java code (`kitehub-admin/AdminController.java` @PreAuthorize annotation)
- `docker-compose.kitehub.yml` (REDIS_HOST + REDIS_PORT env additions)
- Wave 103 = mixed domain (admin RBAC + email + 2FA + tenant init + AWS rebuild SOP) — does NOT qualify for §2.4 domain-milestone deferral (multi-domain scope)

Per §2.1 file-pattern matrix:
- `Controller.java` change → **api-contract /100** required
- `docker-compose*.yml` change → **ops-readiness /100** required

## Proposed Fix

Run 2 audits within 3 days (deadline 2026-05-25):

```bash
# Audit 1: API Contract /100
# Triggered by AdminController.java + 5 wave-103 audit doc citations
# Skill: .claude/skills/quality/api-contract-audit/SKILL.md
# Output: documents/04-quality/audits/api/2026-05-XX-wave-103-post-merge-api-contract.md

# Audit 2: Ops Readiness /100
# Triggered by docker-compose.kitehub.yml + branding REDIS env fix
# Skill: .claude/skills/quality/ops-readiness-audit/SKILL.md
# Output: documents/04-quality/audits/ops-readiness/2026-05-XX-wave-103-post-merge-ops-readiness.md
```

## Acceptance Criteria

- [ ] api-contract /100 audit report shipped before 2026-05-25
- [ ] ops-readiness /100 audit report shipped before 2026-05-25
- [ ] Findings filed as follow-up gaps per `audit-to-gap-pipeline.md` §3
- [ ] `documents/04-quality/audits/audits-index.csv` updated with 2 new rows
- [ ] `output-review-mandate.md` §3 rows refreshed if score changed
- [ ] Hook compliance check on next PR shows 5/5 score (post-audit ship)

## Related

- Wave 103 PR #1709 commit `345b4c0b` (audit-gate hook trigger)
- `post-wave-audit-mandate.md` §2.1 file-pattern matrix + §2.2 freshness window
- `audit-to-gap-pipeline.md` §3 audit findings → gap pipeline
- Wave 103 plan `documents/03-planning/waves/wave-2026-05-22-103-local-self-test-full-walk.md`
- Sister: GAP-709 (01-business docs sync from Wave 103 findings)
