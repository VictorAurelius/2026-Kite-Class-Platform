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

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **N/A** — internal asset classification (STATIC/TEMPLATE/FULL_AI); no PII surface, no compliance trigger.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Classification taxonomy change (e.g., Enterprise tier opt-in flow).

## Log
- 2026-04-21 — GAP-106: externalized `branding.routing.*` keys. Added `BrandingRoutingProperties` (kiteclass-core/module/branding/config), wired `ResourceRoutingService` to emit `branding.routing.classified` counter per classify(), added keys to `kiteclass-core/src/main/resources/application.yml`. Startup log warns when `template-first=false`.
- 2026-04-14 — Initial rules (GAP-007, ADR-005)
