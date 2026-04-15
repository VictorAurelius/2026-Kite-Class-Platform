# Quality Gate — Business Rules

**Domain:** quality-gate
**Source:** GAP-012, Wave 4 Sub-PR 4.5, ai-branding-guidelines.md §5

## Rules

| ID | Rule |
|----|------|
| BR-QG-001 | Quality report score ∈ [0, 100] (DB CHECK constraint) |
| BR-QG-002 | `passed = score ≥ pass-threshold` (default 70, config `quality-gate.pass-threshold`) |
| BR-QG-003 | 5 reference checks applied as Strategy: contrast, css-vars, asset-urls, visual-regression, logo-placement |
| BR-QG-004 | Overall score = arithmetic mean of per-check scores (all equal weight) |
| BR-QG-005 | Per-check columns materialised for admin dashboards (no JSON parsing on hot path) |
| BR-QG-006 | Every review (pass OR fail) writes AuditLog `quality.review.{passed,failed}` |
| BR-QG-007 | Pipeline step `QualityReviewStep` blocks DEPLOY when `passed=false` → throws StepException → saga compensation marks instance FAILED |
| BR-QG-008 | Reports are append-only — re-review produces a NEW row, never updates an existing one |

## 5 reference checks

| Name | Scope | Current status |
|------|-------|----------------|
| `wcag-contrast` | WCAG AA ≥ 4.5:1 | 🏗️ scaffold (deterministic score until theme-color wiring) |
| `css-vars-applied` | Theme variables non-default | 🏗️ scaffold (proxy: frontendUrl present) |
| `asset-urls-reachable` | No broken storage_url | ✅ working (counts null/blank URLs) |
| `visual-regression` | ≤ 20% diff vs baseline | 🏗️ scaffold (constant 85 until screenshot service lands) |
| `logo-placement` | Logo exists + has URL | ✅ working |

Full implementations (contrast calc, screenshot diff, URL HEAD pings) deferred to follow-up Sub-PR that wires supporting services.

## Config keys

| Key | Default | Purpose |
|-----|---------|---------|
| `quality-gate.pass-threshold` | 70 | Minimum aggregate score for passed=true |

## Integration into pipeline

Planner inserts `QualityReviewStep` between `PickTemplateStep` and `PublishPackageStep`.
On failure, the saga catches StepException and transitions the instance to FAILED
(existing `TenantProvisioningSaga` compensation path).

## Log
- 2026-04-14 — Initial rules (Wave 4 Sub-PR 4.5)
