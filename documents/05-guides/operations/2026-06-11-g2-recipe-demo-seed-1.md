---
audience: dev
title: G2★ recipe — demo-seed-1 walk (2 tenant Cô Hà + Thầy Nhì, full academic + landing sections)
created: 2026-06-11
flow: demo-seed-1 (by-subdomain resolve + landing sections/assets + academic data)
gates: G2★ (human production-accurate browser walk) — verify GAP-1180/1190..1195
---

# G2★ Recipe — Demo-Seed-1 Walk (Cô Hà FREE + Thầy Nhì PAID)

> **Mục tiêu:** Con người walk wave `demo-seed-1` end-to-end qua **subdomain Host thật (nip.io)**, KHÔNG `?tenant=` (per `g1-browser-walk-before-flip.md` §3.1). Verify: by-subdomain resolve (GAP-1180) + landing sections/assets thật (GAP-1194/1195) + academic data (GAP-1190..1193). Walk PASS → flip 6 gap DONE + đóng wave demo-seed-1.
>
> Sister recipe: `2026-06-11-g2-recipe-landing-100-subdomain.md` (landing render base). Recipe này thêm: landing **sections** (teachers/pricing/stats) + **assets webp** + **dashboard academic data**.

---

## 1. Setup stack (production-equivalent)

```bash
cd /home/nguyenvankiet/projects/2026-Kite-Class-Platform
git checkout main && git pull --ff-only origin main   # phải có PR #2319 (Bucket E) đã merge
bash kitehub/scripts/up.sh        # Postgres+Flyway RLS thật + gateway :9000 + kiteclass-core + FE :3000
bash kitehub/scripts/status.sh    # services Up + healthy
```

Chờ 3 dev seeder chạy (boot log), verify:
```bash
bash kitehub/scripts/logs.sh kiteclass-core 2>&1 | grep -iE "DemoTrio|BrandingData|DemoAcademic|Seeded"
```
- `DemoTrioInstanceSeeder` (kitehub `instances` → by-subdomain) + `BrandingDataSeeder` (branding/landing/sections) + `DemoAcademicSeeder` (academic core).

Yêu cầu production-parity (per `local-fix-production-parity-check.md`): Postgres+Flyway RLS thật (KHÔNG H2), gateway `:9000` route public-tenant-resolve permitAll, prod-profile config.

---

## 2. Bước 0 — by-subdomain resolve (gateway, production path) — GAP-1180

```bash
curl -s http://localhost:9000/api/v1/public/tenants/by-subdomain/co-ha-toan  | jq .
curl -s http://localhost:9000/api/v1/public/tenants/by-subdomain/thay-nhi-hoa | jq .
```

| | Expected |
|---|---|
| **Action** | curl 2 slug qua gateway |
| **Expected** | HTTP 200 + `tenantId` = `a1100000-0000-4000-a000-000000000001` (Hà) / `b1100000-0000-4000-a000-000000000002` (Nhì). Fresh DB, KHÔNG manual INSERT. |
| **Sad path** | 404 TENANT_NOT_FOUND → DemoTrioInstanceSeeder chưa chạy (dev profile? log?). |
| **Verify** | UUID khớp canonical scheme (a1100000/b1100000) |

---

## 3. Bước 1 — Landing Cô Hà (browser subdomain) — GAP-1194/1195

**Mở browser:** `http://co-ha-toan.127.0.0.1.nip.io:3000`

| Verify | Expected |
|---|---|
| Theme | Xanh dương `#2563EB` |
| Hero | Ảnh **webp** render OK — DevTools Network `co-ha-toan.webp` 200 (không 404 PNG cũ) |
| Logo | Logo **nhỏ riêng** (≠ hero banner) — `co-ha-toan-logo.webp` |
| Section **Đội ngũ GV** | "Nguyễn Thị Hà — Toán tiểu học" (KHÔNG empty-state) |
| Section **Bảng giá** | 1 gói "Miễn phí" |
| Section **Chỉ số** | 2 Lớp / 12 Học viên / 85% chuyên cần |

- **Sad path:** section empty-state → BrandingDataSeeder sections chưa seed HOẶC PERSONAL_TEMPLATE chưa enable teachers (GAP-1194). Ảnh 404 → webp path sai.

---

## 4. Bước 2 — Landing Thầy Nhì

**Mở:** `http://thay-nhi-hoa.127.0.0.1.nip.io:3000`

| Verify | Expected |
|---|---|
| Theme | Xanh lá `#16A34A` |
| Hero/logo | `thay-nhi-hoa.webp` + logo riêng, không 404 |
| Đội ngũ GV | "Nguyễn Đình Nhì — Hóa học THCS" |
| Bảng giá | **3 gói** `1.200.000đ / 1.500.000đ / 1.800.000đ` |
| Chỉ số | 4 Lớp / 35 Học viên / 94% chuyên cần |

---

## 5. Bước 3 — Academic data (psql DB-assert) — GAP-1190..1193

> Browser dashboard walk cần login — demo tenant chưa seed login account (xem §7 + GAP-1197). Tạm verify academic core qua DB:

```bash
docker exec kite-postgres psql -U kite -d kiteclass_shared -c \
"SET app.current_tenant='a1100000-0000-4000-a000-000000000001';
 SELECT (SELECT count(*) FROM classes) cls, (SELECT count(*) FROM students) hs,
        (SELECT count(*) FROM attendances) att, (SELECT count(*) FROM grades) gr,
        (SELECT count(*) FROM invoices) inv;"
```

| | Expected |
|---|---|
| **Hà** (`a1100000…0001`) | cls=2, hs≈12, att>0, gr>0, inv>0 (100% paid) |
| **Nhì** (`b1100000…0002`) | cls=4, hs≈35, att>0, gr>0, inv>0 (còn công nợ — payment <100%) |

> ⚠️ Tên bảng/cột (`classes`/`students`/`attendances`/`grades`/`invoices` + GUC `app.current_tenant`) là khung — xác nhận chính xác theo schema thật khi walk; điều chỉnh tên nếu khác.

---

## 6. Báo kết quả (4 outcome)

- ✅ **PASS** tất cả → flip GAP-1180/1190..1195 DONE + đóng wave demo-seed-1 (Scope-Completeness reconciliation per `wave-closure-scope-completeness`).
- ⚠️ **PARTIAL** (landing PASS, dashboard chưa browser-walk do login) → flip landing-related (1180/1194/1195) DONE; academic (1190..1193) giữ PARTIAL tới khi login seeded (GAP-1197) → browser dashboard walk.
- 🔴 **FAIL bước cụ thể** → báo bước + screenshot/log → catalog batch-fix (per `feature-ship-runtime-walk-mandate.md` §3.4).
- 🚧 **Blocked** (stack không lên) → §7.

---

## 7. Troubleshooting + open item

- **Redis stale landing** (sections null sau seed): `docker exec kite-redis redis-cli --scan --pattern "landingPages*" | xargs -r -I{} docker exec kite-redis redis-cli DEL "{}"`
- **Stale FE proxy `:3000` ERR_EMPTY_RESPONSE** (GAP-1067 class): restart FE container.
- **404 by-subdomain:** verify dev profile active + DemoTrioInstanceSeeder log.
- 🔴 **Open item — dashboard login (GAP-1197):** demo tenant (Hà/Nhì) có academic data nhưng **chưa seed login account** → browser dashboard walk chưa làm được. Quyết: (a) seed dev login demo teacher → full browser dashboard walk; HOẶC (b) academic verify qua psql DB-assert (G1) đủ cho PARTIAL.

### G3 preview
G3-infra (TLS/LB/wildcard-cert/real-DNS production subdomain) = AWS-gated GAP-612, KHÔNG block walk này (G2★ gộp G2+G3-functional per campaign 2026-06-11).
