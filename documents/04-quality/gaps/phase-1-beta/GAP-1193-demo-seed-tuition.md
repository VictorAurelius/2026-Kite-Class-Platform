---
id: GAP-1193
title: Seed học phí 2 tenant demo (Hà invoice thủ công + Nhì pricing nhiều mức + payment records)
status: PARTIAL
priority: P1
domain: Backend
phase: phase-1-beta
created: 2026-06-11
last_verified: 2026-06-11
completion_pct: 70
---

# GAP-1193 — Seed học phí 2 tenant demo

## Problem

2 tenant demo thiếu học phí (invoice + payment) → không demo được doanh thu/công nợ. Hà: hóa đơn thủ công + chuyển khoản (100% paid). Nhì: bảng giá nhiều mức + multi payment (80% paid — còn công nợ demo). Wave plan Bucket D (cùng `DemoAcademicSeeder`).

## Proposed Fix

Seed invoice + payment records: Hà BANK_TRANSFER 100% paid; Nhì pricing tiers nhiều mức + payment records 80% paid (demo doanh thu + công nợ). Tiền VND.

## Acceptance Criteria

- [x] `DemoAcademicSeeder` seed invoice + payment (PR #2315, code shipped)
- [ ] G1: `psql` assert invoice/payment rows + Hà 100% paid / Nhì còn công nợ
- [ ] G2 human walk: dashboard học phí hiện data thật (blocked GAP-1180)

## Related

- Code shipped: PR #2315 `DemoAcademicSeeder.java`
- Wave: `wave-2026-06-11-demo-seed-1-2tenant-full.md` Bucket D
- Depends: [[GAP-1190]] (student/enrollment), prerequisite walk [[GAP-1180]]
