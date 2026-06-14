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

- [x] V87 source thêm normalization UPDATE trước ADD CONSTRAINT (đã landed PR #2274, commit `cda3a40b4`)
- [x] kiteclass-core rebuild + boot healthy (Flyway V87+V88 applied, không crash-loop) — chứng minh qua Testcontainers full-chain migrate (production-equivalent boot path): seed 4 row lowercase ở V86 → migrate V87..V98 sạch (không SQLSTATE 23514) → `flyway_schema_history` V87+V88 `success=true`. Literal container health-check còn cần human/CI Docker rebuild.
- [x] `chk_attendance_status` tồn tại trên `attendance`; 0 row vi phạm — verified trên Testcontainers schema đã giữ row lowercase (reproduce kiteclass_shared): constraint UPPERCASE-only (cho phép MAKEUP, cấm lowercase), 0 row `status <> UPPER(status)`. Literal `kiteclass_shared` instance là live-DB confirmation.
- [ ] Re-walk KC-5 attendance scope spot-check (per `pre-handoff-self-test-completeness.md` §3) — không regress — **còn pending**: PR này chỉ thêm test (không đổi app-code attendance → không regress KC-5), nhưng live re-walk qua full Docker stack + browser vẫn cần human/CI.

## Resolution (Wave wave-2026-06-14-p0-closeout-1 Bucket C — 2026-06-14)

- **Phát hiện:** normalize UPDATE đã có sẵn trong V87 (lines 22-28, landed PR #2274 2026-06-09). Empirical Flyway config check: `validate-on-migrate` default true, `baseline-on-migrate: true`, không out-of-order/repair — nhưng V87 chưa apply ở đâu nên sửa V87 không gây checksum risk (confirms gap §23). Không cần forward-migration V99.
- **Bucket scope:** thêm regression test bảo vệ fix (gap thiếu automated guard). CI-bound `*Test` (surefire gate), KHÔNG `*IT`.
- **Test:** `kiteclass-core/src/test/java/com/kiteclass/core/module/attendance/migration/V87AttendanceStatusNormalizeTest.java` — Testcontainers 2-phase: (1) `Flyway.target("86")` rồi seed 4 row lowercase (`session_replication_role=replica` để skip FK chain attendance→class_sessions→classes→courses; CHECK/NOT NULL vẫn enforce); (2) migrate V87..V98 → assert: rows → UPPERCASE; 0 row vi phạm; constraint UPPERCASE-only + MAKEUP; lowercase bị reject; UPPERCASE/MAKEUP được chấp nhận; normalize UPDATE idempotent (re-run = 0 row). 6/6 PASS dưới `-P strict-warnings`.

## Related

- Resolved in: PR #<this PR>, branch `fix/wave-p0-c-attendance-normalize` (Wave wave-2026-06-14-p0-closeout-1 Bucket C)
- Discovered in: fix branch `fix/v87-attendance-status-normalize-kc5` (G2 readiness check 2026-06-08)
- GAP-996 (V87 original — schema drift fix, incomplete: missing data normalization)
- Flow Verification Campaign KC-5 row (V87 P0 schema drift)
