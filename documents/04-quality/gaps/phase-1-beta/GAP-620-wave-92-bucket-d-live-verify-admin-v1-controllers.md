# GAP-620: Wave 92 Bucket D — live verify 3 admin v1 controllers post-AWS-restore

**Status:** 🔵 OPEN (gated GAP-612 AWS restoration)
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-05-18 (Wave 92 closure scope-completeness audit per `wave-closure-scope-completeness.md` §3)
**Affects:** Wave 92 Bucket D acceptance — 3 admin v1 controller endpoints shipped nhưng chưa live-verify; Wave 90 walkthrough sub-finding 404 fix unconfirmed

## Problem

Wave 92 Bucket D ship 3 admin v1 controllers (PR #1514):
- `AdminInstancesController` @ `/api/v1/admin/instances` (list + detail)
- `AdminPaymentsController` @ `/api/v1/admin/payments` (pending list + summary)
- `AdminRevenueController` @ `/api/v1/admin/revenue` (full report + summary)

Bucket D plan §3 Acceptance Criteria:
> - [ ] 3 controller smoke tests pass
> - [ ] `curl /api/v1/admin/{instances,payments,revenue} → 200 (not 404)` post-deploy

Unit tests PASS (14 Mockito tests). NHƯNG **live smoke `curl … → 200 post-deploy` KHÔNG được verify** vì GAP-612 AWS account suspension chặn deploy. Closure status: complete dù live verify orphan.

Per `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist: live verify cần (a) credential available + (b) login API + (c) login UI + (d) role-guard accept + (e) navigation path + (f) page render + (g) action success. Wave 92 Bucket D shipped code-level chỉ (b)-(d) covered; (a)+(e)+(f)+(g) live verify defer.

## Root Cause

Wave 92 plan ban đầu offline-safe design (per plan §Trigger "OFFLINE-SAFE design: Mọi bucket touch code/docs/rules — KHÔNG cần AWS active"). Live verify Bucket D inherently AWS-active → necessarily defer until AWS restore.

NHƯNG closure PR #1517 flipped wave status: complete dù live verify mandatory per Bucket D AC #5. → orphan item.

## Proposed Fix

### Phase 1: Wait GAP-612 AWS restoration

Per `documents/04-quality/audits/aws-verification/2026-05-18-fe-runtime-state-and-cve-gate-investigation.md` recommendation:
- D+4 = 2026-05-21 — re-check AWS Support case 177903869600100
- D+7 = 2026-05-24 — escalate qua Twitter `@AWSSupport`
- D+8 = 2026-05-25 — evaluate tạo account mới
- Deadline xoá account = 2026-06-01

### Phase 2: Post-restore live verify (~15-30min)

```bash
# After deploy + Coordinator F sequence
curl -sI -H "Authorization: Bearer $JWT" https://api.kitehub.me/api/v1/admin/instances
# Expect: HTTP 200, content-type application/json, list shape

curl -sI -H "Authorization: Bearer $JWT" https://api.kitehub.me/api/v1/admin/payments
# Expect: HTTP 200, pending list + summary

curl -sI -H "Authorization: Bearer $JWT" https://api.kitehub.me/api/v1/admin/revenue
# Expect: HTTP 200, full report + summary
```

### Phase 3: Browser UX verify (admin persona Mai)

Per `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist (a)-(g) — login as PLATFORM_ADMIN seed user → navigate /admin/instances|payments|revenue → verify page render + data display.

## Acceptance Criteria

- [ ] GAP-612 AWS restoration confirmed (precondition)
- [ ] 3 curl smoke tests return HTTP 200 (not 404 — Wave 90 walkthrough finding)
- [ ] Admin browser UX verify (login → navigate → page render) per `pre-handoff-self-test-completeness.md` §2.4 checklist
- [ ] Log live verify output trong `documents/04-quality/audits/aws-verification/2026-05-{XX}-wave-92-bucket-d-live-verify.md`
- [ ] Wave 92 plan §7 Closure Protocol cross-reference live verify completion
- [ ] Status flip DONE only sau live verify complete

## Related

- Wave 92 Bucket D PR: [#1514](https://github.com/VictorAurelius/2026-Kite-Class-Platform/pull/1514)
- GAP-612 — AWS account suspension (blocker precondition)
- Wave 90 walkthrough sub-finding — 3 admin endpoints 404 (Wave 92 Bucket D fix code-level shipped, live verify pending)
- Rule: `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist
- Rule: `wave-closure-scope-completeness.md` §3 (sister gap GAP-619 + GAP-621 same wave closure)
- Rule: `agent-aws-access.md` §5 audit artifact mandate cho live verify session

## Log

- **2026-05-18 (filed):** Filed by Wave 92 closure scope-completeness audit per `wave-closure-scope-completeness.md` v1.0.0 §3 reconciliation. Orphan item surfaced khi user-flagged 2nd recurrence — code shipped Bucket D PR #1514 nhưng live `curl → 200` mandatory AC #5 không thực thi vì GAP-612 chặn deploy. Status OPEN until GAP-612 restore + Phase 2-3 execution.
