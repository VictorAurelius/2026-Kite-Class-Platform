# GAP-1209: DemoAcademicSeeder tạo course DRAFT — catalog public (filter PUBLISHED) trống

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-11 (user G2★ walk follow-up — catalog trống kể cả sau fix GAP-1207)
**Affects:** `kiteclass-core/dev/seeder/DemoAcademicSeeder.java`

## Problem

Seeder tạo course demo (TOAN-HA-L4/L5, HOA-NHI-8A/8B/9A/9B) qua `createCourse` → status DRAFT mặc định, không publish → catalog public (filter `status=PUBLISHED`) trống cho demo-trio. Marker-skip path cũng không reconcile rows cũ.

## Fix (shipped PR #2326)

`publishCourse(course.id())` ngay sau create + reconcile pass `publishDemoCourses(spec)` trong marker-skip branch (publish các DRAFT demo courses hiện có — mirror tinh thần upsert GAP-1203).

## Acceptance Criteria

- [x] Re-boot core → demo courses PUBLISHED (log reconcile + DB verify)
- [x] Catalog demo-trio hiển thị khóa học (re-walk evidence PR)

## Related

- Sister: GAP-1203 (seeder upsert), GAP-1207 (catalog 400), GAP-1190..1193 (seeder gốc)
- Discovered in: user G2★ walk 2026-06-11
