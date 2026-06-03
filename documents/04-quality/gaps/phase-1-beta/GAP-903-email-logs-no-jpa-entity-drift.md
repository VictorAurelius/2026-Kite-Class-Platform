# GAP-903: `email_logs` (V5) không có JPA entity — schema-trước-code drift

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KH email/compliance/admin)
**Affects:** `kitehub-email` `email_logs` table

## Problem

V5 tạo `email_logs` với 23 cột đầy đủ (tracking AWS SES vòng đời, retry, bounce). Migration tồn tại, RLS V34+V50 cover. Nhưng **không có file `EmailLog.java`** trong codebase prod (grep `email_logs` ở module java = 0 hit). Chỉ `email_sent_log` (V11, 5 cột idempotency) có entity `EmailSentLog` ở `kitehub-platform` (không phải `kitehub-subscription` — cross-module ownership drift).

Khả năng: (1) bảng truy cập qua native SQL trong `kitehub-email` (separate service) — chưa verify; (2) dead-code shipped; (3) deprecated by `EmailSentLog`.

2 bảng email mục đích chồng lấn schema rất khác.

## Proposed Fix

Verify owner trong session sau (grep `kitehub-email` service code). Decide: (a) tạo `EmailLog` entity nếu service dùng; (b) drop `email_logs` migration nếu dead-code; (c) deprecate `email_sent_log` nếu `email_logs` mới canonical.

## Acceptance Criteria

- [ ] Verify `email_logs` usage ở kitehub-email service
- [ ] Decision documented per outcome
- [ ] Reference cluster doc KH 04-email-compliance-admin §A2

## Discovered in

`documents/02-architecture/database/kitehub/04-email-compliance-admin.md` §A2
