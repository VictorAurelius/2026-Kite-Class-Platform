# GAP-948: "Tenant ready" email không tồn tại — sendTenantReadyEmail method missing

**Status:** 🟡 PARTIAL (60% — code shipped Wave provisioning-1 Bucket C; live walk + Resend template pending)
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

- [x] `grep "sendTenantReady" kitehub-subscription/src/main/java -r` returns ≥1 hit — `EmailServiceClient.sendTenantReadyEmail(instanceId, to, organizationName, subdomain)` shipped (Wave provisioning-1 Bucket C)
- [ ] Post-provision walk → MailHog UI shows tenant-ready email với onboarding link — **PENDING live walk** (per `feature-ship-runtime-walk-mandate.md`; local Docker stack not available this session)
- [x] Outbox failure retry path (3 retry → DLQ) — reuses existing `email.send` → 3-retry → `email.dlq` path (sendTenantReadyEmail routes through `dispatchEmail`); plus `tenant.deployed.queue` → `tenant.deployed.dlq` for poison-payload on the new consume leg. Unit-tested swallow/no-throw paths; **live 3-retry→DLQ walk PENDING** (needs broker)

## Architecture finding (state-check per `audit-to-gap-pipeline.md` §2.5)

Gap §Proposed Fix assumed `sendTenantReadyEmail` could be "wired into outbox after saga DEPLOYED event" in one place. State-check found a **cross-service split** the gap/wave-plan did not anticipate:

- `EmailServiceClient` lives in **kitehub-subscription**; the saga + `TenantCreatedEventConsumer` live in **kiteclass-core** (separate Spring services — kiteclass-core cannot inject the email client bean).
- `TenantCreatedEvent` carries only `tenantId/slug/audience/tone` — **no owner email** (the recipient lives only in kitehub-subscription `Instance.contactEmail`). So the kiteclass-core consumer cannot *send* the email; it can only *trigger* it.

**Implemented flow** (satisfies all 3 scope items): kiteclass-core `TenantCreatedEventConsumer` → after `saga.provision()` returns DEPLOYED → `TenantReadyNotifier` publishes `tenant.deployed` (raw-UTF8 JSON, GAP-925 wire-format, §3.5.1 Exception D dedicated dispatcher) → kitehub-subscription `TenantDeployedEventConsumer` resolves owner from `Instance` → `EmailServiceClient.sendTenantReadyEmail`. `TenantProvisioningSaga.java` untouched (Bucket D collision avoided).

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-failure-mode-matrix.md A4×E6×EC2 + EC3
- Sister: benchmark §A row 11 welcome-email pattern
- Flow Verification Campaign §4 row KC-1
- Fixed-by: Wave provisioning-1 Bucket C (PR TBD)

## Log

- **2026-06-06 (Wave provisioning-1 Bucket C):** Status OPEN → 🟡 PARTIAL (~60%). Shipped: (1) `EmailServiceClient.sendTenantReadyEmail` (kitehub-subscription, VN template `tenant-ready` "Trường học của bạn đã sẵn sàng trên KiteClass!", reuses email.send 3-retry+DLQ); (2) cross-service `tenant.deployed` event + `TenantReadyNotifier` (kiteclass-core) + `TenantDeployedEventConsumer` (kitehub-subscription) resolving owner from `Instance`; (3) `tenant.deployed.queue` + DLQ in `EmailQueueConfig`; (4) consumer hook in `TenantCreatedEventConsumer` (saga untouched). Unit tests green both modules. **Stays PARTIAL** per `feature-ship-runtime-walk-mandate.md`: live MailHog post-provision walk + Resend `tenant-ready` template (HTML+txt) creation pending broker/stack. Architecture finding documented above (gap under-scoped the cross-service split).
