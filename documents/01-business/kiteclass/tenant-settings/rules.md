# Tenant Settings & Branding Business Logic

> Last verified: 2026-03-24 | Source: `kiteclass-core/module/settings/`

## 1. Rules

| ID | Rule | Detail | Config Key |
|----|------|--------|-----------|
| BR-SET-01 | One branding per tenant | Each instanceId has max 1 Branding record | `instance_id` unique |
| BR-SET-02 | Default branding auto-created | If no branding exists, return defaults (not persisted until update) | — |
| BR-SET-03 | Default display name | "KiteClass" | hardcoded |
| BR-SET-04 | Default tagline | "Nen tang quan ly trung tam dao tao" | hardcoded |
| BR-SET-05 | Default primary color | `#3B82F6` (blue) | hardcoded |
| BR-SET-06 | Default secondary color | `#8B5CF6` (purple) | hardcoded |
| BR-SET-07 | Default accent color | `#10B981` (green) | hardcoded |
| BR-SET-08 | Color format validation | Must be `#RRGGBB` hex format (regex: `^#[0-9A-Fa-f]{6}$`) | — |
| BR-SET-09 | Display name required | NotBlank, max 200 chars | — |
| BR-SET-10 | Tagline optional | Max 500 chars | — |
| BR-SET-11 | Logo upload | Upload to S3, store URL in `logo_url` (max 500 chars) | — |
| BR-SET-12 | Favicon upload | Upload to S3, store URL in `favicon_url` (max 500 chars) | — |
| BR-SET-13 | Theme config JSON | AI-generated complete theme (colors, typography, spacing, layout) | `theme_config_json` TEXT |
| BR-SET-14 | Update semantics | PATCH — only provided fields are updated | — |
| BR-SET-15 | Contact email | Valid email format, max 255 chars | — |
| BR-SET-16 | Contact phone | Max 20 chars | — |
| BR-SET-17 | Social links | Facebook, Zalo, Website — max 500 chars each | — |
| BR-SET-18 | Soft delete | Branding uses `deleted` flag via BaseEntity | — |

## 2. Flow

### Get Branding
```
Request GET /api/branding
  → Resolve instanceId from TenantContext
  → Find branding by instanceId (deleted=false)
  → If not found → return default branding (not persisted)
  → Return BrandingResponse
```

### Update Branding
```
Request PUT /api/branding
  → Validate request (colors, display name, etc.)
  → Find existing branding by instanceId
  → If not found → create new with defaults + request fields
  → If found → PATCH update from request (BrandingMapper)
  → Save → Return BrandingResponse
```

### Upload Logo/Favicon
```
Request POST /api/branding/logo (or /favicon)
  → Receive presigned S3 URL or file path
  → Find or create branding for tenant
  → Set logoUrl/faviconUrl
  → Save → Return BrandingResponse
```

### Get Theme Config
```
Request GET /api/branding/theme
  → Find branding by instanceId
  → Return themeConfigJson (or null if not set)
```

## 3. Emails

Khong co email trigger trong module settings.

## 4. Config

```yaml
# Branding defaults (hardcoded in BrandingServiceImpl)
# No external config keys — values set in createDefaultBranding()
branding:
  defaults:
    display-name: "KiteClass"
    tagline: "Nen tang quan ly trung tam dao tao"
    primary-color: "#3B82F6"
    secondary-color: "#8B5CF6"
    accent-color: "#10B981"

# S3 storage for logo/favicon
aws:
  s3:
    bucket: ${S3_BUCKET}
    region: ${AWS_REGION}
    access-key: ${AWS_ACCESS_KEY}
    secret-key: ${AWS_SECRET_KEY}
```

### Branding Response Fields
| Field | Type | Required | Constraint |
|-------|------|----------|-----------|
| displayName | String | Yes | max 200 |
| tagline | String | No | max 500 |
| primaryColor | String | Yes | hex #RRGGBB |
| secondaryColor | String | Yes | hex #RRGGBB |
| accentColor | String | Yes | hex #RRGGBB |
| logoUrl | String | No | max 500 |
| faviconUrl | String | No | max 500 |
| themeConfigJson | String | No | TEXT |
| contactEmail | String | No | valid email, max 255 |
| contactPhone | String | No | max 20 |
| address | String | No | TEXT |
| facebookUrl | String | No | max 500 |
| zaloUrl | String | No | max 500 |
| websiteUrl | String | No | max 500 |

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **N/A** — internal tenant configuration values; no PII beyond what `tenant-provisioning` already covers.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: New setting category added, tenant-settings UI redesign.

## Log

- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
