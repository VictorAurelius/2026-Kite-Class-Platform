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
- **2026-06-06** Bổ sung §5 Per-tenant configuration (TenantSettings entity — timezone/locale/Năm học) per GAP-947 (Wave provisioning-1 Bucket F). Concern mới, tách khỏi Branding (BR-SET-*) ở trên.

---

## 5. Per-tenant configuration — TenantSettings (GAP-947)

> Last verified: 2026-06-06 | Source: GAP-947, Wave provisioning-1 Bucket F | Code: `kiteclass-core/module/tenantsettings/`

Cấu hình cấp tenant (trường học) — TÁCH BIỆT với Branding (BR-SET-* ở trên). Một bản ghi `TenantSettings` cho mỗi instance (1:1). Trước đây các giá trị này nằm rải rác (`Instance.organizationName/contactEmail`) hoặc hard-code global (`system_config.locale`).

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| BR-TSET-001 | Mỗi tenant có đúng 1 bản ghi TenantSettings (1:1 instance) | 1 | unique index `uk_tenant_settings_instance_id` |
| BR-TSET-002 | Timezone mặc định | `Asia/Ho_Chi_Minh` | `TenantSettings.DEFAULT_TIMEZONE` |
| BR-TSET-003 | Locale mặc định | `vi` | `TenantSettings.DEFAULT_LOCALE` |
| BR-TSET-004 | Năm học auto-fill tại provision (VN K-12 Sep→May) | Sep→May | `AcademicYearCalculator.currentAcademicYear()` |
| BR-TSET-005 | Năm học format `YYYY-YYYY` | vd `2026-2027` | regex `^\d{4}-\d{4}$` |
| BR-TSET-006 | School type mặc định | `CENTER` | enum `SchoolType` (CENTER/K12/UNIVERSITY/OTHER) |
| BR-TSET-007 | Tenant isolation — caller chỉ đọc/ghi settings tenant mình | enforced | controller guard (path id == X-Tenant-Id) + RLS `tenant_isolation` (V90) |
| BR-TSET-008 | First read auto-create default (không 404) | auto | `TenantSettingsService.getSettings()` |
| BR-TSET-009 | PUT update = provided-field-wins merge (null giữ giá trị cũ) | merge | mapper `NullValuePropertyMappingStrategy.IGNORE` |

### Năm học auto-compute (VN K-12)

Năm học chạy tháng 9 → tháng 5/6, label là khoảng 2 năm dương lịch:

| Tháng hiện tại | Công thức | Ví dụ |
|---|---|---|
| ≥ 9 (Sep-Dec) | `<year>-<year+1>` | Oct 2026 → `2026-2027` |
| < 9 (Jan-Aug) | `<year-1>-<year>` | May 2026 → `2025-2026` |

Tính theo timezone `Asia/Ho_Chi_Minh`. Implementation: `AcademicYearCalculator`.

### Fields (TenantSettings)

| Field | Type | Nullable | Default |
|---|---|---|---|
| `timezone` | String(50) | no | `Asia/Ho_Chi_Minh` |
| `locale` | String(10) | no | `vi` |
| `academicYear` | String(20) | no | auto-fill (Năm học) |
| `fiscalYear` | String(20) | yes | — |
| `schoolType` | enum | no | `CENTER` |
| `address` | String(500) | yes | — |
| `phone` | String(30) | yes | — |
| `logoUrl` | String(1000) | yes | — |
| `themeConfig` | jsonb | yes | — |

### 5-attribute review (per `business-logic-review.md` §2)

- **Source:** Benchmark B1 (MISA QLTH — Năm học required field tại provision) + recommendation C2 (KC-1 pre-walk audit 2026-06-04) + informed gut cho defaults (timezone/locale VN).
- **Rationale:** VN K-12 mọi nghiệp vụ (lịch học, học kỳ, điểm) neo theo Năm học → phải có sẵn ngay khi provision. Default `CENTER` vì Phase 1 BETA target P2 trung tâm.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-06-06). Formal review queued GAP-156.
- **Compliance check:** N/A — cấu hình nội bộ tenant; không PII vượt phạm vi `tenant-provisioning` đã cover.
- **Review cadence:** Quarterly. **Next review:** 2026-09-06. Triggers: thêm field settings mới, đổi quy ước Năm học.
