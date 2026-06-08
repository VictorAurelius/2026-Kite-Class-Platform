# GAP-1066: V87 thêm chk_attendance_status UPPERCASE không normalize data cũ → kiteclass-core crash-loop

**Status:** 🟡 PARTIAL
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-08 (G2 readiness check — Flow Verification Campaign, trước khi bắt đầu đợt G2 core spine)
**Affects:** `kiteclass/kiteclass-core/src/main/resources/db/migration/V87__fix_attendance_enrollment_model_drift.sql`; DB `kiteclass_shared`; chặn boot kiteclass-core → toàn bộ flow KC (KC-1..8, KC-10..12)

## Problem

V87 (GAP-996, Wave flow-kc5) thay `chk_attendance_status` lowercase bằng constraint UPPERCASE `{PRESENT,ABSENT,LATE,EXCUSED,MAKEUP}` nhưng **không normalize row cũ trước khi `ADD CONSTRAINT`**.

`kiteclass_shared` có **450 row `attendance` status lowercase** (`present/absent/late/excused` — legacy seed/restore). Khi kiteclass-core boot, Flyway chạy V87 (DB đang ở V86, V87 chưa từng apply ở bất kỳ DB nào):

```
SQL State  : 23514
Message    : ERROR: check constraint "chk_attendance_status" of relation "attendance" is violated by some row
Location   : V87__fix_attendance_enrollment_model_drift.sql Line 21
```

→ migration fail → bean `flywayInitializer` fail → app context fail → container restart → **crash-loop (1230 restarts)** → core không bao giờ healthy → chặn mọi flow KC G2.

Empirical state-check (per `release-fix-retry-budget.md` §3.5): chỉ `kiteclass_shared` fail (450 bad rows); per-tenant DB ở V18 (scope khác, GAP-984 unused); V87 chưa apply ở đâu → **không có checksum risk** khi sửa V87.

## Proposed Fix

Thêm `UPDATE attendance SET status = UPPER(status) WHERE status IS NOT NULL AND status <> UPPER(status)` ngay sau `DROP CONSTRAINT` và trước `ADD CONSTRAINT` trong V87. Idempotent + durable (survive fresh reseed). An toàn vì V87 chưa apply (no checksum mismatch).

## Acceptance Criteria

- [x] V87 source thêm normalization UPDATE trước ADD CONSTRAINT
- [ ] kiteclass-core rebuild + boot healthy (Flyway V87+V88 applied, không crash-loop)
- [ ] `chk_attendance_status` tồn tại trên `kiteclass_shared.attendance`; 0 row vi phạm
- [ ] Re-walk KC-5 attendance scope spot-check (per `pre-handoff-self-test-completeness.md` §3) — không regress

## Related

- Discovered in: fix branch `fix/v87-attendance-status-normalize-kc5` (G2 readiness check 2026-06-08)
- GAP-996 (V87 original — schema drift fix, incomplete: missing data normalization)
- Flow Verification Campaign KC-5 row (V87 P0 schema drift)
