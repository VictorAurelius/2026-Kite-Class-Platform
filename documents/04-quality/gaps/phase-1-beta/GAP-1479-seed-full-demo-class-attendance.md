# GAP-1479: Committed idempotent seed cho 1 lớp demo đầy đủ (12 hs + ~12 buổi + điểm danh phân bố thật)

**Status:** 🟡 PARTIAL
**Priority:** 🟡 P2
**Domain:** Seed / DevOps
**Found:** 2026-06-17 (KC-3 attendance walk — báo cáo điểm danh rỗng/không trực quan)
**Affects:** `kitehub/scripts/seed-attendance-demo.sh` (mới) · tenant `g2walk` walk baseline

## Problem

Khi walk `/attendance/reports` (KC-3 attendance), `seed-walk-tenant.sh` chỉ tạo 1 lớp với **5 học sinh + 1 buổi + 1 bản ghi điểm danh** → báo cáo điểm danh rỗng/không thực tế, không demo được phân bố Có mặt/Vắng/Trễ/Phép. User yêu cầu "seed data đầy đủ cho 1 lớp học để xem cho trực quan".

## Proposed Fix

Thêm committed idempotent seed `kitehub/scripts/seed-attendance-demo.sh` (per `walk-data-committed-seed.md`) tạo 1 **lớp demo độc lập** "Lớp Demo Báo Cáo" trên tenant `g2walk`:
- **12 học sinh** Vietnamese names (`demo_hv1..12@g2walk.vn`), enroll + ACTIVE (GAP-1474 confirm-payment).
- **~12 buổi học** qua `POST /api/v1/classes/{id}/schedule` (Thứ 2-4, dates **quá khứ** −28..+14 ngày → buổi đã diễn ra để điểm danh thật).
- **Điểm danh phân bố thật** trên các buổi đã diễn ra (bulk `POST /api/v1/attendance/classes/{cid}/sessions/{sid}/attendance`, teacher token): deterministic per (student×session) — ~78% PRESENT, rải LATE/ABSENT/EXCUSED/MAKEUP, **tỷ lệ khác nhau mỗi học sinh** → biểu đồ "Phân bố trạng thái" + per-student rate non-trivial.

Idempotent: fetch-by-key (email/code/name), 409 skip, attendance đã có → skip. Standalone (tự đảm bảo teacher + course tồn tại).

## Acceptance Criteria

- [x] Script committed dưới `kitehub/scripts/` (KHÔNG scratch) + idempotent + shellcheck clean
- [x] 12 học sinh + ~12 buổi + điểm danh phân bố thật, tỷ lệ khác nhau/hs
- [x] Dates quá khứ → buổi đã diễn ra (điểm danh thật, không future)
- [ ] Coordinator chạy seed trên running stack → báo cáo demo phong phú (chờ chạy)
- [ ] Human walk `/attendance/reports` lớp demo → phân bố trực quan (chờ G2 walk)

## Related

- Discovered in: KC-3 attendance walk 2026-06-17 (sau GAP-1474 roster + GAP-1476 report fix)
- Sister: GAP-1474 (enroll ACTIVE seed) · GAP-1476/1477 (attendance API envelope) · GAP-1478 (report XLSX export)
- Rule: `walk-data-committed-seed.md` §3 (committed idempotent seed)
