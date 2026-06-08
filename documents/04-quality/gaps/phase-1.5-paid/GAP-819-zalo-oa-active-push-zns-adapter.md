# GAP-819: Zalo OA active push — ZNS adapter (Phase 1.5 paid tier)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-01 (Zalo audit + thesis-as-future-state-mandate rule landing)
**Phase:** phase-1.5-paid
**Affects:** Notification dispatch (grade / attendance / fee reminder) — Phase 1.5 paid tier feature

## Problem

Phase 1 BETA ship passive Zalo OA CTA (GAP-660 DONE Wave 98) — deep-link button trong email + Footer + SupportMenu. User phải chủ động click → mở Zalo → chat với OA. KHÔNG có push proactive từ KiteHub → parent.

Per `thesis-as-future-state-mandate.md` v1.0.0 §3.1 — thesis Ch1 §1.1.2 + Ch1 §1.4 + Ch2 claim "đã kết nối Zalo OA" = forward commitment. Phase 1 BETA shipped MINIMUM interpretation (passive CTA). Phase 1.5 paid tier MUST deliver FULL interpretation (active push) trước khi paid features go live.

## Thesis source

- Ch1 §1.1.2: "Hệ thống đã hỗ trợ email cho tài liệu chính thức và đã kết nối kênh Zalo OA cho liên lạc với phụ huynh"
- Ch1 §1.1.2 P2: "phụ huynh nhận cập nhật thường xuyên qua nhóm Zalo OA đã được kết nối"
- Ch1 §1.4: "kênh thông báo Zalo OA đã kết nối phục vụ liên lạc với phụ huynh"
- Ch2 §System architecture: "đã tích hợp Zalo OA"
- Ch2 §Compatibility: "kênh giao tiếp Zalo cho phụ huynh"

## Root cause

`NotificationChannelType.ZALO` enum value tồn tại từ Phase 1 forward-compat per BR-NOTIF-002 + BR-NOTIF-010 — nhưng `kitehub.notification.channels.enabled=EMAIL` only; ZALO adapter defer to GAP-063b Phase 2. Phase 1.5 trigger là paid tier monetization → cần richer feature set bao gồm active push.

## Current State (verified 2026-06-08 — GAP-819 flow-check, Explore agent)

⚠️ **Proposed fix bản gốc (dưới) đặt adapter SAI CHỖ** (kitehub-email NotificationChannel) + bỏ qua scaffold đã có. Flow-check phát hiện **3 scaffold Zalo cùng tồn tại** → kiến trúc đã reconcile trong `zalo-integration-design.md` §3.0 + §3.3 (2026-06-08). Bản gốc giữ dưới để tham chiếu; **build theo §3.3 reconciled (kiteclass-core)**.

| Thành phần | Trạng thái | Build code-now? |
|---|---|---|
| Outbox `zalo_oa_notification_outbox` (V61) + RLS (V78) | ✅ đã có | — |
| `ZaloOaNotificationService` stub (3 method: invite/payment/attendance, log-only) | ✅ đã có (Wave 105) | — |
| `kitehub-email/zalo/ZaloOAClient` + `zalo.*` config | ✅ đã có (GAP-063, generic send, mock) | scope khác, không reuse |
| `ZaloZnsClient` (HTTP, mock\|live) | ❌ chưa | 🟢 code-now (mock) |
| `ZaloOutboxDispatcher` (@Scheduled drain outbox) | ❌ chưa (outbox ghi mà không ai drain) | 🟢 code-now |
| Fix `resolveTenantId()` nil-UUID bug | ❌ bug (hardcode zero-UUID) | 🟢 code-now (prerequisite) |
| Grade hook (`recordGradePublished` + ALTER `chk_zalo_oa_event_type` + caller) | ❌ chưa (GRADE không trong CHECK) | 🟢 code-now |
| Wire invite + attendance callers (hiện 0 caller) | ❌ chưa | 🟢 code-now |
| `zalo-zns-templates.yml` scaffold | ❌ chưa | 🟢 scaffold-now / 🔴 template_id thật blocked Zalo approval |
| IT mock (dispatcher → client mock, fallback ZALO→EMAIL) | ❌ chưa | 🟢 code-now |
| Live OAuth token + refresh | ❌ | 🔴 blocked — cần Zalo App credentials (user tạo) |
| End-to-end live ZNS push verify | ❌ | 🔴 blocked — cần OA Test + App + template approved |

**Architecture + schema decisions chốt 2026-06-08** (xem `zalo-integration-design.md` §3.0): build ở **kiteclass-core outbox-drain** (KHÔNG kitehub-email); **platform-level single OA** Phase 1.5 (per-tenant OA columns defer Phase 2 → AC §4 below revised).

**Session 2026-06-08 progress:** flow-check + architecture reconcile + design doc §3.0/§3.3 rewrite shipped (this PR, docs-only). Code build (client + dispatcher + migration + grade hook + IT) → next session với context sạch + user's Zalo App credentials.

## Proposed fix (ORIGINAL — superseded by §3.3 reconciled; kept for reference)

Per design doc `documents/02-architecture/zalo-integration-design.md` §3 (reconciled version):

1. `ZaloZnsClient` (NEW, **kiteclass-core**) — HTTP client `provider=mock|live` → `openapi.zalo.me` ZNS
2. `ZaloOutboxDispatcher` (NEW, **kiteclass-core**) — `@Scheduled` drain `zalo_oa_notification_outbox` PENDING → client → DISPATCHED/FAILED + retry
3. `zalo-zns-templates.yml` — event_type ↔ template_id mapping
4. ~~DB migration add columns to `tenants`~~ → **DEFER Phase 2** (platform-level single OA Phase 1.5 per §3.0 schema decision)
5. Fix `resolveTenantId()` + ALTER `chk_zalo_oa_event_type` (+GRADE_PUBLISHED) + wire 0-caller methods
6. Config reuse `zalo.*` platform-level; `provider=mock` default
7. Fallback chain: ZALO → EMAIL on rate-limit/template-not-approved
8. Cost monitoring CloudWatch metric
9. Audit log dispatch outcome per BR-NOTIF-001

## Acceptance criteria

- [ ] `ZaloZnsAdapter` implements `NotificationChannel` interface với send() + getProvider() override
- [ ] `ZaloZnsClient` HTTP client với retry 3x + DLQ fallback
- [ ] At least 3 ZNS templates Vietnamese drafted (grade / attendance / fee) + Zalo approval requested (ops side)
- [ ] Tenant onboarding wizard step "Zalo OA configuration" — UI + BE persistence
- [ ] DB migration applied: `tenants.zalo_oa_id` + `zns_template_id_*` columns
- [ ] `kitehub.notification.channels.enabled` per-tenant override based on subscription tier
- [ ] IT test: dispatcher routes ZALO channel → `ZaloZnsAdapter` correctly
- [ ] IT test: fallback chain ZALO → EMAIL on simulated rate-limit/error
- [ ] Audit log captures dispatch outcome (success / fallback / dropped)
- [ ] CloudWatch metric `kitehub.notification.zalo.sent` tracked per tenant
- [ ] Cross-flow sweep per `cross-flow-bug-class-sweep.md` §3 — verify no other notification path bypasses dispatcher
- [ ] Documentation update: `documents/05-guides/integration/zalo-oa-setup-runbook.md` extended với ZNS section

## Future scope (defer Phase 2)

- Auto-template approval automation
- 2-way messaging (user reply → KiteHub support inbox)
- Zalo deep-link tracking analytics

## Related

- Design doc: `documents/02-architecture/zalo-integration-design.md` §3 (paired same-PR)
- Sister rule: `.claude/rules/thesis-as-future-state-mandate.md` v1.0.0 (paired same-PR)
- Phase 1 minimum: GAP-660 DONE Wave 98 (passive CTA)
- Phase 2 full Notification scope: GAP-063b P1 (overlaps; this gap = narrow Phase 1.5 subset)
- Business rules: `documents/01-business/kitehub/notification/rules.md` BR-NOTIF-002 + BR-NOTIF-010
- Setup runbook: `documents/05-guides/integration/zalo-oa-setup-runbook.md`
