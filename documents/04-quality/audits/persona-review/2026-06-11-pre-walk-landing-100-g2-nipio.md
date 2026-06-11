---
audience: mixed
date: 2026-06-11
flow: landing-100 G2★ (per-tenant subdomain render qua nip.io)
type: pre-walk-persona-simulation
rule: pre-walk-persona-simulation-mandate.md v1.0.0
walk-host: http://co-ha-toan.127.0.0.1.nip.io:3000/ + http://thay-nhi-hoa.127.0.0.1.nip.io:3000/
reviewer: pre-walk Opus agent (chỉ đọc code/config, KHÔNG chạy docker)
---

# Pre-walk persona simulation — landing-100 G2★ nip.io subdomain render

## Mục tiêu

Simulate human walk G2★ production-accurate (Host nip.io, KHÔNG `?tenant=`) cho landing per-tenant
trên local Docker stack. Trả về failure modes có khả năng chặn walk TRƯỚC khi coordinator/user mở browser.

## Kết luận thiết kế (design-first, đã verify code)

Chuỗi resolve (đã đọc code, KHÔNG suy diễn):

1. Browser → `co-ha-toan.127.0.0.1.nip.io:3000/`
2. `middleware.ts:79 extractSlugFromHost` — split Host, parts = `[co-ha-toan,127,0,0,1,nip,io]` (7 label ≥ 3),
   regex IP `^\d+\.\d+\.\d+\.\d+$` KHÔNG match (có chữ) → trả `slug = "co-ha-toan"`. **nip.io host được handle ĐÚNG** (không phải bug).
3. `resolveTenant.ts:106` fetch `INTERNAL_API_URL (kite-gateway:9000)/api/v1/public/tenants/by-subdomain/co-ha-toan`
   → gateway route `public-tenant-resolve` (`Path=/api/v1/public/tenants/**`, SKIP TenantResolver) → kitehub-subscription `PublicTenantController`.
4. Resolve OK → middleware inject header `x-tenant-id = a1100000-0000-4000-a000-000000000001`.
5. `(public)/page.tsx:29` đọc `x-tenant-id` → `publicApi.getLandingPage(tenantId)` server-side qua
   `INTERNAL_API_URL/api/v1/tenants/{id}/landing` → gateway route `public-tenant-landing` (SKIP TenantResolver) → kiteclass-core `LandingPageController`.
6. `ThemeSync` SSR-inline CSS vars (SSR server component, không FOUC) + `TemplateRenderer`.

**Màu kỳ vọng (đã verify trong BrandingDataSeeder.java):**
- co-ha-toan = `#2563EB` (xanh dương), tenant `a1100000-…-0001`
- thay-nhi-hoa = `#16A34A` (xanh lá), tenant `b1100000-…-0002`
- **SKY fallback = `#E8590C` (CAM)**, tenant `e8ff87e1-69fc-4842-a263-7385c68b4ffb` = `NEXT_PUBLIC_TENANT_ID` default trong compose
- Generic catch fallback (BE landing fetch fail) = `#3B82F6` + title "Trung tâm giáo dục", không hero banner

→ **Tiêu chí quan sát then chốt:** nếu trang hiện CAM "Sky" hoặc xanh `#3B82F6` "Trung tâm giáo dục" → resolve/landing ĐÃ FAIL, KHÔNG phải pass.

## Failure modes (9)

### FM-1 — Docker profile mismatch: `kc-only` KHÔNG khởi động kitehub-subscription [HIGH]
- **WHERE:** `kitehub/docker-compose.kitehub.yml:343` (`kitehub-subscription` profiles `["beta-funnel","full"]`) vs `:810` (`kiteclass-frontend` profiles `["kc-only","full"]`) + `:700` (`kiteclass-core` `["kc-only","full"]`).
- **SYMPTOM:** walk bằng profile `kc-only` (trực giác "chỉ KiteClass") → subscription KHÔNG chạy → by-subdomain `fetch` ECONNREFUSED → `TenantResolveNetworkError` → middleware pass-through (KHÔNG inject `x-tenant-id`) → page.tsx fallback `NEXT_PUBLIC_TENANT_ID = e8ff87e1` (SKY). **CẢ HAI subdomain hiện trang CAM "Sky Education" giống hệt**, không xanh dương / xanh lá.
- **PRE-WALK CHECK:** `bash kitehub/scripts/up.sh` với profile `full` (hoặc set COMPOSE_PROFILES=full). Verify: `docker ps | grep kitehub-subscription` phải Up + healthy.
- **CONFIDENCE: HIGH.** Fix: bắt buộc walk trên profile `full`, KHÔNG `kc-only`.

### FM-2 — Dev-profile seeder chưa chạy / instances row thiếu → resolve 404 [HIGH]
- **WHERE:** `DemoTrioInstanceSeeder.java:55 @Profile("dev")` (kitehub-subscription, seed `instances`) + `BrandingDataSeeder.java:48 @Profile("dev")` (kiteclass-core, seed `landing_pages`). Cả hai best-effort (`catch` nuốt lỗi, line 133).
- **SYMPTOM:** subscription chạy nhưng KHÔNG ở profile `dev` (vd prod), HOẶC seeder nuốt exception → `instances` không có row `co-ha-toan` → `PublicTenantController:100` trả 404 `TENANT_NOT_FOUND` → middleware `resolveTenant` trả null → pass-through → fallback SKY CAM (giống FM-1).
- **PRE-WALK CHECK:** `curl -s http://localhost:9000/api/v1/public/tenants/by-subdomain/co-ha-toan` → kỳ vọng 200 + `"id":"a1100000-0000-4000-a000-000000000001"`. Verify `SPRING_PROFILES_ACTIVE: dev` ở subscription (compose `:345`).
- **CONFIDENCE: HIGH.** Fix: đảm bảo subscription profile `dev` + xem log "Seeded demo-trio instances row".

### FM-3 — NEXT_PUBLIC_TENANT_ID che lỗi resolve (green-but-wrong trap) [HIGH]
- **WHERE:** `(public)/page.tsx:30-33` + `(public)/layout.tsx:32-35` priority `headerTenantId ?? NEXT_PUBLIC_TENANT_ID(e8ff87e1 SKY) ?? hardcoded`.
- **SYMPTOM:** mọi resolve failure (FM-1/FM-2/FM-9) KHÔNG crash — page render BÌNH THƯỜNG nhưng SAI tenant (CAM "Sky"). Walker dễ nhầm "trang chạy OK = PASS" nếu không biết màu kỳ vọng.
- **PRE-WALK CHECK:** ghi nhớ co-ha-toan=XANH DƯƠNG, thay-nhi-hoa=XANH LÁ; nếu CAM → FAIL. So sánh `<style data-theme-sync>` chứa `#2563EB` (HA) / `#16A34A` (NHI).
- **CONFIDENCE: HIGH.** Đây là tiêu chí quan sát quan trọng nhất — fail thầm lặng.

### FM-4 — landing_pages row thiếu (cross-DB) → generic fallback [MED]
- **WHERE:** resolve (instances ở kitehub DB) tách rời landing (`landing_pages` ở kiteclass DB, gateway comment `:777` cho thấy 2 DB khác nhau). `page.tsx:37 catch` → hardcoded `#3B82F6` + "Trung tâm giáo dục".
- **SYMPTOM:** resolve OK (đúng tenant id) nhưng `BrandingDataSeeder` chưa seed landing cho UUID đó → `LandingPageService.getLandingPage` ném → catch → trang xanh `#3B82F6` title "Trung tâm giáo dục", KHÔNG hero banner.
- **PRE-WALK CHECK:** `curl -s http://localhost:9000/api/v1/tenants/a1100000-0000-4000-a000-000000000001/landing` → 200 + `primaryColor:"#2563EB"`.
- **CONFIDENCE: MED.** Fix: verify cả 2 seeder chạy (cross-service UUID parity a1100000…/b1100000…).

### FM-5 — demo-banner asset không vào Next standalone build → hero 404 [MED]
- **WHERE:** `BrandingDataSeeder.java:95 HA_BANNER_URL=/demo-banners/co-ha-toan.webp`; asset tồn tại `kiteclass-frontend/public/demo-banners/` (đã verify). Nhưng `next.config.js:8 output:'standalone'` — standalone CẦN Dockerfile copy `public/` thủ công.
- **SYMPTOM:** nếu Dockerfile không copy `public/demo-banners` vào standalone runner → hero/logo `<img>` 404, hero trống / alt text.
- **PRE-WALK CHECK:** `curl -sI http://co-ha-toan.127.0.0.1.nip.io:3000/demo-banners/co-ha-toan.webp` → 200 (không 404).
- **CONFIDENCE: MED.** Fix: verify Dockerfile `COPY --from=builder /app/public ./public` cho kiteclass-frontend.

### FM-6 — Sad path "tenant không tồn tại" hiện SKY thay vì 404/generic [MED]
- **WHERE:** `middleware.ts:117-121` (null → pass-through không inject) + `page.tsx` fallback SKY.
- **SYMPTOM:** `khong-ton-tai.127.0.0.1.nip.io:3000` resolve 404 → middleware pass-through → page render SKY CAM (không phải trang "không tồn tại"/generic). Design `middleware.ts:18` comment nói "let app render generic 404/fallback" nhưng app rơi vào `NEXT_PUBLIC_TENANT_ID` (SKY), KHÔNG generic.
- **PRE-WALK CHECK:** mở `khong-ton-tai.127.0.0.1.nip.io:3000` — quan sát trang gì. Nếu SKY → ghi nhận drift design-vs-impl (gap candidate).
- **CONFIDENCE: MED.** Có thể là gap thật (unknown tenant nên hiện generic, không hiện 1 tenant cụ thể).

### FM-7 — Sad path SUSPENDED không có fixture để walk [MED]
- **WHERE:** `PublicTenantController:128-150` trả 410 khi status≠ACTIVE → `resolveTenant:140` ném `TenantSuspendedError` → `middleware:132` redirect `/suspended`. Nhưng `DemoTrioInstanceSeeder` seed cả 2 trio = `ACTIVE` (line 121), KHÔNG có tenant SUSPENDED.
- **SYMPTOM:** không thể walk path 410/`/suspended` nếu không flip 1 instance sang SUSPENDED thủ công.
- **PRE-WALK CHECK:** `UPDATE instances SET status='SUSPENDED' WHERE subdomain='thay-nhi-hoa'` (rồi revert) để test, HOẶC skip sad-path này + ghi chú.
- **CONFIDENCE: MED.**

### FM-8 — Stack phải chạy TRONG docker network (INTERNAL_API_URL=kite-gateway:9000) [MED]
- **WHERE:** `resolveTenant.ts:111` + `public.ts:21` server-side base = `INTERNAL_API_URL (kite-gateway:9000)`.
- **SYMPTOM:** nếu chạy `pnpm dev` trên HOST (ngoài docker) → DNS `kite-gateway` không resolve → network error → fallback SKY (FM-3). Middleware SSR chỉ resolve đúng khi FE container ở chung docker network với gateway.
- **PRE-WALK CHECK:** walk bằng image Docker (`kiteclass-frontend:latest` trong compose), KHÔNG `pnpm dev` host. Hoặc nếu host-run, set `INTERNAL_API_URL=http://localhost:9000`.
- **CONFIDENCE: MED.**

### FM-9 — CORS preflight cho origin nip.io (browser-side fetch catalog/contact) [LOW]
- **WHERE:** gateway `application.yml:21 allowedOriginPatterns` ĐÃ gồm `http://*.127.0.0.1.nip.io:3000` (đã verify) + `next.config.js:88 connect-src` (Report-Only, không block).
- **SYMPTOM:** landing chính SSR-fetch (server-side, không CORS). Nhưng nếu trang có client-side fetch (catalog `/api/v1/courses`, contact submit) từ origin `co-ha-toan.127.0.0.1.nip.io:3000` → cần CORS allow origin nip.io. Đã cấu hình → LOW risk; chỉ fail nếu BASE_DOMAIN/CORS env bị override.
- **PRE-WALK CHECK:** DevTools Network — không có CORS error đỏ trên nip.io origin.
- **CONFIDENCE: LOW.**

## Bảng tóm tắt (theo confidence)

| FM | Mô tả | WHERE | Confidence |
|----|-------|-------|:---:|
| FM-1 | Profile `kc-only` không start subscription → resolve fail → SKY cam | compose `:343` / `:810` | **HIGH** |
| FM-2 | Seeder dev chưa chạy / instances thiếu → resolve 404 → SKY cam | DemoTrioInstanceSeeder `:55` | **HIGH** |
| FM-3 | NEXT_PUBLIC_TENANT_ID che lỗi (green-but-wrong) | page.tsx `:30` / layout `:32` | **HIGH** |
| FM-4 | landing_pages row thiếu → generic `#3B82F6` | LandingPageController `:55` | MED |
| FM-5 | demo-banner không vào standalone → hero 404 | next.config `output:standalone` | MED |
| FM-6 | unknown tenant hiện SKY thay vì generic (design drift) | middleware `:117` | MED |
| FM-7 | không có SUSPENDED fixture để walk 410 | seeder `ACTIVE` only | MED |
| FM-8 | phải chạy trong docker network (INTERNAL_API_URL) | resolveTenant `:111` | MED |
| FM-9 | CORS preflight origin nip.io (client fetch) | gateway `:21` (đã allow) | LOW |

## Walk steps recommended (thứ tự tối ưu)

**Bước 0 — Pre-walk (BẮT BUỘC, trước khi mở browser):**
1. `bash kitehub/scripts/up.sh` profile **`full`** (FM-1). Đợi subscription + kiteclass-core + frontend healthy.
2. `curl -s http://localhost:9000/api/v1/public/tenants/by-subdomain/co-ha-toan` → 200 + id `a1100000…0001` (FM-2).
3. `curl -s http://localhost:9000/api/v1/public/tenants/by-subdomain/thay-nhi-hoa` → 200 + id `b1100000…0002`.
4. `curl -s http://localhost:9000/api/v1/tenants/a1100000-0000-4000-a000-000000000001/landing` → 200 + `#2563EB` (FM-4).
5. `curl -sI http://co-ha-toan.127.0.0.1.nip.io:3000/demo-banners/co-ha-toan.webp` → 200 (FM-5).

**Bước 1 — Happy path co-ha-toan:** mở `http://co-ha-toan.127.0.0.1.nip.io:3000/` →
quan sát màu chủ đạo phải **XANH DƯƠNG #2563EB** + hero banner + tên "Lớp Toán cô Nguyễn Thị Hà".
NẾU thấy CAM "Sky" → FAIL (FM-1/2/3). View-source kiểm `<style data-theme-sync>` chứa `#2563EB`.

**Bước 2 — Happy path thay-nhi-hoa:** mở `http://thay-nhi-hoa.127.0.0.1.nip.io:3000/` →
phải **XANH LÁ #16A34A** + "Hóa học THCS thầy Nguyễn Đình Nhì". Hai tab cạnh nhau xác nhận theme KHÁC nhau (không cùng cam).

**Bước 3 — Sad path unknown:** `http://khong-ton-tai.127.0.0.1.nip.io:3000/` → ghi nhận render gì (FM-6 — nếu SKY thì là drift candidate).

**Bước 4 — Sad path BE down (tùy chọn):** stop kitehub-subscription → reload co-ha-toan → kỳ vọng graceful fallback (không crash, hiện SKY/generic) per `middleware.ts:141`.

**Bước 5 — Sad path SUSPENDED (tùy chọn):** flip 1 instance SUSPENDED → reload → kỳ vọng redirect `/suspended` (FM-7).

**Bước 6 — Phụ:** mobile hero responsive + OG metadata per tenant (view-source `<meta og:title>` = tên tenant, không "KiteClass") + WCAG contrast (ThemeSync `buildThemeStyleCss` đã clamp 4.5:1).

**Lưu ý production-accuracy (campaign §1 G2★):** dùng Host nip.io, **KHÔNG** `?tenant=` (preview override `middleware.ts:102` ưu tiên hơn Host → che FM-1/2/3). `?tenant=` chỉ để debug, không dùng làm bằng chứng G2★.
