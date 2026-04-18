# GAP-032: Branded Error Pages (404/500/Maintenance)

**Status:** 🟢 DONE (Wave 4)
**Priority:** 🟠 P1
**Domain:** Frontend / UX
**Detected:** 2026-04-14 (simulation: End User × Daily Usage × C2 UX)
**Resolved:** 2026-04-18 (Wave 4 — branding propagation cluster)

## Wave 4 resolution

- New `kitehub-gateway` `BrandingClient` (reactive `WebClient`, Caffeine 5-min
  TTL) fetches minimal branding for the resolved tenant.
- `FallbackController` refactored — every fallback now returns branded HTML
  (status 503) with the tenant's logo + primary color; falls back to defaults
  when the branding fetch fails or the tenant header is absent.
- Templates: `503-service-unavailable.html`, `404-not-found.html`,
  `500-server-error.html` in `classpath:/templates/errors/` using simple
  `{{token}}` substitution (keeps the gateway image small, avoids reactive
  template-engine friction).
- `FallbackControllerTest` asserts branded + default paths.

## Problem

Khi student/teacher gặp error pages (404, 500, maintenance, offline), **KHÔNG có tenant branding** — họ thấy default KiteClass colors/logo. Broken consistency.

## Proposed Fix

Error pages fetch tenant branding (cached), apply theme:

```tsx
// kiteclass-frontend/src/app/not-found.tsx
export default async function NotFound() {
  const branding = await getBrandingFromCache();
  return (
    <BrandedLayout branding={branding}>
      <h1>Trang không tồn tại</h1>
      <p>Quay lại <Link href="/">trang chủ</Link> của {branding.name}</p>
    </BrandedLayout>
  );
}
```

Cover: 404, 500, 503 maintenance, offline page, loading spinner themed.

## Acceptance Criteria

- [ ] 404, 500, 503 pages use tenant branding
- [ ] Offline page themed
- [ ] Loading states use brand primary color
- [ ] Fallback to KiteClass default nếu branding unavailable
- [ ] Test: visit non-existent URL → see branded 404

## Dependencies

- GAP-010 (package API) — branding fetch source

## Log

- 2026-04-14 — Discovered via simulation
