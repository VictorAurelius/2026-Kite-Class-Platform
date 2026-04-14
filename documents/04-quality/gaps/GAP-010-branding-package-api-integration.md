# GAP-010: Branding Package API + KiteClass Integration Testing

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / Frontend / Integration
**Detected:** 2026-04-14
**Related Docs:**
- `documents/02-architecture/ai-branding-v2-redesign.md` §6

## Problem

KiteHub generates branding resources nhưng **chưa test** tích hợp với kiteclass-frontend:

1. Không có composite endpoint "full branding package" — FE phải gọi nhiều API
2. Không có webhook/event notify FE khi branding update
3. Không có cache strategy rõ ràng (CDN, browser)
4. Chưa có integration test end-to-end

## Evidence

- `AIBrandingController.java`: có `/generate-theme` và `/assets/{instanceId}` riêng biệt, không có composite
- **Không có** endpoint `GET /api/v1/branding/{instanceId}/package`
- **Không có** webhook publisher khi branding completed
- **Không có** tests `BrandingIntegrationIT` kết nối kitehub-branding → kiteclass-core → kiteclass-frontend
- CDN cache headers không được verify

## Proposed Fix

### 1. Composite Endpoint

```java
@GetMapping("/api/v1/branding/{instanceId}/package")
public BrandingPackage getFullPackage(@PathVariable String instanceId) {
  return BrandingPackage.builder()
    .instanceId(instanceId)
    .status(instanceService.getStatus(instanceId))
    .theme(themeService.getConfig(instanceId))
    .assets(assetService.getAllUrls(instanceId))
    .metadata(Metadata.of(
      generatedAt,
      brandingVersion,
      Etag.compute(...)
    ))
    .build();
}
```

**Response shape:**
```json
{
  "instanceId": "uuid",
  "status": "DEPLOYED",
  "theme": {
    "primaryColor": "#2563eb",
    "secondaryColor": "#1e40af",
    "accentColor": "#f59e0b",
    "fonts": { "heading": "Inter", "body": "Inter" }
  },
  "assets": {
    "logo": "https://cdn.../logo.png",
    "favicon": "https://cdn.../favicon.ico",
    "banner": "https://cdn.../banner.png",
    "hero": "https://cdn.../hero.png",
    "courseThumbnailDefault": "https://cdn.../thumb.png"
  },
  "metadata": {
    "generatedAt": "2026-04-14T10:00:00Z",
    "version": 3,
    "etag": "W/\"abc123\""
  }
}
```

### 2. KiteClass Frontend Consumption

```typescript
// kiteclass-frontend/src/lib/branding.ts
export async function loadBranding(instanceId: string): Promise<BrandingPackage> {
  // 1. Check localStorage cache
  const cached = localStorage.getItem(`branding:${instanceId}`);
  if (cached) {
    const pkg = JSON.parse(cached);
    // Conditional fetch with ETag
    const resp = await fetch(`/api/v1/branding/${instanceId}/package`, {
      headers: { 'If-None-Match': pkg.metadata.etag }
    });
    if (resp.status === 304) return pkg;  // still fresh
    const fresh = await resp.json();
    localStorage.setItem(`branding:${instanceId}`, JSON.stringify(fresh));
    return fresh;
  }
  // 2. Fresh fetch
  const resp = await fetch(`/api/v1/branding/${instanceId}/package`);
  const pkg = await resp.json();
  localStorage.setItem(`branding:${instanceId}`, JSON.stringify(pkg));
  return pkg;
}

// src/providers/BrandingProvider.tsx
export function BrandingProvider({ children }) {
  const branding = useBranding();  // loads package
  // Inject CSS variables from theme
  useEffect(() => {
    document.documentElement.style.setProperty('--color-primary', branding.theme.primaryColor);
    // ...
  }, [branding]);
  return <>{children}</>;
}
```

### 3. Cache Strategy

**Server side (response headers):**
```
Cache-Control: public, max-age=3600, stale-while-revalidate=86400
ETag: W/"sha256-hash-of-package"
Vary: Accept-Encoding
```

**CDN (CloudFront/Cloudflare):**
- Asset URLs (images): cache 7 days, purge on update
- Package API: cache 1 hour, respect ETag revalidation

**Browser (localStorage):**
- Cache package với ETag
- Revalidate on page load (304 response = use cache)

### 4. Webhook Event

```java
// When branding completes:
@EventListener
public void onBrandingCompleted(BrandingCompletedEvent e) {
  // Publish via RabbitMQ
  rabbitTemplate.convertAndSend("branding.exchange",
    "branding.updated",
    new BrandingUpdatedPayload(e.instanceId, e.version));
}

// KiteClass-core listener:
@RabbitListener(queues = "kiteclass.branding.updates")
public void onBrandingUpdated(BrandingUpdatedPayload p) {
  // Option A: Invalidate CDN cache
  cdnService.purge(getBrandingUrls(p.instanceId));

  // Option B: Push to connected FE clients via SSE/WebSocket
  sseService.broadcast(p.instanceId, "branding.updated", p);
}
```

### 5. Integration Test Suite

```java
@SpringBootTest
public class BrandingE2EIntegrationTest {

  @Test
  void full_branding_flow_from_tenant_creation_to_FE_apply() {
    // 1. Create tenant
    var tenant = tenantService.create(...);

    // 2. Trigger provisioning
    lifecycleService.initiate(tenant.id);

    // 3. Wait for DEPLOYED status (with timeout)
    await().atMost(60, SECONDS)
      .until(() -> instance.getStatus() == DEPLOYED);

    // 4. Fetch package via API
    var pkg = apiClient.getPackage(instance.id);
    assertThat(pkg.status).isEqualTo("DEPLOYED");
    assertThat(pkg.theme.primaryColor).isNotNull();
    assertThat(pkg.assets.logo).isNotNull();

    // 5. Fetch asset URL - verify CDN
    var response = httpClient.get(pkg.assets.logo);
    assertThat(response.status).isEqualTo(200);
    assertThat(response.headers.get("Cache-Control")).contains("max-age=");

    // 6. Update branding
    brandingService.rebrand(instance.id, new Rebrand(...));

    // 7. Verify ETag changes
    var pkg2 = apiClient.getPackage(instance.id);
    assertThat(pkg2.metadata.etag).isNotEqualTo(pkg.metadata.etag);
    assertThat(pkg2.metadata.version).isGreaterThan(pkg.metadata.version);
  }
}
```

### 6. FE Test

```typescript
// kiteclass-frontend/tests/branding.test.ts
describe('Branding provider', () => {
  it('loads package on mount and applies CSS vars', async () => {
    render(<BrandingProvider><App /></BrandingProvider>);
    await waitFor(() => {
      expect(document.documentElement.style.getPropertyValue('--color-primary'))
        .toBe('#2563eb');
    });
  });

  it('uses cached package with ETag revalidation', async () => {
    // Mock localStorage + fetch
    // Verify 304 response reuses cache
  });
});
```

## Acceptance Criteria

- [ ] `GET /api/v1/branding/{id}/package` endpoint implemented với ETag
- [ ] `Cache-Control` headers configured
- [ ] RabbitMQ event `branding.updated` published
- [ ] KiteClass-core listener handles cache invalidation
- [ ] KiteClass-frontend `loadBranding()` + `BrandingProvider`
- [ ] Integration test `BrandingE2EIntegrationTest` passes
- [ ] FE unit test for branding provider
- [ ] Load test: 100 concurrent FE requests to package endpoint
- [ ] Docs: `documents/05-guides/branding-integration.md` với examples

## Dependencies

- **Blocked by GAP-007** (classification) — package API returns categorized assets
- **Blocked by GAP-009** (instance lifecycle) — status field in package

## Log

- 2026-04-14 — Created from AI Branding redesign §6
