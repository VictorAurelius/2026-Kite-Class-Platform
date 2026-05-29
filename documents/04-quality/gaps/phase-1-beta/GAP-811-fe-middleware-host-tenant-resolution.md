---
id: GAP-811
title: FE middleware host→tenant resolution — landing SSR resolve tenantId từ Host header
status: OPEN
priority: P1
phase: phase-1-beta
domain: Frontend
created: 2026-05-29
---

# GAP-811 — FE middleware host→tenant resolution

## Problem

Landing public của `kiteclass-frontend` KHÔNG resolve tenant theo Host (domain/subdomain) — đang dùng mô hình **1-tenant-per-deploy** (`NEXT_PUBLIC_TENANT_ID` build-time env). Hậu quả:

- `src/app/(public)/page.tsx:36-38` + `layout.tsx:15-16` lấy `tenantId` theo thứ tự: `searchParams.tenant` (chỉ dev) → `process.env.NEXT_PUBLIC_TENANT_ID` → fallback hardcode `11111111-1111-1111-1111-111111111111`.
- Một single FE deploy KHÔNG thể phục vụ nhiều tenant theo domain — mỗi trường phải build/deploy riêng với env riêng. Không scale cho multi-tenant SaaS (mục tiêu dự án: chuỗi tenant → domain → landing).
- `useTenantFromUrl.ts` có parse subdomain nhưng (a) chỉ chạy CLIENT-side (`window.location`), (b) trả **slug** chuỗi KHÔNG phải tenantId UUID, (c) landing SSR (`page.tsx` server component) KHÔNG dùng hook này → host-based resolution không tới được tầng SSR fetch.
- Backend landing endpoint `GET /api/v1/tenants/{tenantId}/landing` (`LandingPageController:36`) yêu cầu **UUID** trong path, không nhận slug.

Kết quả thực tế: browse `sky.kiteclass.me` (hoặc custom domain tenant) → landing render branding của tenant fallback (`11111111-...`) thay vì Sky Education, vì middleware host→tenant chưa tồn tại.

## Root Cause

1. **Không có `src/middleware.ts`**: `kiteclass-frontend` chưa có Next.js middleware để intercept request + đọc `Host` header tại edge/server. Verify: `find kiteclass/kiteclass-frontend/src -name "middleware.ts"` → 0 file.
2. **Resolution sai tầng + sai kiểu dữ liệu**: `useTenantFromUrl` client-only + trả slug; landing fetch chạy SSR cần UUID. Hai mảnh không nối được.
3. **Gateway scope hẹp**: `TenantResolverGatewayFilterFactory` (kitehub-gateway) resolve Host→tenant + inject header `X-Tenant-Id` (UUID) NHƯNG chỉ cho route `/api/**` (verify: `application.yml` routes toàn `Path=/api/...`). Request FE root `/` KHÔNG đi qua gateway filter này → SSR landing không nhận được `X-Tenant-Id`.
4. **Thiếu endpoint public slug→UUID**: logic slug→UUID ĐÃ tồn tại internal (`PublicBrandingController.resolveTenantUuid` dùng `FrontendInstanceRepository.findBySlugAndDeletedFalse(slug).getTenantId()`) nhưng CHƯA expose thành endpoint trả bare UUID cho middleware gọi. Verify: grep `by-subdomain|resolve-tenant` trong kiteclass-core controllers → 0 hit endpoint trả UUID.

## Proposed Fix

### Chốt approach — Middleware gọi BE resolve endpoint (Approach A)

Tạo `kiteclass-frontend/src/middleware.ts` intercept mọi request public, đọc `Host`, resolve sang tenantId UUID, set request header `x-tenant-id` để server component đọc qua `next/headers` `headers()`.

**Trade-off 3 approach:**

| Approach | Cơ chế | Ưu | Nhược | Verdict |
|---|---|---|---|---|
| **A. Middleware → BE resolve endpoint** | middleware fetch `GET /api/v1/tenants/by-subdomain/{slug}` → UUID → set header `x-tenant-id` | Single source of truth (BE), middleware mỏng, cache được; tách bạch FE/BE | Cần endpoint mới (CHƯA tồn tại → **GAP-813**); +1 network hop mỗi request (mitigate bằng in-memory cache TTL) | ✅ **CHỌN** |
| **B. Middleware trust gateway `X-Tenant-Id`** | route FE root `/` qua gateway → đọc header gateway inject sẵn | Tái dùng `TenantResolverGatewayFilterFactory` đã có | Gateway routes hiện chỉ `/api/**`; phải mở route FE root qua gateway = thay đổi topology lớn + rủi ro (gateway thành reverse-proxy cho cả FE static) | ❌ Defer |
| **C. Middleware tự query DB** | middleware Edge runtime query Postgres trực tiếp | 0 network hop tới BE | Edge runtime KHÔNG có DB driver; coupling FE↔DB schema = anti-pattern; vỡ tách tầng | ❌ Banned |

**Lý do chọn A:** tách tầng sạch (FE không biết DB schema), tái dùng được logic slug→UUID đã có ở `PublicBrandingController.resolveTenantUuid`, chỉ cần expose nó thành endpoint chuyên trả UUID (GAP-813). Network hop mitigate bằng in-memory `Map` cache trong middleware (host→UUID, TTL ~5 phút).

### Files cần sửa

**1. MỚI `kiteclass/kiteclass-frontend/src/middleware.ts`:**

```ts
import { NextRequest, NextResponse } from 'next/server';

// In-memory host→tenantId cache (per-instance, TTL 5 phút) để giảm network hop.
const cache = new Map<string, { id: string; exp: number }>();
const TTL_MS = 5 * 60 * 1000;
const FALLBACK = '11111111-1111-1111-1111-111111111111';

// Subdomain/custom-domain → slug (mirror logic getSubdomain trong useTenantFromUrl).
function hostToSlug(host: string): string | null {
  const hostname = host.split(':')[0]; // strip port
  if (hostname === 'localhost' || /^\d+\.\d+\.\d+\.\d+$/.test(hostname)) return null;
  const parts = hostname.split('.');
  if (parts.length >= 3) {
    const sub = parts[0] ?? '';
    if (['www', 'api', 'admin', 'staging'].includes(sub) || sub === '') return null;
    return sub;
  }
  return null; // apex/custom-domain → resolve qua endpoint by-domain (GAP-813 mở rộng)
}

async function resolveTenantId(host: string): Promise<string> {
  const cached = cache.get(host);
  if (cached && cached.exp > Date.now()) return cached.id;

  const slug = hostToSlug(host);
  if (!slug) return FALLBACK;

  // INTERNAL_API_URL: middleware chạy server-side trong Next container (giống public.ts SSR split, GAP-809).
  const base = process.env.INTERNAL_API_URL || 'http://kite-gateway:9000';
  try {
    const res = await fetch(`${base}/api/v1/tenants/by-subdomain/${encodeURIComponent(slug)}`, {
      headers: { Accept: 'application/json' },
      // middleware fetch không cache mặc định; dùng in-memory cache ở trên
    });
    if (!res.ok) return FALLBACK; // 404 slug không tồn tại → fallback
    const json = await res.json();
    const id: string = json?.data?.tenantId ?? FALLBACK; // shape do GAP-813 định nghĩa
    cache.set(host, { id, exp: Date.now() + TTL_MS });
    return id;
  } catch {
    return FALLBACK; // BE down → degrade gracefully, không vỡ trang
  }
}

export async function middleware(request: NextRequest) {
  const host = request.headers.get('host') ?? '';
  const tenantId = await resolveTenantId(host);

  // Truyền tenantId xuống server component qua request header.
  const headers = new Headers(request.headers);
  headers.set('x-tenant-id', tenantId);
  return NextResponse.next({ request: { headers } });
}

// Chỉ chạy cho route public (landing + catalog + about + contact); bỏ qua static/_next/api.
export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico|api|.*\\..*).*)'],
};
```

**2. SỬA `src/app/(public)/page.tsx`** — `getLandingPageData` ưu tiên header host-resolved thay vì env:

```ts
import { headers } from 'next/headers';
// ...
const getLandingPageData = async (tenantOverride?: string) => {
  const hdrs = await headers();
  const hostTenant = hdrs.get('x-tenant-id'); // set bởi middleware
  const tenantId: string = tenantOverride          // ?tenant= dev override (giữ)
    ?? hostTenant                                   // host-resolved (mới, ưu tiên)
    ?? process.env.NEXT_PUBLIC_TENANT_ID            // legacy 1-tenant-per-deploy fallback
    ?? '11111111-1111-1111-1111-111111111111';
  // ... phần còn lại giữ nguyên (publicApi.getLandingPage(tenantId))
};
```

**3. SỬA `src/app/(public)/layout.tsx`** — `getTenantIdentity` đọc cùng header:

```ts
import { headers } from 'next/headers';
// ...
async function getTenantIdentity() {
  const hdrs = await headers();
  const tenantId =
    hdrs.get('x-tenant-id')                          // host-resolved (mới)
    ?? process.env.NEXT_PUBLIC_TENANT_ID
    ?? '11111111-1111-1111-1111-111111111111';
  // ... publicApi.getLandingPage(tenantId) giữ nguyên
}
```

### Dependency

- **GAP-813** (BE — `GET /api/v1/tenants/by-subdomain/{slug}` trả bare UUID): BLOCKER cho middleware Approach A. Endpoint chưa tồn tại; logic slug→UUID đã có sẵn ở `PublicBrandingController.resolveTenantUuid` (`FrontendInstanceRepository.findBySlugAndDeletedFalse(slug).getTenantId()`) → GAP-813 chỉ cần expose pattern này thành public endpoint trả `{ data: { tenantId: "<uuid>" } }`. Mở rộng `by-domain/{host}` cho custom domain (dùng `InstanceRepository.findByCustomDomain`) là follow-up trong GAP-813.
- **GAP-810** (demo landing image assets): downstream — sau khi host→tenant resolve đúng, landing render đúng branding tenant (Sky) thì asset hero/GV của GAP-810 mới hiển thị đúng tenant. GAP-811 fix nguồn tenantId; GAP-810 fix nội dung asset.

## Acceptance Criteria

- [ ] `kiteclass-frontend/src/middleware.ts` tồn tại, đọc `Host`, set request header `x-tenant-id`, có in-memory cache TTL + fallback graceful khi BE down/404.
- [ ] `page.tsx` `getLandingPageData` ưu tiên `headers().get('x-tenant-id')` trước `NEXT_PUBLIC_TENANT_ID`; vẫn giữ `?tenant=` dev override + fallback hardcode.
- [ ] `layout.tsx` `getTenantIdentity` đọc cùng header host-resolved.
- [ ] Browse `sky.<domain>` (subdomain) → landing render branding Sky Education (KHÔNG fallback `11111111-...`), verify qua RST walk: middleware log resolved UUID + Network tab landing fetch dùng đúng UUID + nav/footer hiển thị tên Sky.
- [ ] `localhost` / IP / `?tenant=` dev path vẫn hoạt động (regression).
- [ ] BE down / slug không tồn tại → landing degrade về fallback branding KHÔNG crash (graceful).
- [ ] `pnpm --filter kiteclass-frontend build` PASS (per `fe-build-local-verify.md` §3 — middleware + `headers()` dynamic API có thể trigger prerender boundary; build local trước push).
- [ ] Blocked on GAP-813 endpoint; nếu GAP-813 chưa ship → GAP-811 stays OPEN/PARTIAL, KHÔNG flip DONE (per `gap-done-discipline.md` §3).

## Related

- **GAP-813** — BE endpoint `GET /api/v1/tenants/by-subdomain/{slug}` → UUID (hard dependency của Approach A).
- **GAP-810** — demo landing image assets (downstream — branding render đúng tenant trước, asset hiển thị sau).
- **GAP-809** — SSR-aware baseURL split (`INTERNAL_API_URL` vs `NEXT_PUBLIC_API_URL`) — middleware tái dùng pattern này.
- **GAP-808** — public-layout tenant-branded nav/footer (nguồn `getTenantIdentity` hiện đọc env; GAP-811 nâng lên host-resolved).
- `kitehub-gateway` `TenantResolverGatewayFilterFactory` — reference resolution logic (Host→UUID) cho `/api/**`; Approach B (route FE root qua gateway) deferred.
- `kiteclass-core` `PublicBrandingController.resolveTenantUuid` — slug→UUID logic đã có, GAP-813 expose lại.

## Log

- **2026-05-29:** Gap created. Thiết kế FE middleware host→tenant resolution (Approach A — middleware gọi BE resolve endpoint, set header `x-tenant-id` cho SSR đọc qua `next/headers`). Xác nhận hiện trạng qua đọc code: không có `middleware.ts`; landing SSR dùng `NEXT_PUBLIC_TENANT_ID` 1-tenant-per-deploy; `useTenantFromUrl` client-only + trả slug; gateway chỉ resolve `/api/**`; logic slug→UUID đã có internal ở `PublicBrandingController` nhưng chưa expose endpoint public → dependency GAP-813. Trade-off 3 approach phân tích trong Proposed Fix (A chọn, B defer, C banned). Status OPEN, blocked on GAP-813.

## Outside-in findings (3-agent audit 2026-05-29)

Bổ sung vào design trước khi lock (per `outside-in-coverage-trigger.md`):

- **Failure-mode (P1):** middleware phải xử lý graceful — subdomain không tồn tại → trang "tenant không tồn tại" (không 500); tenant SUSPENDED/EXPIRED → trang "tạm ngưng"; `by-subdomain` BE down → middleware timeout + fallback page (không crash toàn site); apex/localhost (no subdomain) → default landing hoặc JWT-claim.
- **Security (P0) — cross-ref GAP-814:** middleware/core KHÔNG được trust `x-tenant-id` client gửi; chỉ trust giá trị gateway-resolved HOẶC BE-resolved trong middleware. Gateway phải strip client `X-Tenant-Id` (GAP-814).
- **Persona (P1):** thêm preview mode (chủ trung tâm xem landing trước go-live) + OG image per-tenant (share Zalo nhóm phụ huynh).
- **Finding:** logic slug→UUID đã tồn tại internal (`PublicBrandingController.resolveTenantUuid`) → GAP-813 chỉ expose endpoint, giảm effort.
