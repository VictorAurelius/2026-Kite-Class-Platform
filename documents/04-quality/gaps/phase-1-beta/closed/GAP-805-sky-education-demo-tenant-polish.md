---
audience: dev
---

# GAP-805 — Sky Education polished demo tenant

**Status:** 🟡 PARTIAL (build code wave demo-tenant-1; live-walk deferred stack down)
**Priority:** 🟠 P1
**Domain:** Mixed (kiteclass-core seeder + kiteclass-frontend + seed scripts)
**Found:** 2026-05-28 (user direction — demo tenant chứng minh tùy biến UI; outside-in 2 agents)
**Phase:** phase-1-beta
**Affects:** Demo cho prospect chủ trung tâm + hội đồng bảo vệ đồ án

## Problem

Tenant demo "Sky Education" hiện trông như mockup giả — 3 lý do P0 (outside-in findings):
1. Theme mặc định (BrandingDataSeeder chỉ seed `thanglong`) → tùy biến UI không thể hiện.
2. Dashboard overview KPI hardcode literal không khớp DB.
3. Attendance + grade + payment trống (seed không tạo).
Plus P1: data nông, ảnh logo/cover/hero vỡ.

Chi tiết findings + plan: `documents/03-planning/waves/wave-demo-tenant-1-sky-education-polish.md`.

## Proposed Fix (3 bucket parallel)

- **A:** seed branding Sky tenant (`BrandingDataSeeder.java`) — tên/3 màu/tagline/logoUrl direct.
- **B:** seed attendance/grade/payment + enrich (3-4 lớp, ~20-30 HS/lớp) — seed script.
- **C:** fix overview KPI hardcode (kiteclass-frontend) + asset seed-direct MinIO.

## ✅ RESOLVED — tenant-id reconciliation (Option C investigate → fix applied this PR)

Tenant-id investigation (Option C) xác nhận **conflation bug**: `1111…` gán cho CẢ thanglong (BrandingDataSeeder) lẫn Sky (seed-thesis tenant_a) → phá multi-tenant isolation. Tenant key thật = `BaseEntity.instance_id` (resolve header `X-Tenant-Id`).

**Fix applied (this PR):** Sky = `a5e00000-0000-0000-0000-000000000001` độc lập (khớp branding agent đã seed):
- `seed-thesis-demo-tenants.sh:60` `TENANT_A_ID` → a5e0…0001 (+ header comment line 14).
- `seed-sky-demo-enrich.sh:61` `SKY_ID` + line 185 PL/pgSQL `sky_id` hardcode → a5e0…0001 (cross-flow sweep bắt hardcode trong DO block).
- thanglong giữ `1111…` (dev default, không động — ~15 test ref `1111…` về thanglong không ảnh hưởng).
- ⚠️ Live-walk phải gửi `X-Tenant-Id: a5e0…0001` cho Sky + chạy `--cleanup` data `1111…` cũ trước reseed.

<details><summary>Lịch sử Open item (trước khi resolve)</summary>

3 agent build độc lập → mismatch tenant id phải reconcile trước khi demo coherent:
- **A (branding)** seed Sky branding dưới instance id **`a5e00000-...-000000000001`** (UUID độc lập — A tránh `11111111-...` vì nó TRÙNG `DEV_TENANT_ID` thanglong trong seeder cũ).
- **B (data)** seed attendance/grade/payment dưới Sky tenant **`11111111-...`** (từ `seed-thesis-demo-tenants.sh` tenant_a).
- **Hệ quả:** branding (a5e0…) ≠ data (1111…) → theme tùy biến KHÔNG apply lên tenant có data. Demo sẽ thấy data nhưng theme default, HOẶC theme custom nhưng tenant trống.
- **Resolve options:** (a) đổi A's branding target → `11111111-...` (nhưng cần verify không đè branding thanglong vì thanglong dùng cùng id — data-model conflation cần làm rõ); (b) đổi B's enrich script seed lên `a5e0…0001` + seed teacher/course gốc cho tenant đó (không phụ thuộc seed-thesis); (c) làm rõ id tenant Sky canonical rồi cả A+B dùng chung. **Quyết định ở live-walk khi stack up + thấy được render thực tế.**

</details>

## Acceptance Criteria

- [ ] **Reconcile tenant id** A-branding ↔ B-data (xem Open item) — branding + data CÙNG 1 tenant
- [ ] Branding Sky tenant seeded → theme tùy biến render live (CSS vars + public theme endpoint)
- [ ] Overview KPI khớp DB thật (không hardcode)
- [ ] Attendance + grade + payment có data demo
- [ ] Enrich: ≥3 lớp, ≥20 HS/lớp, tên VN đa dạng
- [ ] Asset: logo + cover + hero hiển thị (seed-direct MinIO OR placeholder đẹp)
- [ ] **Live-walk** (deferred — stack down): owner login → branding custom + KPI khớp + 3 surface có data (per `feature-ship-runtime-walk-mandate.md`)

## Related

- Wave `wave-demo-tenant-1-sky-education-polish.md` — deliver gap này
- **GAP-804** — logo upload contract drift (workaround: seed logoUrl direct)
- **GAP-798b** — chặn StorageController asset (KHÔNG chặn branding); asset workaround seed-direct MinIO
- `seed-thesis-demo-tenants.sh` — base seed 2 tenant
- `feature-ship-runtime-walk-mandate.md` §5 — live-walk defer hợp lệ (stack down)

## Log

- **2026-05-28:** Filed từ user direction "lập wave demo-tenant + spawn agents". Outside-in 2 agents (persona-sim + failure-mode) cung cấp findings. Wave demo-tenant-1 build via 3 parallel Opus agents. PARTIAL — code build session này, live-walk deferred tới khi stack up.
