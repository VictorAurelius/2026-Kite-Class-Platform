# Resource Classification — API Contract

> Internal service contract (Java). HTTP endpoints added when branding pipeline REST layer lands in a later wave.

## ResourceRoutingService

### `classify(request, context) → ResourceCategory`
Returns first matching category from ordered classifier chain.

```java
ResourceRequest req = ResourceRequest.builder()
    .type(ResourceType.BANNER)
    .customRequested(false)
    .build();

ClassificationContext ctx = ClassificationContext.builder()
    .hasStaticAsset(false)
    .hasMatchingTemplate(true)
    .hasAIQuota(true)
    .build();

ResourceCategory cat = routingService.classify(req, ctx);  // → TEMPLATE
```

**Throws:** `IllegalStateException` if chain lacks terminal classifier.

## BrandingResourceRepository

| Method | Returns |
|--------|---------|
| `findByTypeAndDeletedFalse(type)` | All resources of a type |
| `findFirstByTypeAndCategoryAndDeletedFalse(type, category)` | First match (ordered by id) |
| `findByCategoryAndDeletedFalse(category)` | All active resources in a category |

## BrandingResource.validateInvariants()
Enforces BR-RES-002..004. Call before persist.

## Log
- 2026-04-14 — Initial contract
