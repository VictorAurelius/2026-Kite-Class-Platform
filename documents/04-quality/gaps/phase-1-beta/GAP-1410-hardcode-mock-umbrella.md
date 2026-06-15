# GAP-1410: Umbrella — Hardcode + Mock-in-production state-check (FE+BE)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-15 (state-check audit, 2 Opus agents — dev directive "hardcode GAP lớn FE+BE, phân biệt rõ mock vs hardcode")
**Affects:** Cross-cutting — FE (kitehub-frontend + kiteclass-frontend) + BE (kitehub-* + kiteclass-core)

## Problem

State-check (audit `documents/04-quality/audits/2026-06-15-hardcode-mock-state-check.md`) mapped two DISTINCT classes (per dev directive tách rõ):

**MOCK-in-production** (fix = wire real source): ~36 FE pages render fixture data → real beta users see fake data. Teacher/parent/student portals = P1 functional (parent billing fabricated = trust risk). → child GAP-1411 (parent), GAP-1412 (student); teacher = GAP-268 (re-rate P2→P1).

**HARDCODE** (fix = extract config/i18n/env): discipline good overall (298 MessageSource + @Value), but functional clusters: EmailServiceClient domain inconsistency (.com/.me/.vn) → GAP-1414; grade/invoice business constants → GAP-1415 + GAP-108; enum VN labels → GAP-965; nil-UUID tenant resolvers (P0 multi-tenant) → GAP-1413.

## Proposed Fix

NOT fix-toàn-bộ-now (enormous + mid flow-verification-campaign). Systematic fix wave(s) post-campaign per priority map; P0 (GAP-1413 nil-UUID) sooner. This umbrella = anchor + consolidation; child gaps track concrete clusters.

## Acceptance Criteria

- [ ] Child gaps GAP-1411/1412/1413/1414/1415 filed + triaged
- [ ] Existing gaps cross-referenced: GAP-268 (re-rate), GAP-108, GAP-1001, GAP-965, GAP-140, GAP-692
- [ ] Fix sequencing decided (post-campaign wave + P0 exception)
- [ ] Meta candidate evaluated: rule "no-mock-in-production-render" + `design-source-parity` mock distinction

## Related

- Audit: `documents/04-quality/audits/2026-06-15-hardcode-mock-state-check.md`
- Children: GAP-1411, GAP-1412, GAP-1413 (P0), GAP-1414, GAP-1415
- Consolidate: GAP-268/268a, GAP-108, GAP-1001, GAP-965, GAP-140, GAP-692
- Rules: `design-source-implementation-parity.md`, `markdown-variable-reference.md` (GAP-692), `vn-localization-audit-checklist.md`
