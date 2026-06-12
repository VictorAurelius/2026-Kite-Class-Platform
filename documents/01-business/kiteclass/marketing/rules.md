# Marketing — Business Rules

**Domain:** KiteClass Core (tenant marketing surfaces — landing, contact, leads) + cross-link tới canonical KH marketing rules cho PDPL consent
**Version:** 1.1 (2026-05-06: cross-link BR-PDPL-CONSENT-* tới canonical KH file; existing BR-MKT-001..024 unchanged)
**Updated:** 2026-05-06
**Last verified:** 2026-05-06

---

## 0. PDPL Consent Banner Rules — see canonical KH file

`BR-PDPL-CONSENT-001` cho tới `BR-PDPL-CONSENT-004` (Cookie consent banner mandatory + granular toggles + 36-month consent retention + revocation flow) là **cross-product canonical** ở [`documents/01-business/kitehub/marketing/rules.md`](../../kitehub/marketing/rules.md). Cả KH platform marketing surfaces lẫn KC tenant public marketing surfaces apply những rules đó (cùng PDPL 2023 + Decree 13/2023/NĐ-CP legal mandate; cùng `<ConsentBanner>` shared component ở `packages/shared-ui/`).

KC tenant-specific marketing rules (contact / lead / landing customization) vẫn duy trì trong file này (`BR-MKT-001..024` bên dưới) vì những rule này scope tenant-bound (per-tenant landing config, per-tenant lead pipeline) không apply cho KH platform marketing.

KC public marketing routes apply BR-PDPL-CONSENT-*: `/`, `/catalog`, `/about`, `/contact` (per `kite.consent.banner.public-routes` in canonical file). Banner mounts tại `kiteclass-frontend/src/app/(public)/layout.tsx` (Bucket BC owns wiring).

---

## 1. Rules

### Contact Message Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-MKT-001 | Required fields | name (max 200), message (max 2000) bắt buộc; email OPTIONAL (valid format khi có, max 255 — GAP-1221 phụ huynh VN quen để SĐT), subject OPTIONAL (max 200, server default "Liên hệ từ {name}"), phone (max 20) |
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

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Compliant** — Luật Quảng cáo 2012 (advertising claims); Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 (consumer-protection on pricing display); PDPL 2023 (marketing email consent).
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Advertising law amendment, Consumer Protection Law revision, marketing-claim audit.

## Log

- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
