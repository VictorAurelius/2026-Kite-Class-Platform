---
id: GAP-686
phase: phase-1-beta
status: OPEN
priority: P1
domain: Documentation
audience: dev
---

# GAP-686: kitehub-branding 3-layer business doc sync — RBAC + @PreAuthorize annotations

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Documentation — Business-logic 3-layer sync
**Found:** 2026-05-19 (Wave 101 closure post-merge hook)
**Affects:** `documents/01-business/kitehub/branding/{rules,use-cases,api-contract}.md` (if exist) OR scaffold if missing
**Deadline:** 2026-05-26 (within 1 week of code change per CLAUDE.md §Living Documents — "Doc và code PHẢI cùng PR")

## Problem

Wave 101 Bucket B added `@PreAuthorize` to 4 kitehub-branding controllers (AIBrandingController, BrandingJobController, BrandingWizardController, BrandingJobV1Controller) + RbacAccessDeniedHandler + spring-boot-starter-security dependency. Per CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure", code + doc PHẢI cùng PR. Code shipped without 3-layer doc sync.

Required updates in `documents/01-business/kitehub/branding/`:

1. **rules.md** — add BR rules covering Owner-only branding endpoints (write operations) vs multi-role read access. Reference role canonical: OWNER (canonical) + legacy aliases PLATFORM_ADMIN/ADMIN.
2. **use-cases.md** — UC matrix: who can access which endpoint; 403 surface behavior; audit log trigger.
3. **api-contract.md** — per-endpoint role/permission column showing OWNER-only WRITE vs OWNER+STAFF READ; document 403 response envelope + audit log row schema.

Per post-merge hook flag "Business logic changed but no 01-business/ docs updated".

## Acceptance Criteria

- [ ] Verify if `documents/01-business/kitehub/branding/` folder exists; if not, scaffold per 3-layer structure (rules.md + use-cases.md + api-contract.md per `docs-folder-structure.md`).
- [ ] rules.md documents OWNER vs STAFF role split for branding scope.
- [ ] use-cases.md UC table covers OWNER access success path + STAFF 403 path per endpoint.
- [ ] api-contract.md per-endpoint table includes `required_role` column.
- [ ] Cross-reference verification chain per CLAUDE.md: `BR-xxx → UC-xxx → endpoint → @PreAuthorize → @Test`.
- [ ] Update CSV row `last_verified` post-completion.

## Related

- Wave 101 plan: `documents/03-planning/waves/wave-2026-05-19-101-product-demo-blockers.md`
- Bucket B PR #1607 (merged commit 536ff075)
- Rule: CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure"
- Rule: `.claude/rules/contract-first-for-cross-layer.md` §3 (api-contract.md MUST exist before FE consume)
- Code: `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/config/SecurityConfig.java` + 4 controllers
- Sister gap: GAP-685 (Wave 101 audit suite — api-contract-audit will surface this drift independently)
