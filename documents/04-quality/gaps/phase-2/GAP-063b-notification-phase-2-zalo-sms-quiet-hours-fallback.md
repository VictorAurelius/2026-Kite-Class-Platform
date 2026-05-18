# GAP-063b: Notification Phase 2 — Zalo ZNS + SMS adapter + quiet hours + fallback chain + cost tracking

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (sister of GAP-063 Phase 1 SHIPPED Wave 18a)
**Domain:** Backend / Integration
**Detected:** 2026-05-04 (Wave 18a Bucket B closure — sister gap filed by closure coordinator)
**Affects:** All 4 Tier-1 personas (P1 + P2 + P3 + P5) — notification-channel diversity blocked-on this gap

---

## Context

Phase 1 of GAP-063 shipped Wave 18a Bucket B (PR #759 merged 2026-05-04):
- ✅ `NotificationChannel` interface (Strategy Pattern) in `kitehub-email/api/`
- ✅ `SESEmailService` refactored to implement interface (backward compat preserved)
- ✅ `NotificationPreference` entity (per User × NotificationType × Set<Channel>)
- ✅ V23 migration in kitehub-subscription
- ✅ CRUD service + controller + audit log
- ✅ Settings page UI (EMAIL active; SMS/Zalo/Push placeholders pointing here)
- ✅ Business docs 3-layer + business-logic-review.md 5-attribute frontmatter

This gap (063b) covers Phase 2 deferred items.

## Problem

VN K-12 + center deployments need diverse channels:
- Email không hiệu quả với phụ huynh + học sinh (low check rate)
- Zalo dominant in VN (Zalo OA + ZNS Notification Service for templated messages)
- SMS critical for emergency / urgent (school closure, child safety, payment due)
- Quiet hours respect = compliance + UX
- Cost-attribution per tenant for billing integration (GAP-017)
- Fallback chain: try preferred channel → fall back if quota or delivery fail

Without these, settings page placeholders ("Coming soon — GAP-063b") never activate.

## Current State (verified 2026-05-04)

| Piece | Status (Phase 1 baseline) |
|-------|---------------------------|
| NotificationChannel interface | ✅ DONE Phase 1 |
| EmailNotificationChannel (SES adapter) | ✅ DONE Phase 1 |
| NotificationPreference entity | ✅ DONE Phase 1 |
| Settings page UI | ✅ DONE Phase 1 (EMAIL only) |
| Zalo ZNS adapter | ❌ MISSING |
| SMS adapter (Twilio/VNStack/FPT) | ❌ MISSING |
| Quiet hours respect (in NotificationPreference) | ❌ MISSING — entity has `enabledChannels` only; no time-based rule |
| Cost tracking per tenant | ❌ MISSING — needs counter + billing integration GAP-017 |
| Fallback chain (Zalo → SMS → Email) | ❌ MISSING — no orchestrator above channel layer |
| Push notification (FCM) | ❌ MISSING (lowest priority Phase 2) |

## Proposed Fix

### 2.1 Zalo ZNS adapter (highest priority — VN dominant)

1. Register Zalo Business OA account (out-of-code prerequisite)
2. Get ZNS API credentials → store in tenant secret config
3. Implement `ZaloNotificationChannel implements NotificationChannel`
4. Template message approval workflow (Zalo requires pre-approved templates)
5. Cost-per-message tracking (~600-1500 VND per ZNS depending on type)
6. Wire into NotificationPreferenceService — channel.ZALO becomes valid

### 2.2 SMS adapter

1. Choose provider: Twilio VN OR VNStack OR FPT SMS (cost comparison required first)
2. Implement `SmsNotificationChannel implements NotificationChannel`
3. Phone number format validation (BR-PARENT-002 regex `^0\d{9}$`)
4. Cost-per-SMS tracking (~200-300 VND)
5. Quota enforcement per tenant tier (FREE: limited, PREMIUM: unlimited within budget)

### 2.3 Quiet hours

1. Add `quiet_hours_start` + `quiet_hours_end` columns to `notification_preference` table (V<N> migration)
2. Migration backward-compatible: NULL = no quiet hours
3. NotificationDispatchService checks current time vs window before sending
4. Override exception: critical-severity (e.g., child safety, payment final notice) bypass quiet hours
5. Settings UI: time-picker for quiet hours

### 2.4 Cost tracking + tenant attribution

1. New `notification_cost_log` table: tenant_id, channel, recipient, sent_at, cost_vnd, message_type
2. NotificationDispatchService records per-send
3. Roll-up service for monthly tenant billing integration (depends GAP-017 for billing)
4. Admin dashboard: "Notification spend this month" widget per tenant

### 2.5 Fallback chain (orchestrator)

1. New `NotificationOrchestrator` service (above NotificationChannel layer):
   - Priority-ordered chain: tenant config + user preference
   - Default: Zalo → SMS → Email
   - On adapter failure (timeout, quota exceeded, API error) → try next
   - Audit log: which channel attempted, which succeeded
2. Idempotency: don't send same notification twice if first succeeds
3. Cost-aware: prefer cheaper channels when content allows

### 2.6 Push notification (FCM) — lower priority

Optional Phase 2.6; can defer to GAP-063c if scope grows.

## Acceptance Criteria

- [ ] `ZaloNotificationChannel` implementation + integration test against ZNS sandbox
- [ ] `SmsNotificationChannel` implementation (1 provider chosen)
- [ ] Settings page UI: SMS + Zalo channel toggles activated (no longer "Coming soon")
- [ ] Quiet hours columns added + dispatch service respects them
- [ ] Critical-severity bypass tested
- [ ] Per-tenant cost tracking emits events for billing roll-up
- [ ] `NotificationOrchestrator` fallback chain implemented + tested
- [ ] Fallback test: simulate Zalo failure → falls to SMS → falls to Email
- [ ] Provider selection ADR documented (`documents/02-architecture/adr/`)
- [ ] Business docs updated: BR-NOTIF-013..020 for new behaviors
- [ ] business-logic-review.md 5-attribute on rules.md (Source: VN market data on Zalo dominance + competitor analysis; Reviewer: solo-dev acting Product; Compliance: PDPL Art 16 still respected for child users)

## Estimated Effort

~2-3 weeks (multi-provider integration + sandbox testing). Can split into 4 sub-PRs:
- 063b.1: Zalo ZNS adapter (~5 days)
- 063b.2: SMS adapter + quiet hours (~5 days)
- 063b.3: Cost tracking + tenant billing roll-up (~3 days)
- 063b.4: NotificationOrchestrator fallback chain (~2 days)

## Related

- **Sister of:** GAP-063 Phase 1 (PR #759 merged 2026-05-04)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-04-18a-keystones.md`
- **Cross-cuts:** GAP-017 (billing — cost tracking depends), GAP-021 (branding propagation in messages), GAP-321 (parent portal — Zalo OTP login overlap), GAP-322 (child protection — emergency notification bypass quiet hours)

## Log

- **2026-05-04** — Filed by Wave 18a closure coordinator. Phase 1 SHIPPED; Phase 2 scope explicitly listed in PR #759 description. Per `gap-done-discipline.md` §3 PARTIAL exit ramp, Phase 2 deferral made explicit by filing this sister gap.
