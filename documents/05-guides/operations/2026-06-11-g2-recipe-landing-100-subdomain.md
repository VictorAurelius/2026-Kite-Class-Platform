---
audience: dev
title: G2★ recipe — landing-100 per-tenant subdomain walk (nip.io production-accurate)
created: 2026-06-11
flow: landing-100 (per-tenant landing render qua Host→tenant middleware)
gates: G2★ (human production-accurate browser walk) — verify GAP-811 + GAP-1077
---

# G2★ Recipe — Landing-100 Subdomain Walk (nip.io)

> **Mục tiêu:** Con người walk landing page per-tenant qua **subdomain Host THẬT** (nip.io) trên browser — đúng access-mode production (`{slug}.kiteclass.com`), KHÔNG dùng `?tenant=` (= bằng chứng giả per `g1-browser-walk-before-flip.md` §3.1+§3.2). Walk PASS → flip **GAP-811 + GAP-1077** DONE + đóng nốt landing-100 G2★.
>
> **Đây là G2★ (gộp G2+G3-functional, chốt 2026-06-11):** 1 walk chứng minh CẢ trải nghiệm UX thật LẪN functional production-parity (subdomain→gateway→FE→core, RLS thật, prod access-mode). Phần **G3-infra** (TLS/LB/wildcard-cert/real-DNS) = AWS-gated GAP-612, KHÔNG block walk này.

---

## 1. Vì sao nip.io, không `?tenant=`

Production: user gõ `co-ha-toan.kiteclass.com` → FE `middleware.ts` đọc **Host header** → `extractSlugFromHost()` lấy `parts[0]` = `co-ha-toan` → resolve ra tenantId → render landing Cô Hà Toán.

`?tenant=<uuid>` đi **nhánh dev-preview** (`extractSlug()` ưu tiên query TRƯỚC Host) → **BYPASS** chính `extractSlugFromHost()` cần verify. Pass `?tenant=` KHÔNG chứng minh production hoạt động.

**nip.io** = wildcard DNS công cộng resolve về `127.0.0.1`, no sudo: `co-ha-toan.127.0.0.1.nip.io` → 127.0.0.1, nhưng **Host header thật** chứa subdomain `co-ha-toan` → middleware parse `parts[0]` = `co-ha-toan` (host có ≥3 parts, thỏa `extractSlugFromHost`). Tái hiện 100% resolution logic production.

---

## 2. Setup (production-equivalent stack)

### 2.1 Khởi động stack (Postgres + Flyway RLS THẬT, gateway, core, FE)

```bash
# Stack KiteClass: kite-postgres + kite-gateway(:9000) + kiteclass-core + kiteclass-frontend(:3000)
bash kitehub/scripts/up.sh              # hoặc compose tương ứng KiteClass core+gateway+FE
bash kitehub/scripts/status.sh          # verify services Up + healthy
```

Yêu cầu (G2★ production-parity per `local-fix-production-parity-check.md`):
- ✅ Postgres + Flyway RLS thật (KHÔNG H2)
- ✅ gateway `:9000` route `public-tenant-resolve` (`/api/v1/public/tenants/by-subdomain/{slug}`) permitAll
- ✅ prod-profile config + env-var đủ

### 2.2 Seed demo-trio (slug có sẵn từ landing-100 Bucket G)

`by-subdomain/{slug}` resolve đọc bảng **kitehub `instances`** (DB `kitehub`) qua
`PublicTenantController` (kitehub-subscription). Demo-trio cần **2 seeder cross-service**
cùng chạy ở `dev` profile (UUID khớp nhau, shared-DB + RLS per ADR-023):

| Seeder | Service | DB | Tạo gì | UUID demo-trio |
|---|---|---|---|---|
| `DemoTrioInstanceSeeder` (GAP-1180) | kitehub-subscription | `kitehub` | `instances` row (status=ACTIVE) → **by-subdomain resolve** | `a1100000…0001` / `b1100000…0002` |
| `BrandingDataSeeder` | kiteclass-core | `kiteclass_shared` | `FrontendInstance` + `Branding` + `LandingPage` → branding/landing | cùng UUID trên |

| Slug | Tên | Tier | Màu theme | Template |
|---|---|---|---|---|
| `co-ha-toan` | Cô Hà Toán | FREE | `#2563EB` (xanh dương) | personal |
| `thay-nhi-hoa` | Thầy Nhị Hóa | PREMIUM | `#16A34A` (xanh lá) | personal |

Verify slug resolve qua gateway (production path):
```bash
# phải trả 200 + tenantId UUID (đọc kitehub instances, KHÔNG phải kiteclass landing)
curl -s http://localhost:9000/api/v1/public/tenants/by-subdomain/co-ha-toan | head -c 300
# kỳ vọng: JSON chứa tenantId UUID a1100000-0000-4000-a000-000000000001 (KHÔNG 404)
```
Nếu 404 → **`DemoTrioInstanceSeeder` (kitehub-subscription dev profile) chưa chạy** (KHÔNG
phải BrandingDataSeeder — seeder kiteclass chỉ seed branding/landing, không tạo kitehub
`instances` row mà resolve đọc). Rebuild + restart kitehub-subscription với `dev` profile.

### 2.3 FE đọc đúng base URL resolve

Middleware `resolveTenant.ts` gọi gateway qua `INTERNAL_API_URL` / `NEXT_PUBLIC_API_URL` ?? `http://kite-gateway:9000`:
- **FE trong Docker:** `INTERNAL_API_URL=http://kite-gateway:9000` (mặc định, OK).
- **FE chạy `pnpm dev` local:** set `NEXT_PUBLIC_API_URL=http://localhost:9000` (vì `kite-gateway` không resolve ngoài Docker network).

```bash
# Nếu walk bằng pnpm dev local:
NEXT_PUBLIC_API_URL=http://localhost:9000 pnpm --filter kiteclass-frontend dev
```

---

## 3. Walk — từng bước (browser thật, KH=:3001 / KC landing=:3000)

> Mở **Chrome DevTools → Network + Console** trước khi bắt đầu. Quan sát: (a) Request Host header có subdomain thật, (b) middleware resolve UUID, (c) landing render đúng branding.

### Bước 1 — Tenant A: Cô Hà Toán (happy path)

| | |
|---|---|
| **Action** | Mở browser → `http://co-ha-toan.127.0.0.1.nip.io:3000/` |
| **Expected** | Landing render branding **Cô Hà Toán**: tên trung tâm "Cô Hà Toán", theme màu **xanh dương `#2563EB`** (header/CTA), hero + sections của tenant này. KHÔNG phải fallback `11111111-...`. |
| **Verify (a) Host** | DevTools → Network → request `/` → Request Headers → `Host: co-ha-toan.127.0.0.1.nip.io:3000` (subdomain thật, KHÔNG `?tenant=`) |
| **Verify (b) resolve** | Network có call `…/api/v1/public/tenants/by-subdomain/co-ha-toan` → 200 + tenantId UUID; landing fetch dùng đúng UUID đó |
| **Verify (c) Console** | Console clean — KHÔNG error đỏ, KHÔNG ERR_EMPTY_RESPONSE |

### Bước 2 — Tenant B: Thầy Nhị Hóa (isolation — branding KHÁC tenant A)

| | |
|---|---|
| **Action** | Mở tab mới → `http://thay-nhi-hoa.127.0.0.1.nip.io:3000/` |
| **Expected** | Landing render branding **Thầy Nhị Hóa**: theme màu **xanh lá `#16A34A`**, tên + sections KHÁC hẳn tenant A. Chứng minh cùng 1 codebase render khác nhau theo Host. |
| **Verify** | Network: resolve `by-subdomain/thay-nhi-hoa` → UUID khác Bước 1; theme/tên đúng tenant B (không leak branding tenant A) |

### Bước 3 — Sad path: subdomain không tồn tại

| | |
|---|---|
| **Action** | Mở `http://khong-ton-tai.127.0.0.1.nip.io:3000/` |
| **Expected** | Resolve `by-subdomain/khong-ton-tai` → 404 → middleware **pass-through graceful** → landing render generic/fallback HOẶC trang 404 thân thiện. **KHÔNG crash, KHÔNG 500.** |
| **Verify** | Console clean; Network resolve = 404 nhưng page vẫn render (degrade gracefully per GAP-811 AC) |

### Bước 4 — Sad path: reserved subdomain

| | |
|---|---|
| **Action** | Mở `http://www.127.0.0.1.nip.io:3000/` |
| **Expected** | `www` ∈ RESERVED_SUBDOMAINS → middleware pass-through (apex-marketing, no tenant context) → KHÔNG resolve tenant, render generic. |
| **Verify** | Network: KHÔNG có call `by-subdomain/www`; page render generic |

### Bước 5 — Empty-state (anti-fab, no fake data) — nếu có tenant rỗng

| | |
|---|---|
| **Action** | Browse subdomain của 1 tenant đã provision nhưng **chưa cấu hình landing data** (nếu seed có) |
| **Expected** | Landing hiển thị **empty-state thật** (per landing-100 Bucket A — KHÔNG bịa data giả): placeholder/CTA cấu hình, không section rỗng vỡ layout |

### Bước 6 — Mobile + theme contrast (UX thật)

| | |
|---|---|
| **Action** | DevTools → Toggle device toolbar (mobile viewport) trên Bước 1 |
| **Expected** | Hero/section reflow đúng mobile (chữ Việt crisp, không tràn); text/CTA contrast đạt WCAG AA (theme clamp lightness per Bucket D) |

---

## 4. Sad-path checklist (tổng hợp — phải PASS hết)

- [ ] Subdomain không tồn tại → graceful fallback (Bước 3), không crash/500
- [ ] Reserved subdomain (`www`) → pass-through, không resolve (Bước 4)
- [ ] BE gateway down giữa chừng → landing degrade về fallback branding, không crash (per GAP-811 AC graceful)
- [ ] Tenant A ≠ Tenant B branding (isolation, Bước 2) — không leak

---

## 5. Báo kết quả (4 outcome)

Sau khi walk, báo theo 1 trong 4:

| Outcome | Nghĩa | Hành động |
|---|---|---|
| ✅ **PASS hết** | 6 bước + 4 sad-path OK; branding render đúng theo subdomain Host | Coordinator flip **GAP-811 + GAP-1077 → DONE** (AC cuối `[x]`) + đóng landing-100 G2★. |
| ⚠️ **PASS với lỗi nhỏ** | Render đúng nhưng có gap cosmetic ≤30p | Coordinator fix inline per `small-gap-inline-fix.md` → re-walk → DONE |
| 🔴 **FAIL resolution** | Browse subdomain → vẫn fallback `11111111-...` / branding sai / resolve 404 cho slug có thật | Bug middleware/gateway — KHÔNG flip DONE; coordinator điều tra (xem §6 troubleshooting) |
| ⛔ **Không walk được** | Stack không lên / seeder không chạy / nip.io không resolve | Báo blocker cụ thể (service nào, lỗi gì) |

---

## 6. Troubleshooting

| Triệu chứng | Nguyên nhân khả dĩ | Cách xử |
|---|---|---|
| `co-ha-toan.127.0.0.1.nip.io` → DNS không resolve | Mạng chặn nip.io public DNS | Fallback `/etc/hosts` (`!`+sudo): `127.0.0.1 co-ha-toan.kiteclass.local` rồi browse `co-ha-toan.kiteclass.local:3000` |
| Browse subdomain → vẫn branding fallback `11111111-...` | Middleware không gọi resolve được (base URL sai) | Verify §2.3 — FE local cần `NEXT_PUBLIC_API_URL=http://localhost:9000`; FE Docker cần `kite-gateway:9000` reachable |
| resolve `by-subdomain/co-ha-toan` → 404 | `DemoTrioInstanceSeeder` (kitehub-subscription dev) chưa chạy / slug khác | Rebuild+restart kitehub-subscription dev profile; verify §2.2 (KHÔNG phải kiteclass BrandingDataSeeder) |
| resolve → 401/403 | gateway route `public-tenant-resolve` chưa permitAll | Check gateway SecurityConfig (GAP-813 đã ship permitAll — verify còn đúng) |
| Console `ERR_EMPTY_RESPONSE :3000` | docker-proxy stale (GAP-1067 class) | restart FE container / proxy |

---

## 7. G3-infra preview (AWS-gated, KHÔNG block walk này)

Sau khi G2★ PASS local, phần **G3-infra** còn lại (verify trên AWS, gated GAP-612 — stack đang stopped):
- TLS thật `*.kiteclass.com` wildcard cert
- ALB/LB routing subdomain → FE
- Real-DNS Cloudflare `*.kiteclass.com` wildcard
- Custom-domain DNS verify + SSL (GAP-812)

Khoảng cách nip.io ↔ production thật (port/TLS/LB/wildcard-cert) = infra parity = G3-infra territory (per `g1-browser-walk-before-flip.md` §86). nip.io đã exercise đúng 100% **resolution logic** → functional parity DONE; chỉ còn infra layer chờ AWS restore.
