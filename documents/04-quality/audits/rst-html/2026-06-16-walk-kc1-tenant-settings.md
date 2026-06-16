---
title: G1 Browser-Walk — KC-1 Tenant settings (branding + preferences)
audience: dev
created: 2026-06-16
scope: Flow Verification Campaign KC-1 — headless browser walk qua FE :3000 nip.io subdomain (production-accurate Host resolution per g1-browser-walk-before-flip.md §3.1)
walker: headless Playwright (chromium) agent
tenant: sky-education (instance e8ff87e1-69fc-4842-a263-7385c68b4ffb, ACTIVE/PREMIUM)
persona: Owner (owner@skyedu.vn, KH SSO fallback, tenantId claim e8ff87e1)
references:
  - documents/04-quality/audits/persona-review/2026-06-16-pre-walk-kc1-tenant-provisioning.md
  - documents/05-guides/operations/2026-06-05-g2-recipe-kc1-tenant-settings.md
  - .claude/rules/g1-browser-walk-before-flip.md
---

# G1 Browser-Walk — KC-1 Tenant settings

**Verdict tổng: ✅ PASS** — flow functional 100% qua browser thật. **0 product bug browser-level.** 2 catalog item = recipe doc-staleness (KHÔNG phải product bug).

**Cơ chế walk (production-accurate per g1-browser-walk-before-flip §3.1/§3.2):**
- URL: `http://sky-education.127.0.0.1.nip.io:3000` (subdomain Host thật, nip.io wildcard → 127.0.0.1; CẤM `localhost:3000` thuần / `?tenant=`).
- Login: FE auto-fallback KH SSO — `POST /api/v1/tenant-auth/login` → **401 by-design** (owner ngoài KC users), rồi `POST /api/auth/login` → **200 + JWT** (role OWNER, tenantId claim, tier PREMIUM). FE `loginBaseUrl()` (auth.ts:33) preserve tenant Host trên subdomain → gateway resolve tenant đúng.

---

## Bảng kết quả walk

| Bước | Status | Evidence | Bug |
|---|:---:|---|---|
| 1. Mở `/login` qua nip.io subdomain | ✅ PASS | HTTP 200; login form render + fillable; `branding/public?tenantId=sky-education` → 200 | — |
| 2. Đăng nhập Owner (KH SSO fallback) | ✅ PASS | `tenant-auth/login` 401 (by-design) → `/api/auth/login` 200 → redirect `/dashboard`; KHÔNG circuit-breaker "Dịch vụ tạm ngưng" | — |
| 3. Dashboard widget API (dwell 6s) | ✅ PASS | `/students /courses /teachers /classes /invoices` qua gateway **ALL 200** (recipe GAP-1069 404 KHÔNG reproduce ở state hiện tại) | — |
| 4. Mở `/settings` | ✅ PASS | url=`/settings`, h1="Cài đặt"; KHÔNG redirect `/login`; `GET /api/v1/settings/branding` → 200 | — |
| 5. Tabs Settings | ✅ PASS | Branding=1, Theme preview=1, Tùy chọn=**0** | — |
| 6. Tab "Tùy chọn" ẩn cho OWNER (GAP-979) | ✅ PASS | tabPrefs count=0 — fix GAP-979 hoạt động đúng | — |
| 7. Branding render (form values) | ✅ PASS | displayName="Trung tâm cô Đỗ Lan Khánh", tagline, primaryColor=#e8590c, secondary=#1b4965, accent=#ffb703 (populate vào form input đúng) | — |
| 8. Version-history section | ✅ PASS | `GET /api/v1/branding/{uuid}/versions?page=0&size=20` → **200**; section render keywords "Phiên bản / Lịch sử / Khôi phục" (pre-walk #5 dự đoán 403 → **REFUTED**) | — |
| 9. Tab Theme preview render | ✅ PASS | `[data-testid="settings-theme-preview"]` count=1 | — |
| 10. Logo asset load | ✅ PASS | logo `/demo-banners/co-khanh-phapluat-logo.webp` FE-served 200, KHÔNG broken (pre-walk #6 MinIO :9100 → **REFUTED**) | — |
| 11. PUT branding happy path | ✅ PASS | sửa displayName +" ✦" → `PUT /api/v1/settings/branding` **200** + toast success | — |
| 12. Reload persist | ✅ PASS | F5 → displayName giữ " ✦" (persist DB OK) | — |
| 13. Revert | ✅ PASS | revert → PUT 200; DB confirm `display_name="Trung tâm cô Đỗ Lan Khánh"`, `primary_color=#E8590C` (0 data drift) | — |
| 14. Sad path — màu invalid | ✅ PASS (N/A inject) | `#primaryColor` = native `<input type="color">` → reject hex sai client-side (snap #000000). Server 400 VALIDATION đã curl-verify ở pre-walk nhưng UI ngăn input sai tại client | — |

---

## Verify 8 pre-walk failure modes

| # | Pre-walk failure mode | Browser-walk verdict |
|---|---|---|
| 1 | Owner credential path confusion (chỉ KH SSO fallback) | **CONFIRMED (handled)** — FE auto-fallback đúng, login PASS. KHÔNG phải product bug; recipe cần clarify (doc) |
| 2 | Seed drift displayName ≠ "Sky Education" | **CONFIRMED (recipe stale)** — DB+UI = "Trung tâm cô Đỗ Lan Khánh"; recipe Bước 3 ghi "Sky Education" → recipe doc-fix |
| 3 | localhost vs nip.io (process gap) | **RESOLVED** — walk dùng nip.io subdomain production-accurate ✓ |
| 4 | Post-rebuild stale docker-proxy :3000 (GAP-1067) | **NOT FIRED** — FE up 3h healthy, :3000 → 200 |
| 5 | BrandingVersionHistory 403 | **REFUTED** — versions endpoint 200 + section render OK |
| 6 | MinIO presigned logo :9100 broken | **REFUTED** — logo FE-served 200, no broken asset |
| 7 | tenant-auth 401 → circuit breaker "Dịch vụ tạm ngưng" | **NOT FIRED** — login sạch, breaker không trip |
| 8 | Preferences tab hidden cho OWNER (GAP-979) | **CONFIRMED working** — tab "Tùy chọn" count=0 |

---

## Browser-level bug catalog

**Product bug: 0.** Các quan sát:

1. **Recipe doc-staleness (2 item, KHÔNG phải product bug — coordinator fix recipe G2):**
   - Credential path: recipe nên ghi rõ owner = KH SSO fallback, tenant-auth 401 by-design.
   - Expected displayName: recipe Bước 3 sửa "Sky Education" → "Trung tâm cô Đỗ Lan Khánh" (hoặc re-seed).

2. **Network noise (KHÔNG phải bug):** nhiều `net::ERR_ABORTED` trên `/_next/static/chunks/*` + prefetch `?_rsc=*` = SPA navigation cancel in-flight prefetch khi điều hướng nhanh — bình thường, không ảnh hưởng flow (dwell-test xác nhận mọi API call thật đều 200).

3. **internal inconsistency (cosmetic, pre-walk #2 noted):** `themeConfigJson` còn embed tên cũ "Trung tâm Anh ngữ Sky Education" trong khi `display_name` column = "Trung tâm cô Đỗ Lan Khánh". Không ảnh hưởng UI render (UI đọc `displayName` column). Có thể catalog gap cosmetic nếu muốn.

**Console errors browser-level:** 0 uncaught error trên happy path (chỉ ERR_ABORTED network noise như trên).

---

## Walk artifacts

- Screenshots: `/tmp/kc1-step-{1..6}*.png`, `/tmp/kc1b-settings-branding.png`, `/tmp/kc1c-after-save.png`
- Walk scripts: `/tmp/walk-kc1.mjs` (full), `/tmp/walk-kc1b.mjs` (dwell + form values), `/tmp/walk-kc1c.mjs` (PUT happy/persist/revert/sad)
- DB integrity post-walk: `branding.display_name="Trung tâm cô Đỗ Lan Khánh"`, `primary_color=#E8590C` (restored, 0 drift)
