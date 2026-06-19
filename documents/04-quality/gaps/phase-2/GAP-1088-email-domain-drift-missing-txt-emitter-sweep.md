# GAP-1088: Sweep follow-up — email domain drift + 16 template thiếu .txt + sister outbox emitter double-publish

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-09 (Wave landing-tenant-1 — cross-flow sweep DEFER findings từ GAP-1085 + GAP-1086)
**Affects:** `EmailServiceClient` (domain URLs) + `kitehub-email/templates/emails/*` (16 html-only) + `BrandingEventEmitter` / `BrandingEventPublisher` (outbox fast-path)

## Problem

3 nhánh DEFER từ sweep của GAP-1085/1086 (fix in-scope đã làm; phần còn lại gom đây tránh stash vào narrative per `discovery-to-gap-inline-filing.md`):

### A. Domain drift trong `EmailServiceClient` (10 site còn lại)
grep `https://kitehub\.(vn|com)`:
- `kitehub.com` (9): trial-warning/expired `upgradeUrl`, renewal `paymentUrl`, suspension/retention `renewUrl`, trial-midpoint `upgradeUrl`, data-deleted `contactUrl`.
- `kitehub.vn` (1 nhóm): welcome `loginUrl`, DSAR `dpoQueueUrl` + `statusCheckUrl`.

Canonical Phase 1 BETA = `kitehub.me`. Cần chốt: domain dashboard/login/help/support/legal đồng nhất `kitehub.me`; `kitehub.me` chỉ cho subdomain tenant app (`{subdomain}.kitehub.me`). Footer `support@kitehub.me` trong subscription templates cũng cần soát.

### B. 16 template html-only thiếu `.txt` (= GAP-657/659 "final 20%")
`admin-new-login-alert, beta-request-confirmation, class-rescheduled, data-deleted, data-retention-final-warning, data-retention-warning, dsar-acknowledgement-requester, dsar-new-ticket-dpo, invoice, onboarding-tips, subscription-expired, subscription-renewal-reminder, subscription-suspended, trial-expiration-warning, trial-expired, trial-midpoint`.
`EmailTemplateRenderer.renderPlainTextSibling` trả rỗng → HTML-only → ~20% churn risk Gmail/Outlook. (subscription-created/activated đã fix tại GAP-1086.)

### C. Sister outbox emitter — verify double-publish (class GAP-1085)
GAP-1085 fix `SubscriptionEventEmitter` (stamp `dispatched_at` khi fast-path delivered). Sister emitter có thể cùng class:
- `kitehub-branding/.../outbox/BrandingEventEmitter` — verify có stamp dispatched-on-fast-path không.
- `kiteclass-core/.../branding/events/BrandingEventPublisher` (generic `OutboxEventWriter`) — verify riêng.
Nếu thiếu stamp → branding event cũng double-publish (consumer chỉ dựa idempotency net).

## Proposed Fix

- A: env-reference đồng nhất domain → sweep 1 lần toàn `EmailServiceClient` (+ footer templates).
- B: batch tạo `.txt` cho 16 template (gộp GAP-657/659 follow-up) HOẶC đóng nếu quyết HTML-only acceptable cho non-critical types.
- C: đọc 2 emitter, nếu thiếu stamp thì áp cùng pattern GAP-1085; thêm test.

## Acceptance Criteria

- [ ] A: 0 hit `kitehub.(vn|com)` trong EmailServiceClient URL (trừ external/legacy có lý do)
- [ ] B: mọi queue-consumed template có `.txt` sibling HOẶC documented HTML-only exception
- [ ] C: BrandingEventEmitter + BrandingEventPublisher verify không double-publish (stamp dispatched hoặc dedup tương đương) + test

## Related

- Parent fixes: GAP-1085 (emitter double-publish), GAP-1086 (subscription email content)
- Plain-text origin: GAP-657, GAP-659
- Sweep rule: `cross-flow-bug-class-sweep.md` §4.1 (statically-detectable → persistent detector candidate)
