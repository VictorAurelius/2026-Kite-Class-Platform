# Tenant Settings & Branding Business Logic

> Last verified: 2026-03-24 | Source: `kiteclass-core/module/settings/`

## 1. Rules

| ID | Rule | Detail | Config Key |
|----|------|--------|-----------|
| BR-01 | One branding per tenant | Each instanceId has max 1 Branding record | `instance_id` unique |
| BR-02 | Default branding auto-created | If no branding exists, return defaults (not persisted until update) | — |
| BR-03 | Default display name | "KiteClass" | hardcoded |
| BR-04 | Default tagline | "Nen tang quan ly trung tam dao tao" | hardcoded |
| BR-05 | Default primary color | `#3B82F6` (blue) | hardcoded |
| BR-06 | Default secondary color | `#8B5CF6` (purple) | hardcoded |
| BR-07 | Default accent color | `#10B981` (green) | hardcoded |
| BR-08 | Color format validation | Must be `#RRGGBB` hex format (regex: `^#[0-9A-Fa-f]{6}$`) | — |
| BR-09 | Display name required | NotBlank, max 200 chars | — |
| BR-10 | Tagline optional | Max 500 chars | — |
| BR-11 | Logo upload | Upload to S3, store URL in `logo_url` (max 500 chars) | — |
| BR-12 | Favicon upload | Upload to S3, store URL in `favicon_url` (max 500 chars) | — |
| BR-13 | Theme config JSON | AI-generated complete theme (colors, typography, spacing, layout) | `theme_config_json` TEXT |
| BR-14 | Update semantics | PATCH — only provided fields are updated | — |
| BR-15 | Contact email | Valid email format, max 255 chars | — |
| BR-16 | Contact phone | Max 20 chars | — |
| BR-17 | Social links | Facebook, Zalo, Website — max 500 chars each | — |
| BR-18 | Soft delete | Branding uses `deleted` flag via BaseEntity | — |

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
