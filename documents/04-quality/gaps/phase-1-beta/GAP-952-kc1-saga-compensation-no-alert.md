# GAP-952: Saga compensation failure chỉ log warn — admin không biết để clean orphan

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant provisioning saga compensation) + observability
**Defer-to:** After Wave flow-kh3 finish

## Problem

BR-PROV-005: "Compensation failure is logged but never rethrown (best-effort)". `compensate()` catch RuntimeException + `log.error`. KHÔNG emit alert / metric / dead-letter. Khi saga fail → markFailed compensation cũng fail (vd DB connection lost) → instance row stuck status `GENERATING` forever. Admin không nhận alert → 3 tháng sau audit "0 healthy instance metric drop" thì mới phát hiện. Surfaced: persona Finding 4.3.

## Proposed Fix

Wire `compensate()` failure thành CloudWatch metric `tenant_provisioning_compensation_failed` + SNS alarm threshold >0. Dead-job sweep cron (`@Scheduled`) scan instances stuck `GENERATING`/`INITIALIZING` > 10 min → escalate.

## Acceptance Criteria

- [ ] CloudWatch alarm `tenant_provisioning_compensation_failed > 0` fires SNS
- [ ] Sweep cron job `provisioning-stuck-sweep` scheduled (every 5 min)
- [ ] Manual fail injection → alert arrives within 5 min

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-tenant-provisioning.md Finding 4.3
- Sister: matrix A1×E5×EC5 (saga DEPLOYED step no SLA sweep)
- Flow Verification Campaign §4 row KC-1
