# GAP-589: Admin Resend bounce visibility + impersonate-read-only debugging path

**Status:** 🟡 PARTIAL (25%) — Wave 86 docs-cluster spec planning shipped (`documents/04-quality/audits/persona-review/2026-05-16-gap-589-admin-bounce-visibility-spec.md`). Implementation = Wave 87+ multi-phase (Phase 1 webhook + persistence, Phase 2 admin dashboard tab, Phase 3 impersonate flow + JWT + audit log + FE guard, Phase 4 runbook + training). H-AC13 spec scope only docs-cluster; implementation gap files Wave 87+ planning.
**Priority:** 🟠 P1
**Domain:** Backend / Admin Dashboard
**Phase:** phase-1-beta
**Found:** 2026-05-15 (Wave 86 Bucket A persona-outside-in audit cell 5.5)
**Affects:** Platform Admin (Mai) debugging path during first 5 beta cohort incident response

## Problem

Persona cell 5.5 (Mai — Platform Admin): Incident response prereq cho Wave 86 Bucket H. Hiện thiếu:
1. **Bounce visibility**: Nếu invite email bounce (typo, mailbox full) → admin KHÔNG thấy trong dashboard → first-cohort tenant lost silent. Resend dashboard có data nhưng KHÔNG aggregated vào admin internal dashboard.
2. **Impersonate read-only**: Khi tenant report bug → admin cần 1-click impersonate read-only (KHÔNG có quyền mutate) cho debug. Hiện không có path → admin phải gọi tenant lấy screenshot/credentials = security risk.

Wave 86 Bucket H "monitoring + incident response" không cover hai mechanisms này.

## Root Cause

Wave 84 ops baseline focused infrastructure observability (CloudTrail, CloudWatch). Application-level admin debug tools không scope vào Wave 86 plan.

## Proposed Fix

1. **Resend bounce webhook handler** `kitehub-email/.../ResendWebhookController.java`:
   - Subscribe Resend `email.bounced` + `email.complained` events
   - Persist to `email_send_audit` table với event_type + bounce_reason
   - Admin dashboard query latest 100 bounces per cohort
2. **Admin dashboard "Email Delivery" tab** `kitehub-frontend/src/app/admin/email-delivery/page.tsx`:
   - Table: timestamp / recipient / status (sent/bounced/complained) / reason / resend button
   - Filter by tenant + cohort
3. **Impersonate read-only mechanism**:
   - Admin endpoint `POST /api/v1/admin/impersonate/{tenantId}/{userId}` issues short-lived JWT (15min TTL) với `role=READ_ONLY` claim + `impersonator=mai@kitehub.me` claim
   - All mutation endpoints check `role != READ_ONLY` → 403 if attempt mutate
   - Audit log every impersonate session với reason field (required)
   - Admin dashboard "Hỗ trợ tenant" button → 1-click impersonate
4. **Permission guard FE** mọi mutation button disabled khi `role == READ_ONLY`

## Acceptance Criteria

- [x] Spec planning shipped — `documents/04-quality/audits/persona-review/2026-05-16-gap-589-admin-bounce-visibility-spec.md` defines: webhook schema + email-delivery tab spec + impersonate endpoint + JWT claims + audit log table + FE guard pattern + runbook section
- [ ] Resend webhook handler shipped + bounce events persisted — defer Wave 87+ Phase 1 implementation
- [ ] Admin "Email Delivery" tab live với filter + resend — defer Wave 87+ Phase 2 implementation
- [ ] Impersonate read-only endpoint shipped + JWT enforces role check — defer Wave 88+ Phase 3 (higher security risk; needs review)
- [ ] FE mutation buttons disabled khi impersonate session active — defer Wave 88+ Phase 3
- [ ] Audit log every impersonate session với reason — defer Wave 88+ Phase 3
- [ ] Wave 86 Bucket H runbook reference này cho incident response — defer Wave 88+ Phase 4 (synced với Phase 3 impersonate flow implementation)

## Log

- **2026-05-16** Wave 86 docs-cluster — spec planning shipped. Status flipped OPEN → PARTIAL (25%). Per `gap-done-discipline.md` §3 PARTIAL exit ramp: 1 AC verified (spec doc); 6 ACs deferred to Wave 87+/88+ implementation phases. Verification artifact: `documents/04-quality/audits/persona-review/2026-05-16-gap-589-admin-bounce-visibility-spec.md`. Follow-up gaps to file Wave 87 planning: `GAP-XXX wave-87-resend-bounce-webhook-handler` (P1) + `GAP-XXX wave-87-admin-email-delivery-tab` (P2) + `GAP-XXX wave-87-admin-impersonate-readonly` (P1 — security review required).

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-persona-outside-in.md` §3.5 cell 5.5 + §4 rank 5 + §6 NEW gap proposal #5
- Wave 86 plan §3 Bucket H AC H-AC13 (paired)
- Wave 84 ops audit 78/100 (GAP-144 AlertManager carry-forward)
