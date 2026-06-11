---
id: GAP-1180
title: Demo-trio không có seeder kitehub-side instances → by-subdomain 404 (seed-coverage gap cross-service + UUID mismatch + recipe §2.2 sai)
status: PARTIAL
priority: P1
domain: Backend
phase: phase-1-beta
created: 2026-06-11
last_verified: 2026-06-11
---

# GAP-1180 — Demo-trio thiếu seeder kitehub `instances` (by-subdomain resolve không seedable)

## Problem

Phát hiện 2026-06-11 khi verify seed cho landing-100 G2★ nip.io subdomain walk (recipe `2026-06-11-g2-recipe-landing-100-subdomain.md`). `curl :9000/api/v1/public/tenants/by-subdomain/co-ha-toan` → **404 TENANT_NOT_FOUND** (cả `thay-nhi-hoa`). Walk bị chặn ngay bước 1.

Truy root cause (empirical, design-first):

1. **Endpoint by-subdomain ở kitehub-subscription** — `PublicTenantController.findBySubdomainAndDeletedFalse` đọc bảng **kitehub `instances`** (DB `kitehub`).
2. **BrandingDataSeeder (kiteclass-core, `@Profile("dev")`)** chỉ ghi `FrontendInstance` + `Branding` vào **kiteclass-side** — comment dòng 58 *giả định* "Instance UUID matches the kitehub gateway instances row" nhưng **KHÔNG tạo** kitehub `instances` row.
3. **Không seeder nào populate kitehub `instances` cho demo-trio** → `by-subdomain` 404. Cross-service seed-coverage gap: 1 seeder (kiteclass) phụ thuộc dữ liệu service khác (kitehub) mà không có seeder cho phía kia.
4. **UUID mismatch 2 nguồn:**
   - `BrandingDataSeeder`: `HA_TENANT_ID=a1100000-0000-4000-a000-000000000001` (co-ha-toan), `NHI_TENANT_ID=b1100000-...-002` (thay-nhi-hoa)
   - `kitehub/scripts/seed-landing-content.sql`: `ad0fa96e-...` (co-ha-toan), `0abe093c-...` (thay-nhi-hoa)
   - 2 scheme khác nhau cho cùng tenant → branding/landing link sai nếu trộn.
5. **Recipe §2.2 inaccurate:** nói "BrandingDataSeeder (dev) đã seed → by-subdomain resolve 200", nhưng seeder kiteclass không chạm bảng kitehub resolve. Recipe troubleshooting "re-run dev seeder" cũng không fix vì sai service.
6. **kiteclass-core image stale (phụ):** log seeder chỉ thấy `thanglong`/`sky-education`, không thấy trio → trio-seed code chưa có trong image đang chạy. (Rebuild kiteclass-core 2026-06-11 để có latest seeder + Flyway V95 landing-100.)

## Manual unblock đã làm (KHÔNG phải fix — ad-hoc per user direction "SQL insert thẳng")

INSERT 2 row vào kitehub `instances` (status=ACTIVE, tier=FREE, UUID khớp BrandingDataSeeder `a1100000`/`b1100000`) → `by-subdomain` resolve 200 cho cả 2 (verified). Đây là dev-DB manual seed, KHÔNG durable (mất khi reset DB), KHÔNG vào source control → vẫn cần seeder proper (gap này track).

## Root Cause

Kiến trúc DB-per-tenant + lifecycle tách (kitehub quản instance lifecycle/domain, kiteclass quản nghiệp vụ) nhưng dev-seed chỉ có ở kiteclass-side (`BrandingDataSeeder`). Không có dev seeder kitehub-subscription tạo demo-trio `instances` rows tương ứng → by-subdomain (kitehub) không bao giờ resolve cho demo data dù kiteclass branding đã seed.

## Proposed Fix

1. **Kitehub-side dev seeder** (`@Profile("dev")` trong kitehub-subscription) tạo demo-trio `instances` rows idempotent (subdomain + status=ACTIVE + UUID **thống nhất** với BrandingDataSeeder `a1100000`/`b1100000`). Hoặc 1 seed SQL canonical chạy cùng `up.sh`.
2. **Reconcile UUID scheme** — chốt 1 nguồn UUID cho demo-trio; sửa `seed-landing-content.sql` (`ad0fa96e`/`0abe093c`) HOẶC BrandingDataSeeder (`a1100000`/`b1100000`) để khớp. Cân nhắc gộp seed kitehub-instances + kiteclass-branding + landing-content thành 1 flow idempotent.
3. **Sửa recipe §2.2** — ghi đúng: by-subdomain resolve cần kitehub `instances` row (kitehub-side seeder), không chỉ BrandingDataSeeder kiteclass.

## Acceptance Criteria

- [ ] Dev seeder (kitehub-side OR canonical SQL) tạo demo-trio `instances` rows idempotent, chạy tự động với `up.sh`/dev profile — fresh DB reset → `by-subdomain/co-ha-toan` resolve 200 không cần manual INSERT.
- [ ] UUID demo-trio thống nhất 1 scheme giữa BrandingDataSeeder + seed-landing-content.sql (branding/landing link đúng).
- [ ] Recipe `2026-06-11-g2-recipe-landing-100-subdomain.md` §2.2 sửa cho đúng nguồn seed (kitehub instances + kiteclass branding).

## Related

- Discovered in: seed-state verify cho landing-100 G2★ walk 2026-06-11
- [[GAP-811]] + [[GAP-1077]] — host→tenant middleware (subdomain landing); demo-trio là test data cho flow này
- [[GAP-1171]] — gateway CORS wildcard (sister, cùng landing-100 G2★ unblock)
- [[GAP-1114]] — multi-session gap-ID coordination (gap này reserve block 1180-1189)
- Code: `kiteclass-core .../dev/seeder/BrandingDataSeeder.java` (HA/NHI UUID + comment dòng 58) + `kitehub-subscription .../api/controller/PublicTenantController.java` (findBySubdomainAndDeletedFalse) + `kitehub/scripts/seed-landing-content.sql` (ad0fa96e/0abe093c UUID)
- Rule: `design-first-investigation-order` (design→code truy root cause), `discovery-to-gap-inline-filing` §1 (file inline)
