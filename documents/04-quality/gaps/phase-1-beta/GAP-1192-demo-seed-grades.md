---
id: GAP-1192
title: Seed điểm (grades) 2 tenant demo theo grading_scales V88 (init + finalize)
status: PARTIAL
priority: P1
domain: Backend
phase: phase-1-beta
created: 2026-06-11
last_verified: 2026-06-11
completion_pct: 70
---

# GAP-1192 — Seed điểm (grades) 2 tenant demo

## Problem

2 tenant demo thiếu grade records → bảng điểm rỗng. Cần seed điểm theo `grading_scales` (V88 seed 8 bands) với chuỗi init → finalize để demo nghiệp vụ chấm điểm. Wave plan Bucket C (cùng `DemoAcademicSeeder`).

## Proposed Fix

Seed grades cho student theo môn/cấp (Hà Toán TH, Nhì Hóa THCS), dùng `grading_scales` V88 8 bands, chuỗi grade init + finalize.

## Acceptance Criteria

- [x] `DemoAcademicSeeder` seed grades init+finalize (PR #2315, code shipped)
- [ ] G1: `psql` assert grade rows tồn tại + band khớp grading_scales V88
- [ ] G2 human walk: bảng điểm hiện data thật (blocked GAP-1180)

## Related

- Code shipped: PR #2315 `DemoAcademicSeeder.java`
- Wave: `wave-2026-06-11-demo-seed-1-2tenant-full.md` Bucket C
- Ref: `grading_scales` V88 migration
- Depends: [[GAP-1190]] (student/enrollment), prerequisite walk [[GAP-1180]]
- Sibling: [[GAP-1191]] (điểm danh)
