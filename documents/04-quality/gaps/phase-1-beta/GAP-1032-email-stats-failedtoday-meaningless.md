# GAP-1032: Admin email stats `failedToday` luôn ~0 — `email_sent_log` thiếu status/error column + orphan `email_logs` table

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (kitehub-subscription)
**Found:** 2026-06-06 (KH-10 G1 walk, FM-2)
**Affects:** `EmailAdminService.getEmailStats()` + `email_sent_log` (V11) + orphan `email_logs` (V5)

## Problem

KH-10 G1 walk: `GET /api/platform/admin/emails/stats` trả `failedToday:0` — và **luôn luôn ~0** bất kể email fail thật hay không, nên metric vô nghĩa cho admin.

Walk evidence: `{"totalSentToday":11,"totalSentThisWeek":17,"failedToday":0,"countByType":{...}}` — `/history` + `/stats` trả 200 (không crash), nên đây là **data-semantics issue**, không phải lỗi 500.

## Root Cause

- `email_sent_log` (migration V11) **không có column `status` / `error`** — chỉ ghi email đã gửi. `EmailAdminService.getEmailStats()` tính `failedToday` qua `countByEmailTypeContaining(...)` không map được trạng thái fail thật → đếm ra ~0.
- Tồn tại **2 bảng email**: `email_sent_log` (V11, entity dùng) + `email_logs` (V5, orphan). Drift — cần xác định canonical table.

## Proposed Fix

1. Thêm `status` (SENT/FAILED) + `error_message` column vào `email_sent_log` (Flyway V+1) + ghi status khi send.
2. `getEmailStats().failedToday` đếm theo `status=FAILED AND sent_at::date = today`.
3. Reconcile / drop orphan `email_logs` (V5) nếu không dùng.

## Acceptance Criteria

- [ ] `email_sent_log` có `status` column; send path ghi SENT/FAILED
- [ ] `failedToday` phản ánh fail thật (test: force 1 fail → count = 1)
- [ ] Orphan `email_logs` table được reconcile (drop hoặc document lý do giữ)

## Related

- Discovered in: KH-10 G1 walk (Wave flow-kh10), pre-walk FM-2
- Sister schema-drift class: GAP-996 (KC-5 V87), GAP-998 (KC-6 V88)
