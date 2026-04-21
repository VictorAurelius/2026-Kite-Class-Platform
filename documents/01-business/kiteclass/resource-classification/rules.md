# Resource Classification — Business Rules

**Domain:** resource-classification
**Source:** GAP-007, ADR-005

## Rules

### ResourceCategory (STATIC / TEMPLATE / FULL_AI)
| ID | Rule |
|----|------|
| BR-RES-001 | A branding resource belongs to exactly one category per slot |
| BR-RES-002 | TEMPLATE resource MUST set templateId (enforced in entity + DB CHECK) |
| BR-RES-003 | FULL_AI resource MUST set aiJobId (enforced in entity + DB CHECK) |
| BR-RES-004 | STATIC resource MUST NOT set templateId or aiJobId |
| BR-RES-005 | Template-first routing: ≥80% of requests should resolve to STATIC or TEMPLATE |

### Classification chain order
| ID | Rule |
|----|------|
| BR-CLS-001 | StaticAssetClassifier runs first (user upload always wins) |
| BR-CLS-002 | CustomAIRequest only honored when AI quota present |
| BR-CLS-003 | TemplateMatchClassifier runs before AIFallback (template-first) |
| BR-CLS-004 | DefaultTemplateClassifier is terminal; chain always resolves |
| BR-CLS-005 | Classifiers MUST be pure (no DB/network calls during classify) |

## Config keys

| Key | Default | Purpose | Code reference |
|-----|---------|---------|----------------|
| `branding.routing.template-first` | true | Enforce template-first philosophy (BR-RES-005). Set false only for debug/load tests. | `BrandingRoutingProperties.templateFirst`, `ResourceRoutingService#logRoutingConfig` |
| `branding.routing.max-ai-ratio` | 0.20 | Alert threshold for the `branding.routing.ai_ratio` gauge (FULL_AI share). Prometheus alert fires when exceeded. | `BrandingRoutingProperties.maxAiRatio`, `branding.routing.classified` counter emitted by `ResourceRoutingService#recordClassification` |

Env-var overrides: `BRANDING_TEMPLATE_FIRST`, `BRANDING_MAX_AI_RATIO`.

## Metrics

| Metric | Type | Tags | Source |
|--------|------|------|--------|
| `branding.routing.classified` | Counter | `category` (static / template / full_ai) | `ResourceRoutingService#recordClassification` — one increment per classify() invocation |

Grafana computes `branding.routing.ai_ratio = classified{category=full_ai} / sum(classified)` and alerts when it exceeds `branding.routing.max-ai-ratio`.

## Supported ResourceTypes

`LOGO, FAVICON, BANNER, HERO, COURSE_THUMBNAIL, SOCIAL_COVER, EMAIL_HEADER`

## Log
- 2026-04-21 — GAP-106: externalized `branding.routing.*` keys. Added `BrandingRoutingProperties` (kiteclass-core/module/branding/config), wired `ResourceRoutingService` to emit `branding.routing.classified` counter per classify(), added keys to `kiteclass-core/src/main/resources/application.yml`. Startup log warns when `template-first=false`.
- 2026-04-14 — Initial rules (GAP-007, ADR-005)
