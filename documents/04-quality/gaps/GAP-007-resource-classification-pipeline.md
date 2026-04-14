# GAP-007: Resource Classification Pipeline

**Status:** 🟢 DONE (Wave 2 Sub-PR 2.6, merged 2026-04-14; classification chain + entity + routing service landed; concrete handlers/MinIO storage deferred)
**Branch:** wave/02-data-model
**ADR:** ADR-000
**Priority:** 🔴 P0 (foundation cho AI branding)
**Domain:** AI / Backend
**Detected:** 2026-04-14
**Related Docs:**
- `documents/02-architecture/ai-branding-v2-redesign.md` §2
- `kitehub-branding/src/main/java/com/kitehub/branding/dto/BrandingAsset.java`

## Problem

Code hiện tại **không phân loại** resources thành static / template / full-AI. Mọi request đều đi qua AI generation → tốn compute, chậm, không consistent. Không có best practice 3-tier classification.

## Evidence (codebase scan 2026-04-14)

- ❌ Không có enum `ResourceCategory`, `ResourceType`, hay `BrandingCategory`
- ❌ `BrandingAsset.java` chỉ có `type` + `variant`, không có category
- ❌ `AIBrandingService` calls AIClient trực tiếp cho mọi request, không route theo loại
- ❌ Storage không tách static vs generated
- ❌ Không có decision tree "khi nào dùng AI vs khi nào dùng template"

## Proposed Fix

### Step 1: Define enums

```java
public enum ResourceCategory {
  STATIC,    // User uploaded or system default (no compute)
  TEMPLATE,  // SVG/HTML template + brand params (fast compose)
  FULL_AI    // AI-generated (heavy, async)
}

public enum ResourceType {
  LOGO, FAVICON, BANNER, HERO, COURSE_THUMBNAIL, SOCIAL_COVER, EMAIL_HEADER
}
```

### Step 2: Entity + DB

```sql
CREATE TABLE branding_resources (
  id BIGSERIAL PRIMARY KEY,
  instance_id UUID NOT NULL,
  type VARCHAR(50) NOT NULL,      -- ResourceType
  category VARCHAR(20) NOT NULL,  -- ResourceCategory
  storage_url TEXT,
  template_id BIGINT,             -- FK to image_templates (if TEMPLATE)
  ai_job_id UUID,                 -- FK to branding_jobs (if FULL_AI)
  status VARCHAR(20),
  metadata JSONB,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

### Step 3: Routing service

```java
@Service
public class ResourceRoutingService {

  public ResourceCategory classify(ResourceRequest req, TenantContext ctx) {
    // 1. User uploaded asset exists?
    if (hasStaticAsset(ctx.tenantId, req.type)) return STATIC;

    // 2. Template exists for this type + user not requesting custom?
    if (hasMatchingTemplate(req) && !req.customRequested) return TEMPLATE;

    // 3. User has AI quota?
    if (hasAIQuota(ctx)) return FULL_AI;

    // 4. Fallback to template with default
    return TEMPLATE;
  }

  public CompletableFuture<BrandingResource> route(ResourceRequest req) {
    ResourceCategory cat = classify(req, ctx);
    return switch (cat) {
      case STATIC -> staticService.fetch(req);
      case TEMPLATE -> templateService.compose(req);
      case FULL_AI -> aiService.generate(req);
    };
  }
}
```

### Step 4: Separate storage layout

```
MinIO bucket: kite-branding-assets/
├── static/<instanceId>/<type>/<filename>     (cache long: 30d+)
├── templates/<instanceId>/<type>/<hash>.png  (cache medium: 7d)
└── ai-generated/<instanceId>/<jobId>.png     (cache short: 1d, then archive)
```

## Acceptance Criteria

- [ ] 2 enums (ResourceCategory, ResourceType) + 1 entity (BrandingResource) created
- [ ] DB migration for `branding_resources` table
- [ ] `ResourceRoutingService.classify()` has unit tests cover 4 scenarios
- [ ] `ResourceRoutingService.route()` integration tests (static/template/AI)
- [ ] Storage path convention implemented
- [ ] Metrics: track `resource_requests_total{category}` by Prometheus
- [ ] Docs updated: `ai-branding-v2-redesign.md` marked implemented for §2

## Dependencies

- **None** — foundation for all other AI branding work
- Blocks: GAP-008, GAP-010

## Log

- 2026-04-14 — Created from AI Branding redesign §2
