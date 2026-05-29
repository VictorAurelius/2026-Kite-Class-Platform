# GAP-808 — Public tenant homepage không branded (landing chain 500 → generic fallback)

**Status:** (canonical: gap-status.csv) DONE 2026-05-29
**Priority:** P1 · **Domain:** Mixed · **Phase:** phase-1-beta

## Problem

Trang chủ public của tenant KiteClass (`(public)/page.tsx` tại `/?tenant={id}`) hiển thị **generic KiteClass xanh** thay vì branding tenant (cam + tên + logo). Surfaced 2026-05-29 demo-trio walk khi verify trang chủ Sky Education. Chuỗi 5 bug độc lập (feature chưa từng walk e2e):

1. `landing_pages` table KHÔNG tồn tại — `LandingPage` entity + `LandingPageServiceImpl` shipped thiếu Flyway migration → `relation "public.landing_pages" does not exist` → API 500.
2. `getOrCreateDefault()` tạo landing default hardcode (#3B82F6 blue) — KHÔNG inherit branding tenant.
3. `getLandingPage()` `@Transactional(readOnly=true)` nhưng getOrCreateDefault INSERT → `cannot execute INSERT in a read-only transaction` (mọi tenant first-load).
4. Gateway `/api/v1/tenants/{id}/landing` rơi vào `/api/v1/**` catch-all → TenantResolver → 400 khi no-subdomain (SSR/localhost). tenantId đã trong path.
5. FE `public.ts` baseURL dùng `NEXT_PUBLIC_API_URL=localhost:9000` cho cả SSR → `ECONNREFUSED 127.0.0.1:9000` trong Next container (cần `INTERNAL_API_URL=kite-gateway:9000` server-side).
6. (polish) `ThemeSync` chỉ set `--theme-*` RGB, không `--primary` HSL → shadcn buttons giữ xanh dù accent cam.

## Acceptance Criteria

- [x] V75 migration tạo `landing_pages` table khớp entity
- [x] `getOrCreateDefault` inherit branding tenant (primaryColor/secondaryColor/logoUrl/displayName→heroTitle/tagline)
- [x] `getLandingPage` writable tx (lazy-create works)
- [x] Gateway route public `/api/v1/tenants/*/landing` skip TenantResolver
- [x] FE `public.ts` SSR-aware baseURL (server→INTERNAL_API_URL, browser→NEXT_PUBLIC_API_URL)
- [x] `ThemeSync` set `--primary`/`--secondary`/`--accent` HSL (shadcn buttons branded)
- [x] Walk verified: `/?tenant=e8ff87e1-...` render "Trung tâm Anh ngữ Sky Education" + `--primary: 21 90% 48%` (cam) + full orange theme

## Log

- **2026-05-29 (DONE):** RST walk demo-trio fix-forward — 6 fixes shipped (V75 + LandingPageServiceImpl inherit + writable tx + gateway public route + public.ts SSR baseURL + ThemeSync HSL). Landing API 200 + Sky branding; homepage fully Sky-branded (visual evidence local `08-thesis/evidence/demo-trio/12-public-homepage-sky-branded.png`). See audits/rst-html/2026-05-29-demo-trio-walk-findings.md. Follow-up GAP-809 (FE↔BE contract drift /classes /invoices) + layout header hardcode "KiteClass" (nav still generic — TemplateRenderer hero/theme branded, layout nav not tenant-driven).
