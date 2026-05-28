---
id: GAP-789
title: META — Wave A Bucket B post-merge audit suite + 01-business doc refresh (3-day deadline)
status: OPEN
priority: P1
domain: Meta
phase: phase-1-beta
found_date: 2026-05-28
last_verified: 2026-05-28
---

# GAP-789 — META Wave A Bucket B post-merge audit suite + 01-business doc refresh

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Meta
**Phase:** phase-1-beta
**Found:** 2026-05-28 (post-merge `audit-gate.py` hook flagged 5 compliance violations on PR #1929 merge to main HEAD)
**Deadline:** 2026-05-31 (3 ngày post-merge per `.claude/rules/post-wave-audit-mandate.md` §2.2)

## Problem

PR #1929 (Wave A Bucket B — re-host staff invitations canonical to kitehub-subscription) đã merge vào main HEAD `8d8028d8` ngày 2026-05-28 09:17 UTC. `audit-gate.py` hook chạy post-merge phát hiện **5 compliance violations** mà PR closure không address inline:

1. **Missing audits** — `business-logic-audit` + `api-contract-audit` chưa được chạy refresh sau khi code shipped to main (per `.claude/rules/post-wave-audit-mandate.md` §2.1 file-pattern matrix: thay đổi `Controller.java` + `Service.java` + `application.yml` → trigger 2 audits này)
2. **Business logic changed but no 01-business/ docs updated** — staff invitation logic moved kiteclass-core → kitehub-subscription canonical; `documents/01-business/kitehub/staff-invitations/` (rules.md + use-cases.md + api-contract.md per Living Docs 3-layer convention) chưa refresh
3. **CI status unknown** — cosmetic post-merge audit issue (hook không observe CI run completion at merge time)
4. **GAP-786 Log doc drift** — đã sync separate docs-only PR cùng session (paired với GAP-789 filing — this PR)
5. **Wave merge — run /wave-completion-check** — within 3 days

PR #1929 đã merge sạch (43/44 SUCCESS, 0 FAILING, mergeStateStatus CLEAN). Compliance violations là **post-merge cadence work**, không phải merge-time block. 3-day window per `post-wave-audit-mandate.md` §2.2 = deadline **2026-05-31**.

## Root Cause

- PR #1929 scope = Wave A Bucket B re-host + 4 CI fix bundle; coordinator focused on getting CI green + merging trong session limit window
- Post-merge audit suite (business-logic + api-contract) là cadence-driven work per rule, không phải PR-time gate — cho phép defer 3 ngày
- Living Docs 3-layer (per CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure") mandate doc-and-code-same-PR, nhưng staff invitation 3-layer docs có thể chưa tồn tại trước đó (kiteclass-core domain) → cần verify state-check
- `audit-gate.py` hook đúng — codify cadence enforcement, không phải false positive

## Proposed Fix

### Phase 1 — Audit suite refresh (P1, deadline 2026-05-31)

- [ ] Run `business-logic-audit` skill against current main HEAD scope (kitehub-subscription/staff/**, kitehub-frontend admin staff pages)
- [ ] Run `api-contract-audit` skill against same scope
- [ ] Save reports to `documents/04-quality/audits/business-logic/2026-05-28-wave-a-bucket-b-post.md` + `documents/04-quality/audits/api-contract/2026-05-28-wave-a-bucket-b-post.md`
- [ ] Add 2 rows to `documents/04-quality/audits/audits-index.csv` per `meta-csv-index-pattern.md`
- [ ] File P0/P1 findings as new gaps per `audit-to-gap-pipeline.md` §3 if surfaced

### Phase 2 — 01-business docs refresh (P1, same deadline)

- [ ] State-check: `find documents/01-business -type d -iname "*staff*"` — verify if 3-layer docs exist for staff invitations
- [ ] If absent: create `documents/01-business/kitehub/staff-invitations/{rules.md,use-cases.md,api-contract.md}` reflecting canonical kitehub-subscription scope (BR-ROLE-INVITE-IDEMPOTENT, STAFF_MAX_PER_TENANT=50, INVITATION_TTL_DAYS=14, etc.)
- [ ] If existing kiteclass-core docs: move/refactor to kitehub-subscription scope; add deprecation note pointing to V72 Flyway migration
- [ ] Update `documents/01-business/README.md` index if new domain added

### Phase 3 — Wave completion check

- [ ] Run `/wave-completion-check` against Wave A Bucket B scope per `.claude/skills/workflow/wave-completion-check.md`
- [ ] Verify Level 7 (audit suite gate) per `post-wave-audit-mandate.md`
- [ ] Update `wave-history.jsonl` if entry not yet final

## Acceptance Criteria

- [ ] 2 audit reports shipped + indexed in audits-index.csv (Phase 1)
- [ ] 01-business/kitehub/staff-invitations/ 3-layer docs reflecting canonical scope (Phase 2)
- [ ] Wave completion check Level 7 PASS (Phase 3)
- [ ] `audit-gate.py` re-run shows compliance ≥ 4/5 (CI status cosmetic exempt)
- [ ] GAP-789 Log entry references audit reports + business doc PR refs

## Related

- **PR #1929** — Wave A Bucket B canonical re-host (merged main HEAD `8d8028d8` 2026-05-28)
- **GAP-786** — Wave A Bucket B parent gap (DONE; sister to this follow-up)
- **GAP-787** — Bug #14 email path (sister, OPEN — different scope)
- **GAP-788** — META Wave 80+ retro-walk batch (sister meta-tracking)
- **Rule**: `.claude/rules/post-wave-audit-mandate.md` §2.1 + §2.2 — 3-day audit suite cadence
- **Rule**: `.claude/rules/audit-to-gap-pipeline.md` §3 — audit findings → new gaps
- **Rule**: CLAUDE.md §"Business Logic Documents — 3-Layer Structure" — doc-and-code-same-PR mandate
- **Hook log**: `documents/03-planning/pr-logs/PR-1929.json` (audit-gate.py compliance 1/5)

## Log

- **2026-05-28** — Filed in response to `audit-gate.py` post-merge hook flagging 5 violations on PR #1929 squash merge. P1 because audit cadence work, not P0 incident; 3-day window per `post-wave-audit-mandate.md`. Tracks Phase 1 (2 audits) + Phase 2 (01-business docs) + Phase 3 (wave completion check). Deadline 2026-05-31. Paired same docs-only PR with GAP-786 Log sync.
