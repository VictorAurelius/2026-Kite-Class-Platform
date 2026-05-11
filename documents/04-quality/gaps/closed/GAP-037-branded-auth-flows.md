# GAP-037: Branded Authentication Flows (Email verify, Reset Password)

**Status:** 🟢 DONE (Wave 4)
**Priority:** 🟠 P1
**Domain:** Frontend / Integration
**Detected:** 2026-04-14 (simulation: End User × Onboarding × C2 UX)
**Resolved:** 2026-04-18 (Wave 4 — branding propagation cluster)

## Wave 4 resolution

- `PublicBrandingController` (`GET /api/v1/branding/public?tenantId=...`)
  exposes a minimal branding payload safe for anonymous callers (display name,
  logo URL, primary/secondary/accent colors, tagline). No auth required —
  slug or UUID both accepted.
- `kiteclass-frontend`:
  - `lib/api/public-branding.ts` — new public-branding client + defaults.
  - `hooks/use-public-branding.ts` — React Query hook, 5-min staleTime.
  - `providers/BrandingProvider.tsx` — wraps auth pages, injects CSS vars
    (`--brand-primary`, Shadcn `--primary` HSL triple) onto `<html>`.
  - `app/(auth)/layout.tsx` wraps children with `BrandingProvider`.
  - `components/layout/auth-layout.tsx` reads `useTenantBranding()` and
    shows tenant logo + display name on both desktop hero panel and
    mobile top bar. Degrades to KiteClass defaults when tenant unknown.
- Email templates (`welcome`, `email-verification`, `trial-expiration-warning`)
  now branded via GAP-021.

## Problem

Tenant rebrand đẹp, NHƯNG:
- ❌ Email verification page: default KiteClass design
- ❌ Password reset page: default
- ❌ "Check your email" page: default
- ❌ Invalid token page: default

Student/teacher nhận email với link → click → landing tại **default KiteClass** (không biết tenant này).

Khác với GAP-021 (email templates) — gap này về **auth pages trong FE**.

## Proposed Fix

### 1. Tenant-aware Auth Pages

URL structure:
```
{tenantSlug}.kiteclass.com/auth/verify?token=...
{tenantSlug}.kiteclass.com/auth/reset-password?token=...
```

FE extract tenant từ subdomain → load branding → render pages với theme.

### 2. Pages cần cover

| Page | Current | Cần |
|------|---------|-----|
| `/auth/verify-email` | Default | Branded |
| `/auth/reset-password` | Default | Branded |
| `/auth/forgot-password` | Default | Branded |
| `/auth/login` | Partial branded | Full branded |
| `/auth/register` | Partial | Full branded |
| `/auth/invalid-token` | Default | Branded |
| `/auth/expired-link` | Default | Branded |
| `/auth/logout-success` | Default | Branded |

### 3. Server-side branding resolution

```tsx
// app/auth/verify-email/page.tsx
export default async function VerifyEmailPage({ params }) {
  const tenant = await resolveTenantFromHost();
  const branding = await getBrandingPackage(tenant.id);

  return (
    <BrandedAuthLayout branding={branding}>
      {/* Verify logic */}
    </BrandedAuthLayout>
  );
}
```

### 4. Email template links

Ensure email links point to tenant subdomain:
```
❌ https://kiteclass.com/verify?token=X (default domain)
✅ https://{tenantSlug}.kiteclass.com/verify?token=X
```

### 5. Fallback

Nếu không resolve được tenant (e.g., email link typo) → default KiteClass design với message "Không xác định được trung tâm, kiểm tra link".

## Acceptance Criteria

- [ ] 8 auth pages branded với tenant theme
- [ ] Subdomain → tenant → branding resolution
- [ ] Email links use tenant subdomain
- [ ] Invalid/expired token → branded page, not raw error
- [ ] Logo, colors, fonts apply consistently
- [ ] Responsive mobile-friendly
- [ ] E2E test: register → receive email → click → see branded verify page

## Dependencies

- GAP-010 (package API) — branding source
- GAP-021 (email branding) — email template links

## Log

- 2026-04-14 — Broken brand consistency qua simulation
