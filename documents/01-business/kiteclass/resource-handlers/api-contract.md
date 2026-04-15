# Resource Handlers — API Contract

> Internal Java SPI (no REST endpoints — resource routing is infrastructure).

## ResourceHandler (SPI)

```java
public interface ResourceHandler {
    ResourceCategory supports();
    HandlerResult handle(ResourceRequest request, ClassificationContext context);
}
```

- Implementations register as Spring beans; `ResourceRoutingService` collects them in a `List` and builds an `EnumMap<ResourceCategory, ResourceHandler>` at construction.
- Duplicate handler for the same category → `IllegalStateException` at startup.

## HandlerResult

```java
public final class HandlerResult {
    enum Status { READY, PENDING, FALLBACK }

    Status           status;
    ResourceCategory category;
    BrandingResource resource;   // READY only
    String           jobId;       // PENDING only
    String           message;     // FALLBACK only

    static HandlerResult ready(category, resource);
    static HandlerResult pending(category, jobId);
    static HandlerResult fallback(message);
}
```

## ResourceRoutingService.route(req, ctx)

```java
HandlerResult route(ResourceRequest request, ClassificationContext context);
```

Steps: classify → dispatch to handler → rescue via FallbackHandler if FALLBACK.

## FallbackHandler.rescue(req)

```java
HandlerResult rescue(ResourceRequest request);
```

Terminal rescue. Always returns READY or PENDING, never FALLBACK.

## BrandingStoragePaths (utility)

```java
static String staticPath(UUID tenantId, ResourceType type, String filename);
static String templatePath(UUID tenantId, ResourceType type, String hash);
static String aiGeneratedPath(UUID tenantId, UUID aiJobId);

static final String BUCKET = "kite-branding-assets";
```

## Log
- 2026-04-14 — Initial contract
