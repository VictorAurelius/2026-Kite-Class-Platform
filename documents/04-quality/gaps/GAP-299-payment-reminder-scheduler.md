# GAP-299: Payment Reminder Scheduler (3-day pre-due + due-date)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core invoice + scheduling) + integration with notification channel
**Found:** 2026-05-04 (Wave 17 Bucket B — P2 persona review)
**Affects:** P1, P2, P3 (anywhere parents pay monthly tuition with due-date) — every active tenant

---

## Problem

P2 owner expects auto-reminder for unpaid tuition: 3 days before due-date + on due-date. Currently:

- No scheduler/cron job that scans `Invoice` rows for upcoming due dates
- No reminder template ("Học phí tháng X còn 1M chưa thanh toán, hạn 5/X")
- No suppress-after-paid logic

P2 review evidence: AC-COMM-003 FAIL — no scheduler module found; depends on GAP-063 (Zalo channel) for delivery but the scheduler logic itself is also missing.

## Root Cause

Invoice module shipped without lifecycle automation. Owner currently does manual chase via personal Zalo (the Job-To-Be-Done that drove them to consider the platform).

## Proposed Fix

1. Spring `@Scheduled` job (daily 9am Asia/Ho_Chi_Minh) scanning unpaid invoices where `due_date - today ∈ {3, 0}`.
2. For each match, enqueue notification job with template (i18n key `notif.payment.reminder.preDue3` / `notif.payment.reminder.dueToday`).
3. Suppress: if invoice paid since last scan, skip. Track `last_reminder_sent_at` to avoid duplicate.
4. Tenant config: opt-in/out via `tenant_settings.reminder_enabled` (default true).
5. Per-invoice override: parent can request "stop reminders" (creates `reminder_suppressed` flag).
6. Channel selection: prefer Zalo (GAP-063), fallback SMS, fallback email.

## Acceptance Criteria

- [ ] `@Scheduled` job runs daily 09:00 Asia/Ho_Chi_Minh
- [ ] Reminder templates exist for both windows (pre-due 3-day + due-day)
- [ ] Idempotency: invoice receives at most 1 pre-due reminder + 1 due-day reminder
- [ ] Tenant opt-out config respected
- [ ] Performance: scans 10K invoices in <30 sec (P5 K-12 scale)
- [ ] Test: integration test seeding invoices with various due-dates → run scheduled job → assert correct reminders queued
- [ ] Test: paid invoice does NOT receive reminder

## Related

- Parent review: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` AC-COMM-003
- Depends on: [GAP-063](GAP-063-sms-zalo-notification-integration.md) for Zalo/SMS dispatch
- Cluster: end-of-month closeout (with GAP-297 + GAP-298)
