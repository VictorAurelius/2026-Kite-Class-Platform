# GAP-948: "Tenant ready" email không tồn tại — sendTenantReadyEmail method missing

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant provisioning) — recovery path + trust signal
**Defer-to:** After Wave flow-kh3 finish

## Problem

`EmailServiceClient` expose `sendBetaInviteEmail` + `sendInviteStaffEmail` only. KHÔNG có `sendTenantReady`/`sendTenantReadyEmail` method. Sau khi `registerFromBetaInvite` thành công, user nhận JWT + redirect dashboard nhưng KHÔNG nhận confirmation email với onboarding link / SLA / support contact. Recovery path absent nếu user đóng tab. Per benchmark §A row 11: industry standard (Stripe/Slack/Google Classroom) ship welcome email + setup checklist post-provision. Surfaced: matrix A4×E6×EC2.

## Proposed Fix

Thêm method `sendTenantReadyEmail(ownerEmail, tenantName, dashboardUrl, onboardingChecklistUrl)` trong `EmailServiceClient`. Wire vào outbox sau saga `DEPLOYED` event. Template Vietnamese + Resend transactional. Plus DLQ visibility (per matrix A4×E6×EC3) cho 3-retry exhausted.

## Acceptance Criteria

- [ ] `grep "sendTenantReady" kitehub-subscription/src/main/java -r` returns ≥1 hit
- [ ] Post-provision walk → MailHog UI shows tenant-ready email với onboarding link
- [ ] Outbox failure retry path tested (3 retry → DLQ); alert hooked

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-failure-mode-matrix.md A4×E6×EC2 + EC3
- Sister: benchmark §A row 11 welcome-email pattern
- Flow Verification Campaign §4 row KC-1
