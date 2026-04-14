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

| Key | Default | Purpose |
|-----|---------|---------|
| `branding.routing.template-first` | true | Enforce template-first philosophy |
| `branding.routing.max-ai-ratio` | 0.20 | Metric alert threshold for FULL_AI share |

## Supported ResourceTypes

`LOGO, FAVICON, BANNER, HERO, COURSE_THUMBNAIL, SOCIAL_COVER, EMAIL_HEADER`

## Log
- 2026-04-14 — Initial rules (GAP-007, ADR-005)
