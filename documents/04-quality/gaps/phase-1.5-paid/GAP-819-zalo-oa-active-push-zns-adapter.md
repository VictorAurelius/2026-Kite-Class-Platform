# GAP-819: Zalo OA active push — ZNS adapter (Phase 1.5 paid tier)

**Status:** 🔵 OPEN
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

## Proposed fix

Per design doc `documents/02-architecture/zalo-integration-design.md` §3:

1. `ZaloZnsAdapter` (NEW) implements `NotificationChannel` — HTTP POST → `openapi.zalo.me/v2.0/oa/message`
2. `ZaloZnsClient` (NEW) — HTTP client wrapping ZNS API + retry + DLQ
3. `zalo-templates.yml` — local mapping `NotificationType` ↔ `template_id` (Zalo approves templates)
4. DB migration: add `zalo_oa_id`, `zalo_zns_access_token`, `zns_template_id_*` columns to `tenants`
5. Tenant onboarding step "Zalo OA verification" — verify ownership, store credentials encrypted
6. Config: `kitehub.notification.channels.enabled=EMAIL,ZALO` cho paid tenants only via subscription guard
7. Fallback chain: ZALO → EMAIL on rate-limit/template-not-approved
8. Cost monitoring CloudWatch metric per tenant
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
