# Marketing — Business Rules

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-03-24

---

## 1. Rules

### Contact Message Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-MKT-001 | Required fields | name (max 200), email (valid format, max 255), subject (max 200), message (max 2000) |
| BR-MKT-002 | Phone optional | Max 20 characters, no format enforcement |
| BR-MKT-003 | Email notification | Creating a contact message triggers email notification to tenant teacher/admin |
| BR-MKT-004 | Soft delete only | `deleted` flag, never hard delete |
| BR-MKT-005 | Public endpoint | Contact form submission requires no authentication, only `X-Tenant-Id` header |
| BR-MKT-006 | Read tracking | Messages track `readBy` (username/email) and `readAt` timestamp |

### Lead Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-MKT-010 | Email unique per tenant | Duplicate lead email within same tenant is rejected |
| BR-MKT-011 | Required fields | email (valid format, max 255), name (max 200) |
| BR-MKT-012 | Default status NEW | New leads start with status `NEW` |
| BR-MKT-013 | Status lifecycle | NEW -> CONTACTED -> QUALIFIED -> CONVERTED or LOST; any state -> INVALID |
| BR-MKT-014 | Lead source tracking | Source enum: LANDING_PAGE, CONTACT_FORM, TRIAL_SIGNUP, REFERRAL, SOCIAL_MEDIA, OTHER |
| BR-MKT-015 | Course interest link | Optional `courseInterestId` links lead to a specific course |
| BR-MKT-016 | Soft delete only | `deleted` flag, never hard delete |
| BR-MKT-017 | Public endpoint | Lead creation requires no authentication, only `X-Tenant-Id` header |

**Lead statuses:** NEW, CONTACTED, QUALIFIED, CONVERTED, LOST, INVALID

### Landing Page Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-MKT-020 | One per tenant | Each tenant has exactly one landing page record |
| BR-MKT-021 | Color format | `primaryColor` and `secondaryColor` must match `^#[0-9A-Fa-f]{6}$` |
| BR-MKT-022 | Partial update | All fields optional; only non-null fields are updated (PATCH semantics via PUT) |
| BR-MKT-023 | Public read | GET landing page requires no authentication |
| BR-MKT-024 | Size constraints | heroTitle max 200, heroSubtitle max 500, heroImageUrl max 500, logoUrl max 500, tagline max 200, social URLs max 255 each |

---

## 2. Config Keys

| Key | Default | Description |
|-----|---------|-------------|
| `marketing.contact.message.max-length` | 2000 | Max contact message body length |
| `marketing.lead.sources` | 6 values | Allowed LeadSource enum values |
| `marketing.landing.color.pattern` | `^#[0-9A-Fa-f]{6}$` | Hex color validation regex |
