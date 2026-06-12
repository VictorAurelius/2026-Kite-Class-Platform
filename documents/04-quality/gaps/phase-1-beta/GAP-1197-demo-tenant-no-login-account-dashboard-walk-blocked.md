---
id: GAP-1197
title: Demo tenant (Hà/Nhì) có academic data nhưng chưa seed login account → browser dashboard G2 walk blocked
status: OPEN
priority: P2
domain: Backend
phase: phase-1-beta
created: 2026-06-11
last_verified: 2026-06-11
completion_pct: 0
---

# GAP-1197 — Demo tenant chưa seed login account (dashboard browser-walk blocked)

## Problem

Discovered khi soạn G2 recipe demo-seed-1 (`2026-06-11-g2-recipe-demo-seed-1.md`): `DemoAcademicSeeder` tạo Teacher + Student **domain entities** (email `gv.<suffix>@demo.kitehub.me`, `hs.<suffix>.<n>@...`) nhưng **KHÔNG seed login account/password** (auth user). `BrandingDataSeeder` cũng chỉ seed branding/landing.

→ Demo tenant Hà (`a1100000…0001`) / Nhì (`b1100000…0002`) có đầy đủ lớp/HV/điểm danh/điểm/học phí (GAP-1190..1193) NHƯNG **không log in được** → G2 browser dashboard walk (verify academic data hiện trên UI thật) chưa làm được. Hiện chỉ verify qua psql DB-assert (G1).

Per `feature-ship-runtime-walk-mandate` + `g1-browser-walk-before-flip`: academic feature user-facing cần browser walk thật trước DONE. Thiếu login → academic gaps (1190..1193) giữ PARTIAL.

## Proposed Fix

Seed dev login account cho demo teacher mỗi tenant (vd `gv.<suffix>@demo.kitehub.me` + password dev biết, role TEACHER, gắn tenant + teacher entity) trong `@Profile("dev")` seeder. Đảm bảo auth chain (login → JWT → tenant context → dashboard) hoạt động cho demo tenant. Document creds trong G2 recipe.

## Acceptance Criteria

- [ ] Dev seeder tạo login account demo teacher 2 tenant (Hà/Nhì), role + tenant đúng.
- [ ] Browser login `gv.<suffix>@demo.kitehub.me` → dashboard tenant tương ứng (không cross-tenant leak).
- [ ] G2 dashboard walk: hiện lớp/HV/điểm danh/điểm/học phí thật (GAP-1190..1193) trên UI.
- [ ] G2 recipe cập nhật creds + dashboard walk steps.

## Related

- Discovered in: G2 recipe demo-seed-1 soạn 2026-06-11
- [[GAP-1190]] [[GAP-1191]] [[GAP-1192]] [[GAP-1193]] academic core (blocked browser-walk bởi gap này)
- Recipe: `documents/05-guides/operations/2026-06-11-g2-recipe-demo-seed-1.md` §7 open item
- Rule: `feature-ship-runtime-walk-mandate` (browser walk before DONE), `g1-browser-walk-before-flip`
