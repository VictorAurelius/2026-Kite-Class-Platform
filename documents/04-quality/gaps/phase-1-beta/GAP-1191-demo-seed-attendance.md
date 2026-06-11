---
id: GAP-1191
title: Seed điểm danh (attendance) cho buổi học 2 tenant demo (Nhì tỷ lệ cao demo "báo cáo nâng cao")
status: PARTIAL
priority: P1
domain: Backend
phase: phase-1-beta
created: 2026-06-11
last_verified: 2026-06-11
completion_pct: 70
---

# GAP-1191 — Seed điểm danh (attendance) 2 tenant demo

## Problem

2 tenant demo thiếu attendance records → dashboard điểm danh rỗng, không demo được "báo cáo chuyên cần" (Hà cơ bản, Nhì nâng cao per thesis §4.4). Wave plan Bucket C (cùng `DemoAcademicSeeder`).

## Proposed Fix

Seed attendance records cho N buổi gần nhất mỗi class: Hà ~75% chuyên cần (giao diện cơ bản), Nhì ~88% (báo cáo nâng cao demo). Gắn vào session đã seed (GAP-1190).

## Acceptance Criteria

- [x] `DemoAcademicSeeder` seed attendance (PR #2315, code shipped)
- [ ] G1: `psql` assert attendance rows tồn tại cho 2 tenant + tỷ lệ Nhì > Hà
- [ ] G2 human walk: dashboard điểm danh hiện data thật (blocked GAP-1180)

## Related

- Code shipped: PR #2315 `DemoAcademicSeeder.java`
- Wave: `wave-2026-06-11-demo-seed-1-2tenant-full.md` Bucket C
- Depends: [[GAP-1190]] (session/class), prerequisite walk [[GAP-1180]]
- Sibling: [[GAP-1192]] (điểm)
