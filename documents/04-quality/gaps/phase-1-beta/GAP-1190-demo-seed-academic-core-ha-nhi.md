---
id: GAP-1190
title: Seed academic core 2 tenant demo (Hà FREE + Nhì PAID) — course/class/student/enrollment/schedule
status: PARTIAL
priority: P1
domain: Backend
phase: phase-1-beta
created: 2026-06-11
last_verified: 2026-06-11
completion_pct: 70
---

# GAP-1190 — Seed academic core 2 tenant demo (Hà + Nhì)

## Problem

Landing-100 seed chỉ ở mức branding + landing-hero (audit `2026-06-11-demo-trio-seed-coverage-audit.md` 38/100). 2 tenant demo thực tế (cô Hà FREE / thầy Nhì PAID) chưa có data nghiệp vụ academic core: course, class, student, enrollment, schedule/session → landing đẹp vỏ rỗng ruột, không demo được nghiệp vụ trường như thesis §4.3/4.4.

Wave plan: `documents/03-planning/waves/wave-2026-06-11-demo-seed-1-2tenant-full.md` Bucket A (Hà FREE-quota) + Bucket B (Nhì PAID-unlimited).

## Proposed Fix

`DemoAcademicSeeder.java` (`@Profile("dev")`, idempotent) seed chuỗi mỗi tenant: teacher → course → class → student → enrollment → schedule/session. Hà FREE-limited (2 lớp Toán TH, ~12 HS); Nhì PAID-scale (4 lớp Hóa THCS, ~35 HS). UUID khớp BrandingDataSeeder `a1100000`/`b1100000`.

## Acceptance Criteria

- [x] `DemoAcademicSeeder.java` ship academic core 2 tenant (PR #2315, code shipped + compile PASS)
- [ ] G1 full: dev profile boot → `psql` assert Hà ≥10 HS + Nhì ≥30 HS, idempotent re-run không duplicate
- [ ] G2 human walk: landing Hà + Nhì qua FE `:3000` → dashboard hiện lớp/HS thật (blocked GAP-1180 by-subdomain resolve)

## Related

- Code shipped: PR #2315 `DemoAcademicSeeder.java`
- Wave: `wave-2026-06-11-demo-seed-1-2tenant-full.md` Bucket A+B
- Prerequisite walk: [[GAP-1180]] (kitehub instances by-subdomain resolve)
- Sibling: [[GAP-1191]] [[GAP-1192]] [[GAP-1193]] (cùng seeder), [[GAP-1194]] [[GAP-1195]] (Bucket E/F)
