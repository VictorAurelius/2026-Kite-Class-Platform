---
id: GAP-721
title: Zalo OA owner-notify stub log (invoice + invite + payment confirm) — extends GAP-286 Phase 2
status: OPEN
priority: P1
phase: phase-1-beta
audience: dev
found: 2026-05-22
last_verified: 2026-05-22
completion_pct: 0
related: [GAP-286, GAP-720, GAP-722]
---

# GAP-721 — Zalo OA owner-notify stub log

## Problem

Wave 105 Bucket B Owner persona walk (per `documents/04-quality/audits/persona-review/2026-05-22-wave-105-bucket-b-owner-walk.md` §1 Step 8) AC: "Invoice delivery: PDF + Zalo OA stub log 'would send invoice' + email backup OK". Per outside-in audit `vn-saas-benchmark.md`: **3/3 VN edu SaaS competitors (DotB/EduSpace/CloudClass) prioritize Zalo OA channel** cho center owner notifications. KiteHub email-only path = industry outlier.

**Persona impact (Hằng — Owner):**
- Hằng dùng Zalo OA cho tenant ↔ phụ huynh communication (VN edu norm per `vn-localization-audit-checklist.md` §4)
- Email backup OK cho formal invoice (compliance), nhưng Zalo OA primary cho real-time alerts (payment confirm, invite accepted, attendance alert)
- Current Wave 105 ship email-only → Hằng SẼ NOT see real-time alerts

## Root cause

Wave 100 GAP-286 locked email-only signup path (cost-priority + Phase 1 BETA scope narrow). Phase 2 Zalo OA full integration deferred. Wave 105 wave plan §11 confirms "Zalo OA full integration Wave 106 — extends GAP-286 stub from this wave".

## Proposed Fix

### Phase 1 (this gap — stub log only)

3 events emit stub log "would send Zalo OA" cho Owner persona events:

1. **Invoice generated** — `kitehub-subscription/.../invoice/service/InvoiceService.java` after PDF gen + email dispatch → emit log `INFO would send Zalo OA: invoice_id=<X> recipient=<Hằng>` (stub, no real Zalo API call)
2. **Beta invite approved** — `kitehub-platform/.../beta/service/BetaAccessService.java` after invite token issued → emit log `INFO would send Zalo OA: invite_token=<X> recipient=<email>`
3. **Payment confirmed** — `kitehub-subscription/.../payment/service/PaymentService.java` after VietQR webhook callback → emit log `INFO would send Zalo OA: payment_id=<X> amount_vnd=<X> recipient=<Hằng>`

Stub helper class: `kitehub-subscription/.../zalo/ZaloOAStubLogger.java` — single `logWouldSend(eventType, recipientId, payload)` method emitting structured JSON log per `logs-format-standard.md`. Wired tại 3 sites above.

### Phase 2 (Wave 106 — full integration, out-of-scope this gap)

- Real Zalo OA API integration (vendor account + access token)
- Owner opt-in flow (link Zalo OA ↔ KiteHub account)
- Fallback to email when Zalo OA delivery fails
- Template rendering qua `kitehub-email` extension OR new `kitehub-zalo` module

## Acceptance Criteria

- [ ] `ZaloOAStubLogger.java` created với `logWouldSend(eventType, recipientId, payload)` method
- [ ] 3 sites wired (invoice + beta-invite + payment) emit stub log
- [ ] Log format: structured JSON với fields `event_type`, `recipient_id`, `payload_size`, `would_send_via=zalo_oa_stub`
- [ ] Unit test verifies 3 sites emit stub log với correct event_type
- [ ] Wave 106 plan §3 includes "Zalo OA full integration" bucket (extends GAP-286 Phase 2)
- [ ] `documents/01-business/kitehub/notification/rules.md` adds BR-NOTIF-NEW "Zalo OA stub Phase 1, full Phase 2"

## Related

- Persona walk: `documents/04-quality/audits/persona-review/2026-05-22-wave-105-bucket-b-owner-walk.md` §1 Step 8
- VN edu benchmark: `documents/04-quality/audits/persona-review/2026-05-22-wave-105-vn-saas-benchmark.md` (Zalo OA industry norm)
- Cross-persona Zalo culture: `.claude/rules/vn-localization-audit-checklist.md` §4
- Email-only path lock: GAP-286 Wave 100 (Phase 2 Zalo extension owner)
- Wave plan defer: `documents/03-planning/waves/wave-2026-05-22-105-persona-walk-beta-readiness.md` §Open Items "Zalo OA full integration Wave 106"
- Sister gaps Wave 105 Bucket B: GAP-720 (multi-branch FAQ), GAP-722 (VietQR live)

## Log

- **2026-05-22:** Gap filed. Wave 105 Bucket B AC PARTIAL — Zalo OA stub log "would send" mandate per wave plan AC; full integration defer Wave 106.
