# Marketing — KiteHub Platform

**Domain:** KiteHub public marketing surfaces (landing, blog, pricing, about, contact)
**Created:** 2026-05-06 (Wave 23 Bucket A — GAP-353 PDPL consent banner)

---

## Files

| File | Purpose | Status |
|------|---------|--------|
| [`rules.md`](./rules.md) | Layer 1 — Business Rules (PDPL consent + 5-attribute mandate) | ✅ shipped (Wave 23 Bucket A, 2026-05-06) |
| `use-cases.md` | Layer 2 — Use Cases (consent banner UX flow, revocation flow) | ⏳ deferred → GAP-353b/c (Phase 2 follow-up) |
| `api-contract.md` | Layer 3 — API Contract (server-side consent record API) | ⏳ deferred → GAP-353b (Phase 2 follow-up) |

Per CLAUDE.md §Business Logic Documents 3-Layer pattern: 3 files per domain expected. Wave 23 ships rules.md only (Phase 1 of GAP-353 PDPL consent); use-cases.md + api-contract.md deferred to GAP-353b/c follow-up gaps. Pre-commit hook may warn on missing trio — intentional partial.

---

## Cross-product canonical

`rules.md` BR-PDPL-CONSENT-001..004 là **canonical cross-product** — cả KiteHub marketing surfaces lẫn KiteClass tenant marketing surfaces apply. KC marketing rules at [`../../kiteclass/marketing/rules.md`](../../kiteclass/marketing/rules.md) cross-links tới file này cho consent rules; KC-specific rules (BR-MKT-001..024 cho contact / lead / landing) vẫn ở KC marketing rules.md.

---

## Related

- [GAP-353](../../../04-quality/gaps/GAP-353-pdpl-cookie-consent-banner-marketing-kits.md) — PDPL Cookie Consent Banner umbrella gap
- [GAP-182](../../../04-quality/gaps/GAP-182-privacy-policy.md) — Privacy Policy (Phase 2 legal counsel review queued)
- [GAP-156](../../../04-quality/gaps/GAP-156-quarterly-business-correctness-audit.md) — Quarterly business-correctness audit (formal sign-off path)
- `.claude/rules/business-logic-review.md` v1.0.0 — 5-attribute mandate
- PDPL 2023 + Decree 13/2023/NĐ-CP (effective 2026-07-01)
