# Branding Integration Guide

**Audience:** Frontend engineers (kiteclass-frontend, third-party integrators)
**Last verified:** 2026-04-26 (GAP-229 Phase 2.1)
**Related docs:** [`documents/01-business/kitehub/ai-branding/api-contract.md`](../01-business/kitehub/ai-branding/api-contract.md), [`02-architecture/ai-branding-v2-redesign.md`](../02-architecture/ai-branding-v2-redesign.md), ADR-009

---

## Two integration paths

Branding hits FE via **two distinct endpoints** depending on the user state:

| Path | Endpoint | Auth | Payload | Cache |
|------|----------|:----:|---------|-------|
| **Public** | `GET /api/v1/branding/public?tenantId={uuid\|slug}` | None | 6 fields (displayName, logoUrl, 3 colors, tagline) | none — defaults on failure |
| **Composite** | `GET /api/v1/branding/{instanceId}/package` | Bearer | Theme + assets + metadata + ETag | ETag-based 304, server Redis cache |

**Rule of thumb:** auth pages (login / register / reset) use **Public**; everything inside the authenticated app uses **Composite**.

---

## Path 1: Public branding (auth pages)

### Server contract

```http
GET /api/v1/branding/public?tenantId=abc-school
Accept: application/json
```

Response:

```json
{
  "displayName": "Trường ABC",
  "logoUrl": "https://cdn.kitehub.me/branding/abc/logo.svg",
  "primaryColor": "#1a73e8",
  "secondaryColor": "#fbbc04",
  "accentColor": "#10B981",
  "tagline": "Học cho tương lai"
}
```

If tenant is unknown / branding never set / backend unreachable → server (or client fallback) returns defaults: `{ displayName: "KiteClass", primaryColor: "#3B82F6", secondaryColor: "#8B5CF6", accentColor: "#10B981" }`.

### Reference client (kiteclass-frontend)

`src/lib/api/public-branding.ts`:

```ts
export const publicBrandingApi = {
  get: async (tenantId: string | null): Promise<PublicBranding> => {
    if (!tenantId) return DEFAULT_PUBLIC_BRANDING;
    try {
      const { data } = await apiClient.get<PublicBranding>(
        '/api/v1/branding/public',
        { params: { tenantId } },
      );
      return { ...DEFAULT_PUBLIC_BRANDING, ...data };
    } catch {
      // Graceful degradation: never block auth pages on branding fetch.
      return DEFAULT_PUBLIC_BRANDING;
    }
  },
};
```

### React provider pattern

`src/providers/BrandingProvider.tsx` — applies branding to `<html>` via CSS variables:

```tsx
'use client';

export function BrandingProvider({ children }: { children: ReactNode }) {
  const tenantId = useTenantFromUrl();              // resolve from path / subdomain
  const { branding, isLoading } = usePublicBranding(tenantId);

  useEffect(() => {
    applyCssVars(branding);
  }, [branding]);

  return (
    <BrandingContext.Provider value={{ branding, isLoading, tenantId }}>
      {children}
    </BrandingContext.Provider>
  );
}

function applyCssVars(b: PublicBranding) {
  const root = document.documentElement;
  // Raw hex — for consumers reading `var(--brand-primary)` directly.
  root.style.setProperty('--brand-primary', b.primaryColor);
  root.style.setProperty('--brand-secondary', b.secondaryColor);
  root.style.setProperty('--brand-accent', b.accentColor);

  // Shadcn/Tailwind H S L channels (space-separated triple, no hsl() wrapper).
  const primaryHsl = hexToHslString(b.primaryColor);
  if (primaryHsl) root.style.setProperty('--primary', primaryHsl);
  const accentHsl = hexToHslString(b.accentColor);
  if (accentHsl) root.style.setProperty('--accent', accentHsl);
}
```

### Why two CSS-var formats

Existing Shadcn UI components key off Tailwind's `--primary` / `--accent` H-S-L triple (e.g. `bg-primary` resolves to `hsl(var(--primary))`). Custom components reading raw hex use `var(--brand-primary)`. Setting both keeps both consumer styles working without a coordinated migration.

### Tenant resolver

`useTenantFromUrl` extracts tenant identifier from:

1. Subdomain (e.g. `abc-school.kitehub.me` → `abc-school`)
2. Path prefix (e.g. `/t/abc-school/login`)
3. Query param fallback (`?tenant=abc-school`)

Returned slug is sent to `/api/v1/branding/public?tenantId=...`. The backend resolves slug → UUID via `FrontendInstanceRepository.findBySlugAndDeletedFalse` (see `PublicBrandingController.resolveTenantUuid`).

### Failure modes

| Scenario | Behavior |
|---------|----------|
| Tenant not in URL | Render defaults; no fetch |
| Tenant slug unknown | Server returns defaults (200, NOT 404) |
| Backend unreachable | Client catch returns defaults |
| Malformed hex color | `hexToHslString` returns null → that var stays unset → CSS fallback |

**Never block render on branding fetch.** Default palette renders immediately, branded palette overwrites on resolve. This keeps Time-to-First-Paint constant regardless of branding-service latency.

---

## Path 2: Composite package (authenticated app)

### Server contract

```http
GET /api/v1/branding/{instanceId}/package
Authorization: Bearer <jwt>
If-None-Match: W/"v3-a1b2c3d4"      ← ETag from previous response
```

Response (200):

```json
{
  "instanceId": 12345,
  "tenantId": "kitehub-tenant-uuid",
  "slug": "abc-school",
  "frontendUrl": "https://abc-school.kitehub.me",
  "brandingVersion": 3,
  "deployedAt": "2026-04-26T10:05:00Z",
  "assets": [
    { "type": "logo",   "category": "STATIC",   "url": "https://cdn/.../logo.svg",  "alt": "ABC School logo" },
    { "type": "hero",   "category": "TEMPLATE", "url": "https://cdn/.../hero.png",  "alt": "Hero banner" },
    { "type": "favicon","category": "STATIC",   "url": "https://cdn/.../fav.ico",   "alt": "" }
  ]
}
```

Response header: `ETag: W/"v3-a1b2c3d4"` (per BR-PKG-002).

If client's `If-None-Match` matches, server returns **304 Not Modified** with empty body and the same `ETag`. Client should keep using its cached representation.

### Caching rules

| Layer | Mechanism | Eviction |
|-------|-----------|----------|
| Server (Redis via Spring Cache) | `CachingBrandingPackageProxy` | Outbox event listener for `instance.deployed` / `instance.regenerating` |
| Server-derived ETag | `W/"v{brandingVersion}-{hashHex}"` | Bumps automatically when `brandingVersion` changes |
| Client (browser HTTP cache) | Standard `If-None-Match` revalidation | 304 from server |
| Client (in-memory React cache) | TanStack Query / SWR keyed by `instanceId` | Refetch on `branding.refreshed` SSE event |

### Reference client (illustrative)

```ts
export async function fetchBrandingPackage(
  instanceId: number,
  cachedEtag?: string,
): Promise<{ pkg: BrandingPackage | null; etag: string | null; status: 200 | 304 }> {
  const headers: Record<string, string> = {};
  if (cachedEtag) headers['If-None-Match'] = cachedEtag;

  const res = await fetch(`/api/v1/branding/${instanceId}/package`, { headers });
  if (res.status === 304) {
    return { pkg: null, etag: cachedEtag ?? null, status: 304 };
  }
  if (!res.ok) {
    throw new Error(`branding package fetch failed: ${res.status}`);
  }
  const pkg = (await res.json()) as BrandingPackage;
  const etag = res.headers.get('ETag');
  return { pkg, etag, status: 200 };
}
```

### Reactive updates (rebrand events)

When the tenant rebrands (`POST /api/v1/instances/{id}/rebrand` → REGENERATING → DEPLOYED), the server fires an outbox event `instance.deployed` which evicts the Redis cache. Clients learn about the new branding two ways:

1. **Polling:** next package fetch returns the new ETag automatically (304 → 200 once they revalidate).
2. **Push (SSE):** subscribe to `/api/v1/branding/events?instanceId={id}` (when wired). On `branding.refreshed` event, refetch the package.

> **Status of SSE channel:** In-flight at the time of writing (Wave 7+). Without SSE, clients should refetch on a 30–60s schedule or after specific user actions (refresh button, route change).

---

## Theme application reference

CSS variable names produced by `BrandingProvider`:

| Variable | Source | Consumer |
|----------|--------|----------|
| `--brand-primary` | hex from API | Custom components reading raw hex |
| `--brand-secondary` | hex from API | Same |
| `--brand-accent` | hex from API | Same |
| `--primary` | H S L triple from API hex | Shadcn `bg-primary`, `text-primary`, etc. |
| `--accent` | H S L triple from API hex | Shadcn `bg-accent` |

For full Shadcn theming, also map `--secondary`, `--muted`, `--card`, `--border` once design system needs land. Today only primary + accent are derived.

---

## Outbox events (server → cache layer)

These events drive cache eviction; FE doesn't subscribe to them directly but their behavior matters for understanding stale-data windows:

| Event | Producer | Consumer | Effect |
|-------|----------|----------|--------|
| `instance.deployed` | `InstanceLifecycleService.markBrandingCompleted` | `CachingBrandingPackageProxy` listener | Evict Redis cache for `instanceId` |
| `instance.regenerating` | `InstanceLifecycleService.rebrand` | Same | Evict Redis cache (FE will fetch latest after) |
| `branding.refresh.required` | KiteHub trial-to-paid migration → KiteClass core | Future SSE bridge | Notify FE of paid-tier feature unlock |

Source: `documents/01-business/kitehub/ai-branding/rules.md` BR-PKG, BR-LIFE.

---

## Common pitfalls

| Pitfall | Fix |
|---------|-----|
| Hardcoding default colors in components | Always read from CSS var; let `BrandingProvider` own the values |
| Fetching package on every route navigation | Use TanStack Query `staleTime: Infinity` keyed by `instanceId`; refetch only on outbox event / manual refresh |
| Ignoring 304 responses | Send `If-None-Match`; treat 304 as "your cache is still good" |
| Blocking render on branding fetch | Use defaults immediately; overwrite when fetch resolves |
| Reading `--primary` as hex | It's H-S-L triple — wrap in `hsl(...)` if reading directly, OR use `var(--brand-primary)` for hex |
| Not handling slug → UUID | Server resolves both; client just sends whichever is in URL |

---

## Local development

The kiteclass-core dev profile seeds two demo tenants:

- `abc-school` — fully branded (logo, colors, tagline)
- `default-demo` — minimal (uses defaults to test fallback path)

Test the public path: `curl http://localhost:8080/api/v1/branding/public?tenantId=abc-school`.

Test the auth path with a JWT and known instance: see `documents/05-guides/local-dev-setup.md` (existing — separate guide).

---

## Related

- Server impl: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/branding/controller/{BrandingPackageController,PublicBrandingController}.java`
- FE impl: `kiteclass/kiteclass-frontend/src/{providers/BrandingProvider.tsx, hooks/use-public-branding.ts, lib/api/public-branding.ts}`
- ADR-009 (composite + ETag) — `documents/02-architecture/adr/ADR-009-*.md` if landed
- ai-branding-guidelines.md §7 (Integration với KiteClass Frontend)
