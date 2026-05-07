# Beta Access — Domain Index

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../../.claude/rules/docs-folder-structure.md) + CLAUDE.md §"Business Logic Documents — 3-Layer Structure"

Domain governing the beta tenant invite mechanism (Phase 1 BETA launch). Owner-facing public submit + coordinator listing/approve/reject flows. Created Wave 33 (GAP-372); extended Wave 35 with PDPL 2023 explicit consent (GAP-385) + admin role guard (GAP-384) + Micrometer counters (GAP-387).

---

## Files

| File | Purpose |
|------|---------|
| `README.md` | This index |
| `rules.md` | Layer 1 — business rules (BR-BETA-001..003) with 5-attribute compliance per `business-logic-review.md` |
| `use-cases.md` | Layer 2 — actor-driven flows (UC-BETA-001 PDPL submit; UC-BETA-002..006 referenced) |
| `api-contract.md` | Layer 3 — endpoint schemas, request/response, error codes, status enum |

---

## Cross-layer scope (Wave 35)

This domain is the cross-layer source-of-truth (per `.claude/rules/contract-first-for-cross-layer.md`) for Wave 35 GAP-385. BE Bucket B + FE Bucket B + the MSW handler (`kitehub-frontend/src/test/msw/handlers/beta-access.ts`) all read these three documents to align on the consent-field contract.

## Related

- Source-of-truth controller: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/controller/BetaAccessController.java`
- MSW handler: `kitehub/kitehub-frontend/src/test/msw/handlers/beta-access.ts`
- Wave plan: `documents/03-planning/waves/wave-2026-05-08-35-audit-p0-blockers-sprint.md`
