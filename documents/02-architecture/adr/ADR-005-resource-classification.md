# ADR-005: Resource Classification Pipeline

**Status:** ACCEPTED
**Date:** 2026-04-14
**Deciders:** Tech Lead + AI Lead
**Related Gap:** GAP-007

## Context

Current AI Branding luôn gọi AI model cho mọi request → expensive, slow, inconsistent.

80% use cases có thể dùng template (pre-built) hoặc static (user-uploaded) thay vì AI full generation.

User raised principle:
> "SaaS giáo dục, KHÔNG phải AI creative platform. Target: best branded instance."

Template-first approach saves cost + latency + improves consistency.

## Decision

**Phân loại resources thành 3 categories với routing chain:**

```java
enum ResourceCategory {
  STATIC,    // User uploaded / system default (<100ms, $0)
  TEMPLATE,  // SVG template + brand params (1-3s, ~$0)
  FULL_AI    // AI generated (10s-5min, expensive)
}
```

Chain of Responsibility routing:
```
ResourceRequest
  → StaticHandler: "Has uploaded asset?" Yes → return
  → TemplateHandler: "Matching template + not custom?" Yes → compose
  → AIHandler: "User has quota?" Yes → async generate
  → FallbackHandler: template with default colors
```

80% requests land at TEMPLATE. AI only when user explicitly requests custom.

## Consequences

### Positive
- ✅ 80% cost reduction (fewer AI calls)
- ✅ 80% latency reduction (<3s vs 5min)
- ✅ Brand consistency enforced via templates
- ✅ Graceful fallback when AI fails
- ✅ Extensible (add new handler = new strategy)

### Negative
- ❌ Template library dependency (GAP-011 blocker)
- ❌ Routing complexity (but Chain pattern handles)
- ❌ User education: "when do I get AI?" (tier-gated)

## Alternatives Considered

### Alternative A: AI for everything
Pros: Simple code
Cons: Current state — overloaded, expensive

**Rejected:** unsustainable

### Alternative B: User picks mode explicitly
Pros: User control
Cons: Decision fatigue, most users pick default

**Rejected:** guided workflow better

### Alternative C: ML classifier decides
Pros: Smart
Cons: Over-engineering, unpredictable

**Rejected:** deterministic rules sufficient

## Implementation Notes

Migration V32:
```sql
CREATE TABLE branding_resources (
  id BIGSERIAL PRIMARY KEY,
  instance_id UUID,
  type VARCHAR(50),             -- LOGO, BANNER, HERO, ...
  category VARCHAR(20),         -- STATIC, TEMPLATE, FULL_AI
  storage_url TEXT,
  template_id BIGINT,           -- FK if TEMPLATE
  ai_job_id VARCHAR(36),        -- FK if FULL_AI
  status VARCHAR(20),
  metadata JSONB,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE INDEX idx_br_instance ON branding_resources(instance_id);
CREATE INDEX idx_br_category ON branding_resources(category);
```

Storage layout:
```
MinIO: kite-branding-assets/
├── static/{instanceId}/{type}/{file}    (30d cache)
├── templates/{instanceId}/{type}/{hash} (7d cache)
└── ai-generated/{instanceId}/{jobId}    (1d cache, archive)
```

Routing service:
```java
@Service
public class ResourceRoutingService {
  ResourceCategory classify(ResourceRequest req, TenantContext ctx) {
    if (hasStaticAsset(ctx, req.type)) return STATIC;
    if (hasMatchingTemplate(req) && !req.customRequested) return TEMPLATE;
    if (hasAIQuota(ctx)) return FULL_AI;
    return TEMPLATE;  // fallback with default
  }
}
```

## References

- GAP-007 implementation
- Design pattern: Chain of Responsibility
- Related: ADR-001 (K-12 data model), GAP-011 (template library)

## Log
- 2026-04-14 — Accepted
