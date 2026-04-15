# Branding Wizard — Business Rules

**Domain:** branding-wizard
**Source:** GAP-013 + GAP-031 + GAP-069, Wave 3 Sub-PR 3.7, ADR-006

## Rules

### Wizard flow
| ID | Rule |
|----|------|
| BR-WIZ-001 | 6 ordered steps: welcome → logo → audience → tone → template → preview |
| BR-WIZ-002 | NEXT blocked until step's required input is set (segment, audience, tone, templateId) |
| BR-WIZ-003 | BACK from first step is a no-op |
| BR-WIZ-004 | Free-form AI prompt forbidden except Enterprise tier Advanced Mode (ai-branding-guidelines §2) |
| BR-WIZ-005 | Reducer MUST stay pure — side effects only in hook/submit |

### Tier-gated inputs (GAP-031)
| Tier | Visible fields |
|------|----------------|
| FREE | segment + audience + tone + templateId (4) |
| PRO | + colorHint + typographyHint (6) |
| PREMIUM | + contentDensity + imageryStyle + ctaStyle (9) |
| ENTERPRISE | + customPrompt + brandKeywords + bannedKeywords + preferredFonts + accessibilityLevel + supportedLanguages + brandValues (16) |

### Segment gating (GAP-069)
| Segment | Template tags pre-filter |
|---------|--------------------------|
| K12 | warm palette, serif/rounded, age-appropriate imagery |
| CENTER | energetic, bright, marketing-driven |
| UNIV | muted, serif, photography-grade |
| CORP | corporate clean, minimal, sans-serif |
| OTHER | generic default templates |

### Regenerate quota
| Tier | Limit per session |
|------|:-----------------:|
| FREE | 3 |
| PRO | 10 |
| PREMIUM | 30 |
| ENTERPRISE | unlimited |

RegenerateCounter MUST be visible; quota-exhausted state surfaces disabled button + destructive color.

### State machine invariants
| ID | Rule |
|----|------|
| BR-WIZ-010 | Reducer handles 12 event types across 9 states; `never` default catches exhaustiveness regressions at compile time |
| BR-WIZ-011 | SUBMIT transitions to submitting; SUBMIT_OK → done; SUBMIT_FAIL → error |
| BR-WIZ-012 | error state allows SUBMIT (retry) or BACK to preview |
| BR-WIZ-013 | RESET returns to welcome preserving tier + instanceId |

## Config keys

| Key | Default | Purpose |
|-----|---------|---------|
| `wizard.tier` | `PRO` (dev) / real from auth session | Tier-gated rendering |
| `wizard.regenerate.limits.{tier}` | FREE=3, PRO=10, PREMIUM=30, ENTERPRISE=∞ | Quota |

## Log
- 2026-04-14 — Initial rules (Wave 3 Sub-PR 3.7)
