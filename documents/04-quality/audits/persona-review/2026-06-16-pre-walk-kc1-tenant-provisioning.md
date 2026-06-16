---
title: Pre-walk persona simulation — KC-1 Tenant provisioning + settings (branding + preferences)
audience: dev
created: 2026-06-16
scope: Flow Verification Campaign KC-1 G2 — Owner mở /settings KiteClass → branding + preferences. Predict failure modes TRƯỚC browser walk trên local Docker stack.
mode: PREDICT-ONLY
persona: Owner (sky-education tenant) đăng nhập lần đầu → mở /settings → xem/sửa branding → reload xác nhận persist
references:
  - documents/03-planning/waves/wave-2026-06-04-flow-kc1-tenant-provisioning-settings.md
  - documents/05-guides/operations/2026-06-05-g2-recipe-kc1-tenant-settings.md
  - .claude/rules/pre-walk-persona-simulation-mandate.md
  - .claude/rules/g1-browser-walk-before-flip.md
---

# Pre-walk persona simulation — KC-1 Tenant settings

**Stack tại thời điểm sim (2026-06-16):** kite-gateway :9000 healthy, kiteclass-frontend :3000 healthy (up 3h), kiteclass-core :8088 healthy, kite-postgres :5433 healthy. FE :3000/login → 200; gateway → 200.

**Tenant test (re-verified):** `sky-education`, instance `e8ff87e1-69fc-4842-a263-7385c68b4ffb`, status **ACTIVE / PREMIUM**, owner_id `3c659096` = `owner@skyedu.vn`.

**Kết luận tổng:** Core branding flow (GET / PUT / version-history) **đều PASS với JWT thật** (đã curl-verify). NHƯNG **recipe G2 đang stale ở 3 điểm sẽ khiến walker false-fail hoặc kẹt ngay Bước 1**: (1) credential path confusion, (2) displayName seed-drift, (3) localhost vs nip.io. Đây là pre-walk fixes cần làm TRƯỚC khi đưa user walk.

---

## Failure modes (8)

| # | Failure mode | (a) Where | (b) Symptom khi walk | (c) Pre-walk check (đã chạy) | Confidence | Proposed fix |
|---|---|---|---|---|:---:|---|
| 1 | **Owner credential path confusion — tenant-auth probe FAILS, chỉ KH SSO fallback work** | `kiteclass-frontend/src/lib/api/auth.ts:80-99` (probe tenant-auth → fallback KH); `users` table `owner@skyedu.vn` có `tenant_id = NULL` | Login `owner@skyedu.vn/SkyEdu@2026`: tenant-auth/login → **401 INVALID_CREDENTIALS** (owner không nằm trong KC users tenant); FE phải fallback KH `/api/auth/login` (work). Nếu walker curl tay tenant-auth hoặc fallback FE lỗi → báo "credential sai" FALSE-BLOCKING | `curl /api/v1/tenant-auth/login` → **401**; `curl /api/auth/login` → **200 + JWT** (role OWNER, tenantId claim) | **HIGH** | Cập nhật recipe Bước 1: nêu rõ owner login đi qua **KH SSO fallback** (`/api/auth/login`), tenant-auth 401 là **by-design** không phải lỗi. Browser-verify FE auto-fallback thành công |
| 2 | **Seed drift — branding displayName KHÔNG phải "Sky Education"** | `branding` table (kiteclass_shared) instance `e8ff87e1` `display_name` | Recipe Bước 3 nói verify "Trung tâm Anh ngữ Sky Education"; DB thực = **"Trung tâm cô Đỗ Lan Khánh"** (`primaryColor #E8590C`). Walker thấy mismatch → false-fail | `curl branding` → `displayName: "Trung tâm cô Đỗ Lan Khánh"`. (themeConfigJson nội bộ vẫn embed tên cũ "Sky Education" — internal inconsistency) | **HIGH** | Cập nhật recipe expected value sang "Trung tâm cô Đỗ Lan Khánh" HOẶC re-seed branding về tên recipe |
| 3 | **Recipe dùng `localhost:3000` thay vì nip.io subdomain (vi phạm `g1-browser-walk` §3.1/§3.3 production-accurate)** | Recipe Bước 1-2 URL `http://localhost:3000/...`; `TenantResolverGatewayFilterFactory.java:79-96` JWT tenantId-claim fallback | localhost work qua **JWT claim fallback** (đã verify), NHƯNG **bypass Host-subdomain resolution** — không test cơ chế production. Per meta rule, G2 evidence host-based flow CẤM localhost thuần | nip.io đã verify: `sky-education.127.0.0.1.nip.io:3000/login` → **200**; gateway via Host `sky-education.127.0.0.1.nip.io` branding → **200** | **HIGH** (process gap) | Recipe đổi sang `http://sky-education.127.0.0.1.nip.io:3000/...` cho cả login + /settings (production-accurate, no sudo) |
| 4 | **Post-rebuild stale docker-proxy `:3000` ERR_EMPTY_RESPONSE (GAP-1067 class)** | docker port-forward Windows↔kiteclass-frontend container | Nếu recipe rebuild FE container giữa chừng → mọi truy cập `:3000` timeout/ERR_EMPTY_RESPONSE dù route đúng | Hiện FE up 3h healthy, `:3000/login` → 200 (KHÔNG đang fire). Risk chỉ khi rebuild | **MED** | Recipe rebuild step PHẢI kèm `docker restart kiteclass-frontend` + chờ ~12s (đã có note ở recipe §6, giữ) |
| 5 | **BrandingVersionHistory auto-render trên /settings gọi `/api/v1/branding/{id}/versions`** | `branding-settings.tsx:407` render `<BrandingVersionHistory>`; `branding-version-history.tsx:26-27` instanceId = `useAuthStore.tenantId`; hook `enabled: !!instanceId` | Endpoint security tier KHÁC settings/branding: subdomain-only → **403**. Nếu auth store `tenantId` không set post-login → query disabled (no error); nếu set sai tenant → section 403/404 | `curl versions` subdomain-only → **403**; **với JWT OWNER → 200** (trả content). Query có guard `enabled: !!instanceId` | **MED** | Browser-verify section version-history render OK sau login (tenantId trong auth store khớp instance); nếu 403 → check tenantId propagation |
| 6 | **MinIO presigned logo URL `:9100` trong version snapshot / uploaded logo** | version snapshot `snapshotJson.logoUrl = http://localhost:9100/kite-branding-assets/...`; uploaded logo qua POST /logo | Nếu walker expand version history hoặc upload logo mới → browser load `:9100` presigned URL; broken-image nếu MinIO unreachable từ browser hoặc URL hết hạn | MinIO `:9100/` → **403** (root forbidden = bình thường; presigned path mới 200). Current branding logo = `/demo-banners/co-khanh-phapluat-logo.webp` → FE-served **200** (OK) | **MED** | Browser-verify presigned logo load khi upload/expand version; nếu broken → check MinIO browser-reachability + URL TTL |
| 7 | **tenant-auth probe 401 mỗi lần owner login → trip gateway authCircuitBreaker ("Dịch vụ tạm ngưng")** | Owner login luôn fire 1 tenant-auth 401 TRƯỚC KH fallback; gateway `authCircuitBreaker` | Login lặp nhiều lần (walker retry) → circuit breaker mở → trang "Dịch vụ tạm ngưng" → walker nghĩ flow hỏng | `curl tenant-auth owner` → 401 confirmed mỗi lần. Recipe §1 sad-path đã ghi nhận triệu chứng | **MED** | Recipe note: nếu "Dịch vụ tạm ngưng" → chờ ~30s (đã có ở recipe Bước 1 sad-path, giữ); cân nhắc carve tenant-auth probe khỏi breaker count |
| 8 | **Preferences tab "Tùy chọn" — verify HIDDEN cho OWNER (GAP-979)** | `settings/page.tsx:52` `showPreferences = (user?.userType as string) !== 'OWNER'` | Recipe Bước 2 yêu cầu verify tab "Tùy chọn" KHÔNG hiện. Nếu auth store `userType` post-KH-login KHÔNG resolve thành chuỗi `'OWNER'` (vd normalized khác) → tab hiện sai → click → `/users/{uuid}/preferences` **403** | Code đọc đúng (so sánh string 'OWNER'); cần browser-verify `userType` thực post-login = 'OWNER' | **LOW-MED** | Browser-verify tab "Tùy chọn" ẩn sau login owner; nếu hiện → check userType mapping trong auth store/useAuth normalize |

---

## Recommended pre-walk batch fix (sort confidence × impact)

**FIX TRƯỚC WALK (HIGH — recipe stale, sẽ chặn/false-fail ngay):**
- #1 Credential path: recipe clarify owner = KH SSO fallback, tenant-auth 401 by-design
- #2 displayName: recipe expected value → "Trung tâm cô Đỗ Lan Khánh" (hoặc re-seed)
- #3 nip.io: recipe URL → `sky-education.127.0.0.1.nip.io:3000` (production-accurate per meta)

→ 3 fix này là **doc-only recipe update** (không đụng product code), nên rẻ + nên gom 1 PR recipe-refresh TRƯỚC khi đưa user G2.

**SPOT-CHECK trong browser walk (MED — verify-now):**
- #5 version-history section render OK (đã 200 với JWT, chỉ cần confirm visual)
- #6 logo asset load (current /demo-banners OK; check MinIO khi upload/expand)
- #8 preferences tab ẩn cho OWNER (verify visual)

**MONITOR (MED — chỉ fire có điều kiện):**
- #4 post-rebuild restart FE (recipe note đã có)
- #7 circuit breaker chờ 30s (recipe note đã có)

---

## Endpoint verification log (curl, JWT OWNER thật)

| Endpoint | Header | Result |
|---|---|---|
| `POST /api/auth/login` (KH SSO) | — | **200** + JWT (role OWNER, `tenantId=e8ff87e1`, tier PREMIUM) |
| `POST /api/v1/tenant-auth/login` (KC) | — | **401** INVALID_CREDENTIALS (by-design, owner ngoài KC users) |
| `GET /api/v1/settings/branding` | `X-Instance-Subdomain: sky-education` | **200** + body đầy đủ |
| `GET /api/v1/settings/branding` | JWT only (localhost fallback) | **200** (JWT tenantId-claim fallback) |
| `GET /api/v1/settings/branding` | `X-Tenant-Id` (uuid, no JWT) | **400** (client header stripped per TenantHeaderGuard) |
| `GET /api/v1/settings/branding` | no header | **400** graceful |
| `PUT /api/v1/settings/branding` (bad color "red") | subdomain | **400** VALIDATION |
| `PUT /api/v1/settings/branding` (valid) | JWT OWNER | **200** persist |
| `GET /api/v1/branding/{id}/versions` | subdomain only | **403** |
| `GET /api/v1/branding/{id}/versions` | JWT OWNER | **200** + content |
| FE `/login` | nip.io subdomain Host | **200** |
| FE `/demo-banners/co-khanh-phapluat-logo.webp` | — | **200** |
| MinIO `:9100/` | — | 403 (root forbidden, normal) |

**KHÔNG sửa product code. KHÔNG commit.** (PREDICT-ONLY mode.)
