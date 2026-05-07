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

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **N/A** — internal resource routing per classification; same scope as `resource-classification`.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Handler added/removed, new resource type.

## Log
- 2026-04-14 — Initial rules (Wave 3 Sub-PR 3.3)
