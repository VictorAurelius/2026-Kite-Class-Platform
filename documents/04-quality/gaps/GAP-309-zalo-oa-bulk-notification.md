# GAP-309: Zalo Official Account Bulk Notification + Targeted Alerts + Daily Digest

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (business-logic tier — communication backbone for VN market)
**Domain:** Backend / Notification / Integration
**Found:** 2026-05-04 (Wave 17 Bucket C P3 persona review — round 1)
**Affects:** P3 Medium Center (500 parents bulk), P5 K-12 School (2400 parents), P2 Small Center (60 parents), P1 Solo Teacher (15 parents)

## Problem

Zalo OA là kênh primary cho parent communication tại VN market. P3 cần:
- Bulk notification 500 parents (250 students × 2) ≤2 phút với delivery receipt
- Targeted alerts (filter by class / grade level)
- Daily digest option (avoid spam khi student × 5 teachers post events)
- 1:1 chat platform-mediated (parent ↔ teacher) với 24-month archive cho compliance
- Auto-trigger SMS/Zalo on attendance absence (per AC-OPS-002)

State-check 2026-05-04:
- `payment/gateway/impl/ZaloPayGatewayClient.java` exists but is **payment gateway only**, NOT Zalo OA
- `grep -rln "ZaloOA\|ZaloOfficial\|OAccount\|zalo.oa" kiteclass kitehub --include="*.java"` → 0 results
- No `notification/` module, no SMS gateway, no email-batch service
- `ParentInvitationService` does parent linking but no broadcast notification

Affects ACs (collected across personas): P3 AC-OPS-002 (parent SMS on absence), AC-COMM-001/002/003, admin-in-P3 AC-COMM-001/002, teacher-employee-in-P3 AC-OPS-002, AC-COMM-001/002, student-in-P3 AC-ONBOARD-001 (Zalo credentials), AC-COMM-001/002/003. Total ~12 ACs blocked.

Existing GAP-063 (SMS/Zalo notification) claims OPEN but state-check confirms ZERO implementation; this gap supersedes / re-scopes GAP-063.

## Root Cause

Wave 1-16 prioritized core domain (enrollment, invoice, branding). Notification deferred. Confusion between ZaloPay (payment) and Zalo OA (Official Account messaging) — different APIs, different SDKs.

## Proposed Fix

3-phase delivery:

**Phase 1 — Notification core + Zalo OA adapter** (Wave 18)
- `kiteclass-core/module/notification/` module
- `NotificationService` + `NotificationChannel` enum (ZALO_OA, SMS, EMAIL, PUSH, IN_APP)
- `ZaloOaAdapter` (Zalo OA SDK integration — auth flow + send template message)
- `SendNotificationRequest` DTO + audience filter (class, grade, tag)
- API: `POST /api/v1/notifications/broadcast`

**Phase 2 — Bulk + targeted + delivery receipt** (Wave 18-19)
- Bulk send queue (RabbitMQ) for 500+ recipients
- Audience filter: class_id list, grade level, subject, tag-based
- Delivery receipt webhook from Zalo OA → store in `notification_delivery_log`
- Failure list export CSV for follow-up via SMS

**Phase 3 — Daily digest + 1:1 chat + auto-triggers** (Wave 19-20)
- Daily digest aggregation per parent (configurable cutoff time, default 8pm)
- 1:1 chat: `Conversation` entity + 24-month retention per Consumer Protection Law
- Attendance auto-trigger: hook into `attendance.recorded` event → if absent, fire notification
- Monthly progress report scheduler (cron 1st of month)

## Acceptance Criteria

- [ ] Phase 1: `notification/` module + Zalo OA adapter + SendNotification API
- [ ] Phase 1: SMS adapter (Viettel / VinaPhone / MobiFone gateway)
- [ ] Phase 2: Bulk send 500 recipients in <2 minutes
- [ ] Phase 2: Audience filter supports class / grade / subject / tag
- [ ] Phase 2: Delivery receipt webhook + audit log
- [ ] Phase 3: Daily digest aggregation + per-parent preference toggle
- [ ] Phase 3: 1:1 chat with 24-month archive per Consumer Protection Law 2023
- [ ] Phase 3: Attendance absence auto-fires notification within 5 minutes of teacher mark
- [ ] Each phase: `documents/01-business/kiteclass/notification/{rules,use-cases,api-contract}.md` ships with code

## Related

- Audit report: `documents/00-brd/persona-reviews/P3-medium-center-round-1-2026-05-04.md` §Critical Findings #4
- Existing gap (re-scope): GAP-063 (SMS/Zalo notification integration)
- Compliance: Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 (24-month dispute window), PDPL 2023 (consent)
- Persona AC: 12 ACs across P3 + admin + teacher + student personas
