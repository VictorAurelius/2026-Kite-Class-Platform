# Resource Handlers — Business Rules

**Domain:** resource-handlers
**Source:** Wave 3 Sub-PR 3.3, ADR-005

## Rules

| ID | Rule |
|----|------|
| BR-HND-001 | Exactly one `ResourceHandler` per `ResourceCategory` (STATIC / TEMPLATE / FULL_AI) |
| BR-HND-002 | Duplicate handler registration → construction throws `IllegalStateException` |
| BR-HND-003 | Handler returning `FALLBACK` status → router escalates to `FallbackHandler.rescue` |
| BR-HND-004 | `FallbackHandler` is NOT a `ResourceHandler` — invoked explicitly by router, not by classifier |
| BR-HND-005 | `AIResourceHandler` MUST use `ResilientAIClient` (Primary bean from Sub-PR 3.2) — never bare `AIClient` impls |
| BR-HND-006 | Handler `handle()` is pure-read + returns pending jobId — no blocking on heavy generation |

## Storage path convention (MinIO)

Per ADR-005 and ai-branding-v2-redesign.md §2.4:

```
kite-branding-assets/
  ├── static/{tenantId}/{type}/{filename}      cache 30d+
  ├── templates/{tenantId}/{type}/{hash}.png   cache 7d
  └── ai-generated/{tenantId}/{jobId}.png      cache 1d, archive later
```

Helper: `BrandingStoragePaths.{staticPath, templatePath, aiGeneratedPath}` — single source of truth so ops can tune cache tiers without touching handlers.

## Config keys

| Key | Default | Purpose |
|-----|---------|---------|
| `branding.storage.bucket` | `kite-branding-assets` | MinIO bucket name |
| `branding.storage.static-ttl-days` | 30 | CDN cache TTL for STATIC |
| `branding.storage.template-ttl-days` | 7 | CDN cache TTL for TEMPLATE |
| `branding.storage.ai-generated-ttl-days` | 1 | CDN cache TTL for FULL_AI |

## Handler catalogue

| Handler | Supports | Status in Sub-PR 3.3 |
|---------|----------|----------------------|
| `StaticResourceHandler` | STATIC | ✅ ready |
| `TemplateResourceHandler` | TEMPLATE | 🏗️ scaffold (reuse existing rows; SVG compose deferred) |
| `AIResourceHandler` | FULL_AI | 🏗️ scaffold (calls ResilientAIClient; enqueue via queue in 3.5) |
| `FallbackHandler` | — (terminal rescue) | ✅ ready |

## Log
- 2026-04-14 — Initial rules (Wave 3 Sub-PR 3.3)
