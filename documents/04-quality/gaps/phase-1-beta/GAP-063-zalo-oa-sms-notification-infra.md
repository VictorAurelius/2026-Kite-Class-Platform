# GAP-063: Zalo OA + SMS Notification Infrastructure (Phase 1 BETA P0)

**Status:** 🟡 PARTIAL (45%) — Phase 1 BETA P0 (re-scoped Wave 11). **Phase-1 Zalo channel scaffold DONE 2026-06-21** (`ZaloNotificationChannel` bridge + 5 tests green + `sms-provider-evaluation.md`). Phase 2 (live ZNS/SMS) + Phase 3 (monitoring) vendor/AWS-blocked. Phase 1 EMAIL channel scaffolding đã shipped Wave 18a Bucket B (notification abstraction + email adapter + user preference UI), nhưng Zalo OA + SMS adapter (the actual P0 channels per VN edu market) vẫn chưa có. Wave 11 outside-in persona audit (PR #2085) elevated từ P1 → P0 vì 3/5 Tier 1 personas blocked bởi missing Zalo OA channel.
**Priority:** 🔴 P0 (Phase 1 BETA blocker — re-classified 2026-06-02 per Wave 11 outside-in audit)
**Domain:** Backend + DevOps + Frontend (multi-layer notification + provisioning + integration)
**Phase:** phase-1-beta
**Found:** 2026-04-14 (original Phase 1 EMAIL scope) — **re-scoped 2026-06-02** (Wave 11 outside-in persona audit PR #2085 elevated to P0 Zalo OA + SMS infra)
**Affects:** P2 Center Owner + P3 Manager + P5 K-12 Parent (3/5 Tier 1 personas blocked per Wave 11 outside-in audit). Plus downstream blockers GAP-286 (Mobile OTP signup Zalo/SMS) + GAP-297 (Batch monthly invoice notification multi-channel).
**Related sister gap:** [GAP-063b](../phase-2/GAP-063b-notification-phase-2-zalo-sms-quiet-hours-fallback.md) — Phase 2 quiet hours + cost tracking + fallback chain (Wave 12+ scope)

## Problem

Wave 11 outside-in persona simulation audit (PR #2085) — `documents/04-quality/audits/persona-review/2026-06-02-wave-11-pre-lock-persona-mobile-otp-batch-invoice.md` §3-§4 — surfaced 3 critical findings về notification channel hierarchy:

1. **Mental model VN edu market** (Finding C1, X1): Zalo OA push notification = friendly, default kênh; SMS = banking spam (kèm phí ngầm); Email = audit trail only. 3/5 personas (Trần P2 Owner, Cô Hương P3 Manager, Linh P3 Student via parent) **expect** OTP / invoice notification qua Zalo OA. Nếu chỉ có Email channel → bounce rate cao → trust damage.

2. **Thesis claim drift** per `thesis-as-future-state-mandate.md` v1.0.0 — Chapter 1 §1.1.2 + §1.4 claim "đã kết nối Zalo OA" + §1.2.5 mention group Zalo cho phụ huynh. **Minimum interpretation** (passive CTA / config field) đã shipped GAP-660 Wave 98; **full interpretation** (active 2-way ZNS push + audit log) Phase 1.5+ delivery commitment — but Wave 11 audit elevated baseline Zalo OA push channel lên Phase 1 BETA P0 vì 3/5 personas mental model gap blocks shipping.

3. **Downstream cascade**: GAP-286 (Mobile OTP signup) + GAP-297 (Batch monthly invoice notification multi-channel) both reference Zalo OA + SMS as primary channels. Without notification infrastructure → cả 2 features Wave 12+ stay blocked.

**Phase 1 shipped (Wave 18a Bucket B 2026-05-04) — preserved as historical baseline:**

- Notification abstraction interface `NotificationChannel.java` + `NotificationContext` + `NotificationSendResult` (Strategy Pattern)
- `SESEmailService` implementing `NotificationChannel` (single adapter — EMAIL only)
- `NotificationPreference` entity per User × NotificationType × Set<Channel> + V23 Flyway migration
- CRUD endpoints `GET/PATCH /api/v1/notification-preferences` + settings UI page
- Mandatory-type guard (BR-NOTIF-008): EMAIL on `BILLING_INVOICE` / `SECURITY_ALERT` / `TRIAL_ENDING` cannot be disabled
- Business docs 3-layer: `documents/01-business/kitehub/notification/{rules.md, use-cases.md, api-contract.md}` BR-NOTIF-001..012

→ Notification abstraction READY for Zalo OA + SMS adapter wiring. Phase 1 EMAIL adapter unblocks downstream gaps khi Zalo OA + SMS adapter ship cùng (parallel channels).

## Root Cause

VN edu SaaS market reality không khớp default-email assumption:

- **Zalo dominance**: 75M+ Zalo users VN (~80% penetration). Push notification miễn phí, branded, trusted.
- **SMS cost asymmetry**: ~200-300 VND/SMS × tenant scale (vd 100 phụ huynh × 12 tháng × 3 thông báo/tháng = 720k VND/tenant/năm) → cost gating cần per-tier budget tracking.
- **Email VN penetration kém**: Phụ huynh K-12 + Center Manager đa số không check email daily. Email = audit/legal channel, không real-time trigger.

Phase 1 EMAIL-only scaffold + GAP-063 nguyên P1 priority underestimated market urgency. Wave 11 outside-in audit (3-persona walkthrough) là first empirical surface this priority gap.

GAP-286 (OTP) + GAP-297 (invoice batch) cũng underestimated cùng pattern — cả 2 đang OPEN P0 nhưng blocked bởi missing Zalo OA infra. Wave 11 plan chose to UNBLOCK chain bằng filing GAP-063 P0 infra trước.

## Proposed Fix — 3-phase delivery

### Phase 1 — Scaffold (Wave 11 + Wave 12 — current scope)

- **Zalo OA adapter scaffold** (Wave 11 Bucket B parallel): `ZaloOAClient.java` interface + mock impl + config skeleton (`zalo.oa.access-token`, `zalo.oa.template-id-otp`, `zalo.oa.template-id-invoice`). IT verify mock-mode dispatch. Live verify defer Wave 12 sau Zalo OA Business account verified.
- **SMS provider eval research** (Wave 11 Bucket C parallel): 3-provider comparison doc (Twilio VN / VNStack / FPT SMS) — pricing, latency, coverage, contract terms. Output: `documents/02-architecture/sms-provider-evaluation.md` + recommendation.
- **Integration tests**: Mock Zalo OA send → verify NotificationChannel contract; Mock SMS send → verify fallback chain logic; Email parallel (already working).
- **Persona-specific defaults**: P2 Owner default Zalo + Email; P3 Manager default Zalo + SMS fallback; P5 Parent K-12 default Zalo group + Email.

### Phase 2 — Live integration (Wave 12+, post-account-verify)

- **Verified Zalo OA Business account** registered + ZNS API credentials provisioned (operational task, not engineering).
- **SMS provider contract** signed với pick-winner (likely Twilio VN per Wave 11 Bucket C research outcome).
- **Live integration tests**: send real Zalo OA push → verify delivery in test Zalo group; send real SMS → verify delivery to test phone. Audit log entry per send (recipient, channel, timestamp, status).
- **Per-tenant cost tracking**: SMS cost attribution per tenant (foundation cho `business-logic-audit.md` tier budget Phase 1.5+).
- **Template approval**: 3 ZNS templates approved by Zalo (OTP signup, invoice payment reminder, attendance alert).

### Phase 3 — Monitoring (Wave 12+ post-live)

- Grafana dashboard: queue depth (Zalo + SMS + Email) + delivery rate per channel + per-tenant volume + p95 latency.
- Alert routing: queue depth > 100 → SRE; delivery rate < 95% per channel → on-call; SMS cost > budget threshold per tenant → billing alert.
- Cost report monthly: SMS spend per tenant aggregate → finance reconciliation.

## Acceptance Criteria

### Phase 1 — Scaffold (Wave 11 + Wave 12 close scope)

- [x] Notification abstraction interface — `NotificationChannel.java` (Wave 18a, preserved)
- [x] Email adapter — `SESEmailService` implementing `NotificationChannel` (Wave 18a, preserved)
- [x] User preference UI — `kitehub-frontend/src/app/(customer)/settings/notifications/page.tsx` (Wave 18a, EMAIL toggle live; Zalo/SMS toggles disabled với "Sắp ra mắt" tooltip)
- [x] `ZaloOAClient.java` interface + mock impl + config skeleton (`com.kitehub.email.zalo` — existed) **+ NEW `ZaloNotificationChannel` adapter bridging `ZaloOAClient` → platform `NotificationChannel` seam** (2026-06-21) — the genuinely-missing slot; mock-mode default, no live HTTP
- [x] SMS provider evaluation doc — `documents/02-architecture/sms-provider-evaluation.md` (2026-06-21; 3-provider ZNS-primary + SMS-fallback, reframes Bucket-C set toward ZNS)
- [~] IT verify mock dispatch — **Zalo mock verified** (`ZaloNotificationChannelTest` 5 tests green + `ZaloOAScaffoldIT`); **SMS mock adapter NOT built** (eval-first per Bucket C — SMS adapter deferred to Phase 2 once provider chosen)
- [ ] FE settings UI enable Zalo OA + SMS toggles — DEFER (explicitly post-Phase-2-verify; depends on live Zalo, vendor-blocked)

### Phase 2 — Live integration (Wave 12+ scope, post-account-verify)

- [ ] Verified Zalo OA Business account registered + ZNS credentials in Secrets Manager
- [ ] SMS provider contract signed (Twilio VN / VNStack / FPT SMS per Bucket C research)
- [ ] Live integration test: real Zalo OA push delivery verified in test group
- [ ] Live integration test: real SMS delivery verified to test phone
- [ ] 3 ZNS templates approved (OTP + invoice + attendance)
- [ ] Per-tenant SMS cost tracking table + attribution logic
- [ ] Fallback chain logic: Zalo OA fail → SMS → Email per BR-NOTIF-010

### Phase 3 — Monitoring (Wave 12+ post-live)

- [ ] Grafana dashboard `notification-channels` + Loki source
- [ ] Alert routing PagerDuty (SRE + on-call + billing)
- [ ] Cost report monthly aggregate per tenant

## Dependencies

- **Blocks**: GAP-286 (Mobile OTP signup Zalo/SMS) + GAP-297 (Batch monthly invoice notification multi-channel) — both downstream Wave 12+ features need infra trước
- **Related**: [GAP-063b](../phase-2/GAP-063b-notification-phase-2-zalo-sms-quiet-hours-fallback.md) Phase 2 quiet hours + cost tracking + fallback chain — separate Wave 12+ scope; this gap focuses Phase 1 scaffold + Phase 2 baseline integration
- **External vendor**: Zalo OA Business account verification (~2-3 business days), SMS provider contract negotiation (~1-2 weeks)
- **Compliance**: PDPL Article 21 (notification consent) — per `documents/01-business/kitehub/notification/rules.md` BR-NOTIF-009 (consent already covered Wave 18a)

## Related

- **Wave 11 outside-in audit** (PR #2085 trigger): [`documents/04-quality/audits/persona-review/2026-06-02-wave-11-pre-lock-persona-mobile-otp-batch-invoice.md`](../../audits/persona-review/2026-06-02-wave-11-pre-lock-persona-mobile-otp-batch-invoice.md) §3 persona walks + §4 cross-cutting findings (C1, X1)
- **Wave 11 plan**: [`documents/03-planning/waves/wave-2026-06-02-local-doable-11-zalo-sms-infra.md`](../../../03-planning/waves/wave-2026-06-02-local-doable-11-zalo-sms-infra.md) Bucket A (this gap file) + Bucket B (Zalo OA scaffold) + Bucket C (SMS provider eval)
- **Thesis source** per `.claude/rules/thesis-as-future-state-mandate.md` v1.0.0:
  - Chapter 1 §1.1.2 "đã kết nối Zalo OA" — Phase 1 BETA minimum interpretation = passive CTA shipped GAP-660 Wave 98; Phase 1 BETA P0 baseline elevated by Wave 11 audit = active push channel infra
  - Chapter 1 §1.4 — Zalo OA dominance mention
  - Chapter 1 §1.2.5 — group Zalo phụ huynh (Phase 1.5+ scope extension)
- **Downstream gaps unblocked**:
  - GAP-286 — Mobile OTP signup Zalo/SMS (P0 OPEN, blocked by this infra)
  - GAP-297 — Batch monthly invoice generation UX + multi-channel notification (P0 OPEN, blocked by this infra)
- **Sister Phase 2 gap**: [GAP-063b](../phase-2/GAP-063b-notification-phase-2-zalo-sms-quiet-hours-fallback.md) — quiet hours, cost tracking, fallback chain (Wave 12+ scope)
- **Original Phase 1 EMAIL scope** (preserved historical): Wave 18a Bucket B 2026-05-04 — notification abstraction + email adapter + V23 migration + preference UI + business docs 3-layer

## Log

- **2026-06-21** — Phase-1 Zalo channel scaffold DONE (completion 20% → 45%). Discovery: low-level Zalo mock (`ZaloOAClient` / `ZaloOAMockClient` / `ZaloOAConfig` / `ZaloOAScaffoldIT` trong `com.kitehub.email.zalo`) **đã tồn tại**; slot thiếu thật = adapter bridge sang platform `NotificationChannel` seam → thêm `ZaloNotificationChannel` (`@Service`, mock-mode default, map `NotificationContext`→`ZaloMessage`) + `ZaloNotificationChannelTest` (5 Mockito test green) + mở rộng config `zalo.*` (`enabled` + `zns-template-ids`). `./mvnw -pl kitehub-email test-compile` EXIT 0. SMS eval doc shipped (`documents/02-architecture/sms-provider-evaluation.md` — Zalo ZNS primary + eSMS-class SMS fallback; reframe Bucket-C set sang ZNS-primary). SMS mock adapter CHƯA build (eval-first per Bucket C; build Phase 2 khi chốt provider). Config giữ `zalo.*` có sẵn (không tạo `kitehub.notification.zalo.*` song song) per `design-patterns` no-duplicate-abstraction. Phase 2 (live ZNS/SMS) + Phase 3 (monitoring) vẫn vendor/AWS-blocked (REAL-USER-ACTION). 2 Opus agent + coordinator inline per `agent-concurrency-budget-inline-hybrid`.
- **2026-06-02** — Gap **re-scoped P1 → P0** + filename renamed (`GAP-063-sms-zalo-notification-integration.md` → `GAP-063-zalo-oa-sms-notification-infra.md`) per Wave 11 plan Bucket A. Trigger: Wave 11 outside-in persona simulation audit (PR #2085) surfaced 3/5 Tier 1 personas (P2 Owner / P3 Manager / P5 K-12 Parent) blocked bởi missing Zalo OA + SMS channel. AC restructured 3-phase delivery (Phase 1 scaffold Wave 11+12 / Phase 2 live integration Wave 12+ post-account-verify / Phase 3 monitoring Wave 12+ post-live). Original Phase 1 EMAIL scope (Wave 18a Bucket B) preserved as historical baseline + Phase 1 ACs marked `[x]` đã shipped. Per `thesis-as-future-state-mandate.md` v1.0.0 — gap references thesis Ch1 §1.1.2 + §1.4 + §1.2.5 source.
- **2026-05-04** — Phase 1 EMAIL scope shipped Wave 18a Bucket B. Status flipped 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 (Phase 2 Zalo + SMS deferred via sister gap GAP-063b filed by closure PR). Phase 1 deliverables: `NotificationChannel` interface + `SESEmailService` implementing it + `NotificationPreference` entity + V23 migration + CRUD service/controller + settings UI + business docs 3-layer. Existing email callers unchanged (backward compat verified via full subscription test suite + email module test suite). 11 new unit tests for service + controller; 5 contract tests for interface; 4 FE page tests. State-check 2026-05-04: V18 (GAP-098) instance-level columns retained as legacy fallback per BR-NOTIF-006.
- **2026-04-14** — Persona review — critical VN market fit. Original P1 priority assigned (later elevated to P0 by Wave 11 outside-in audit 2026-06-02).
