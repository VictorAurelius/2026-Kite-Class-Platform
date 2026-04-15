# ADR-009: Branding Package API — Composite + ETag + Proxy Cache

**Status:** ACCEPTED
**Date:** 2026-04-14
**Deciders:** Tech Lead + Architect
**Related Gap:** GAP-010 (Wave 3 Sub-PR 3.4)

## Context

KiteClass frontend (per-tenant site) renders many pages. Each page needs theme vars + logo URL + hero URL + email header URL + favicon etc. Naive: fetch each via separate endpoint. Chatty (10 round-trips per page load) and races (theme loads after layout paints → flash-of-default-theme).

## Decision

**Single composite endpoint + ETag + server-side Proxy cache:**

```
GET /api/v1/branding/{instanceId}/package
Response 200:
{
  "instanceId": "...",
  "brandingVersion": 7,
  "theme": {
    "colors": { "primary": "#...", "secondary": "#..." },
    "typography": { "heading": "...", "body": "..." },
    "spacing": { ... }
  },
  "assets": {
    "logo":        { "url": "...", "alt": "...", "category": "STATIC" },
    "favicon":     { "url": "...", "alt": "..." },
    "banner":      { "url": "...", "alt": "...", "category": "TEMPLATE" },
    "hero":        { "url": "...", "alt": "..." },
    "emailHeader": { "url": "...", "alt": "..." }
  },
  "metadata": { "deployedAt": "...", "tier": "PREMIUM" }
}
Headers:
  ETag: "W/\"v7-a3f8\""
  Cache-Control: public, max-age=3600
```

Client (FE) caches by `ETag`; subsequent requests send `If-None-Match` → 304 Not Modified (no body, cheap).

**Server-side caching (Proxy pattern):**

```java
class CachingBrandingPackageProxy implements BrandingPackageService {
  @Cacheable("branding-package")
  BrandingPackage get(String instanceId) { return delegate.get(instanceId); }

  @CacheEvict("branding-package")
  void onEvent(InstanceDeployedEvent e) { }  // invalidate on RabbitMQ event

  @CacheEvict("branding-package")
  void onEvent(InstanceRegeneratedEvent e) { }
}
```

Cache TTL = 1h; evicted on any lifecycle event mutating branding. Bust via brandingVersion change.

## Consequences

### Positive
- ✅ 1 round-trip vs 10 — latency win for FE cold loads
- ✅ ETag prevents wasted bandwidth for repeat visits
- ✅ Proxy cache makes hot tenants cheap (single DB fetch per eviction cycle)
- ✅ Invalidation driven by outbox events (ADR-007) — no stale branding
- ✅ Satisfies GAP-043 cache-stampede protection by using `Caffeine` single-flight

### Negative
- ❌ Composite response is "fat"; must not grow unbounded — keep to core theme/assets/metadata
- ❌ Cache invalidation bugs = showing old branding. Integration tests mandatory.

## Alternatives

- **A. N endpoints, client combines** — rejected: chatty, race-prone.
- **B. GraphQL** — rejected: not justified by scope; REST + ETag gives 90% of benefit.
- **C. No server cache, rely only on CDN + ETag** — rejected: CDN invalidation on branding change is slow; in-process cache faster.

## Implementation Notes

### Builder for response

`BrandingPackageResponse.Builder` (per §3.9 design rules — avoid long params) assembles from:
- `FrontendInstance` (for brandingVersion, deployedAt, tier)
- `BrandingResource` list (assets + categories)
- Theme JSON blob (from branding service, TBD Sub-PR 3.4)

### Cache configuration

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=1h
    cache-names: branding-package
```

`maximumSize=10000` covers up to ~10k active tenants before eviction thrash.

### 304 generation

```java
@GetMapping("/package")
public ResponseEntity<BrandingPackage> get(
    @PathVariable String instanceId,
    @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
  var pkg = service.get(instanceId);
  String etag = "W/\"v" + pkg.brandingVersion() + "-" + pkg.hashCode() + "\"";
  if (etag.equals(ifNoneMatch)) return ResponseEntity.status(304).build();
  return ResponseEntity.ok().eTag(etag).body(pkg);
}
```

## References

- GAP-010, GAP-043 (cache-stampede)
- design-patterns.md §2 (Proxy, Builder)
- MDN HTTP ETag docs

## Log

- 2026-04-14 — Accepted
