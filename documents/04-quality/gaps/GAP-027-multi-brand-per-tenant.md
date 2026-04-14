# GAP-027: Multi-Brand per Tenant (Franchise / Multi-Branch Support)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Product / Backend
**Detected:** 2026-04-14 (simulation)

## Problem

Hiện tại design giả định **1 tenant = 1 brand**. Nhưng real-world:

- Enterprise tenant có nhiều trung tâm (franchise)
- Mỗi chi nhánh cần branding riêng (địa phương hóa)
- Parent brand + child brand relationship
- Tenant đổi địa điểm seasonal (summer camp, test prep)

Hiện tại force tenant tạo multiple accounts → ugly workaround, lose unified billing.

## Proposed Fix

### 1. Brand Variant Entity

```java
@Entity
public class BrandVariant {
  Long id;
  String tenantId;
  String variantName;       // "Chi nhánh Hà Nội", "Chi nhánh HCM"
  String subdomain;          // hn.abc-center.kiteclass.com
  Boolean isPrimary;         // main brand
  ThemeConfig theme;
  Map<String, String> assetUrls;
  InstanceStatus status;
  Timestamp createdAt;
}
```

### 2. Tier Entitlements

| Tier | Max Brand Variants |
|------|-------------------|
| FREE | 1 |
| PRO | 3 |
| PREMIUM | 10 |
| ENTERPRISE | Unlimited |

### 3. Management UI

```
Tenant Dashboard
├── Brands (list)
│   ├── Hà Nội (Primary) [DEPLOYED]
│   ├── HCM [DEPLOYED]
│   └── Đà Nẵng [GENERATING]
│   [+ Add brand variant]
├── Each brand:
│   - Edit branding (runs wizard)
│   - View analytics per brand
│   - Manage classes/students scoped to this brand
└── Billing: 1 subscription covers all variants
```

### 4. Routing

- Primary brand: `{tenantSlug}.kiteclass.com`
- Variants: `{variantSlug}.{tenantSlug}.kiteclass.com` or `{tenantSlug}.kiteclass.com/{variantSlug}`

### 5. Data Scoping

Classes, students, teachers:
- Option A: Shared across variants (parent-level)
- Option B: Scoped to variant (child-level)
- Option C: Hybrid — some shared (teachers), some scoped (classes)

**Recommend Option C** với UI controls.

### 6. Unified Branding Inheritance

```
Parent brand (tenant level)
  ↓ inherits
Variant brand (can override)
```

- New variant starts with parent branding as baseline
- Override specific elements (logo, colors) per variant
- Parent update → cascade to non-overridden variants (opt-in)

## Acceptance Criteria

- [ ] `BrandVariant` entity + DB schema
- [ ] Tier entitlement check on create variant
- [ ] Management UI với list + create + edit
- [ ] Subdomain routing (single wildcard cert)
- [ ] Shared vs scoped data decision implemented
- [ ] Inheritance model: parent brand → variant brand
- [ ] Integration test: Enterprise tenant creates 5 variants, each with independent branding

## Dependencies

- GAP-009 (lifecycle) — multi-instance lifecycle
- GAP-010 (package API) — variant-aware package endpoint
- Gateway routing (DNS wildcard)

## Log

- 2026-04-14 — Enterprise use case surfaced qua simulation
