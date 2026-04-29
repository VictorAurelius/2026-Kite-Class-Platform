# 00-brd — Business Requirements Document

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../.claude/rules/docs-folder-structure.md)

Enterprise-standard Business Requirements Documentation. Chứa nghiệp vụ nền tảng trước khi implementation: personas, business objectives, compliance scope, pricing strategy, NFRs, go-to-market plan. Nguồn cho `01-business/` (rules chi tiết per domain) và `03-planning/` (roadmap).

**Audience:** PM, Business Lead, Legal, Stakeholders. Secondary: Architects, Tech Lead để align technical decisions với business intent.

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| [`personas-catalog.md`](personas-catalog.md) | Canonical list of 10 target tenant personas (DRAFT v1, 2026-04-14) | 1 |
| [`trial-to-paid-conversion.md`](trial-to-paid-conversion.md) | Trial flow + conversion funnel | 1 |
| [`business-objectives.md`](business-objectives.md) | OKRs, success metrics, north-star KPIs (skeleton) | 1 |
| [`compliance-scope.md`](compliance-scope.md) | VN PDPL, MoET, Cybersecurity, Labor, Consumer, Tax mapping (skeleton) | 1 |
| [`pricing-model.md`](pricing-model.md) | Free/Pro/Premium/Enterprise tier definition + AI metering (skeleton) | 1 |
| [`nfr-catalog.md`](nfr-catalog.md) | Uptime SLA, RTO/RPO, performance budgets, accessibility (skeleton) | 1 |
| [`go-to-market.md`](go-to-market.md) | Target persona priority, pilot strategy, sales funnel (skeleton) | 1 |
| [`terms-of-service.md`](terms-of-service.md) | TOS legal contract — 15 sections (skeleton, Phase 1) | 1 |
| [`acceptable-use-policy.md`](acceptable-use-policy.md) | AUP — prohibited content/conduct + enforcement (skeleton) | 1 |
| [`privacy-policy.md`](privacy-policy.md) | Privacy Policy — VN PDPL Decree 13/2023 + GDPR (skeleton) | 1 |
| [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) | Retention matrix + deletion process — VN PDPL Art 6 (skeleton) | 1 |
| [`refund-dispute-resolution-policy.md`](refund-dispute-resolution-policy.md) | Refund eligibility + dispute resolution — VN Consumer Protection Law 2023 (skeleton) | 1 |
| [`billing-terms.md`](billing-terms.md) | Payment terms + VAT/TCT e-invoice compliance — Circular 78/2021/TT-BTC (skeleton) | 1 |
| [`child-protection-policy.md`](child-protection-policy.md) | K-12 minor protection + parental consent + safeguarding — Law on Children 2016 + PDPL Art 16 (skeleton) | 1 |

---

## File Placement Rules

- ✅ **Belongs here:**
  - Personas catalog (who uses the product)
  - Business objectives + OKRs + success metrics
  - Compliance scope (VN PDPL, MoET circulars, labor law)
  - Pricing model (Free/Pro/Enterprise, AI metering)
  - Non-functional requirements catalog (SLA, uptime, RTO/RPO)
  - Go-to-market plan (target schools, pilot strategy)

- ❌ **Does NOT belong here:**
  - Per-domain business rules → [`documents/01-business/`](../01-business/) (3-layer: rules.md + use-cases.md + api-contract.md)
  - Technical architecture → [`documents/02-architecture/`](../02-architecture/)
  - Roadmap / waves → [`documents/03-planning/`](../03-planning/)

- Naming: `kebab-case.md`, mỗi concern = 1 file riêng (không gộp "business-everything.md")

---

## Current Gaps (Skeleton — Phase 1 shipped 2026-04-29)

Phase 1 ship 5 BRD skeleton files (frame + section structure + TODO markers). Content fill (Phase 2) requires stakeholder engagement — tracked separately.

| File | Owner | Status | Tracking |
|------|-------|:------:|----------|
| [`business-objectives.md`](business-objectives.md) | PM | skeleton | [GAP-150](../04-quality/gaps/GAP-150-brd-docs-completion.md) (Phase 1) → GAP-155 (Phase 2 content) |
| [`compliance-scope.md`](compliance-scope.md) | Legal + PM | skeleton | [GAP-150](../04-quality/gaps/GAP-150-brd-docs-completion.md) → GAP-155 |
| [`pricing-model.md`](pricing-model.md) | PM + Finance | skeleton | [GAP-150](../04-quality/gaps/GAP-150-brd-docs-completion.md) → GAP-155 |
| [`nfr-catalog.md`](nfr-catalog.md) | Architect + PM | skeleton | [GAP-150](../04-quality/gaps/GAP-150-brd-docs-completion.md) → GAP-155 |
| [`go-to-market.md`](go-to-market.md) | PM | skeleton | [GAP-150](../04-quality/gaps/GAP-150-brd-docs-completion.md) → GAP-155 |
| [`terms-of-service.md`](terms-of-service.md) | Legal + PM/CEO | skeleton | [GAP-180](../04-quality/gaps/GAP-180-terms-of-service.md) (Phase 1, Wave Legal-BRD 2026-04-29) → GAP-154 (Phase 2 content) |
| [`acceptable-use-policy.md`](acceptable-use-policy.md) | Legal + Trust & Safety | skeleton | [GAP-181](../04-quality/gaps/GAP-181-acceptable-use-policy.md) (Phase 1) → GAP-154 |
| [`privacy-policy.md`](privacy-policy.md) | Legal + DPO | skeleton | [GAP-182](../04-quality/gaps/GAP-182-privacy-policy.md) (Phase 1, **VN PDPL Decree 13/2023 mandate**) → GAP-154 |
| [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) | Legal + Engineering Lead | skeleton | [GAP-184](../04-quality/gaps/GAP-184-data-retention-deletion-policy.md) (Phase 1, **VN PDPL Art 6 mandate**) → GAP-154 |
| [`refund-dispute-resolution-policy.md`](refund-dispute-resolution-policy.md) | Legal + Finance + Support Lead | skeleton | [GAP-183](../04-quality/gaps/GAP-183-refund-dispute-resolution-policy.md) (Phase 1, Wave Legal-BRD 1.5 2026-04-29, **VN Consumer Protection Law 2023 mandate**) → GAP-154 |
| [`billing-terms.md`](billing-terms.md) | Legal + Finance + Tax advisor | skeleton | [GAP-185](../04-quality/gaps/GAP-185-billing-terms-vat-tct-compliance.md) (Phase 1, **TCT e-invoice mandate Circular 78/2021/TT-BTC**) → GAP-154 → GAP-108 |
| [`child-protection-policy.md`](child-protection-policy.md) | Legal + Trust&Safety + DPO | skeleton | [GAP-186](../04-quality/gaps/GAP-186-child-protection-policy.md) (Phase 1, **Law on Children 2016 + PDPL Art 16 mandate**) → GAP-154 |

Thứ tự ưu tiên khi fill content (Phase 2): compliance → pricing → business-objectives → NFR → GTM → privacy-policy → data-retention → child-protection → TOS → AUP → billing-terms → refund-dispute.

**BRD scope expansion:** Simulation gap-finder 2026-04-20 found 22 additional BRD docs needed — tracked by umbrella GAP-154. **Wave Legal-BRD Phase 1 + 1.5 (2026-04-29) ships 7/7 P0 BL legal mandate skeletons:** TOS / AUP / Privacy / Retention (Phase 1) + Refund-Dispute / Billing-VAT / Child-Protection (Phase 1.5). GAP-154 umbrella Phase 1 milestone complete.

**Why skeleton-first:** Engineering MVP runs without formal BRD (placeholder rules); skeleton unblocks per-domain `rules.md` traceability without blocking on stakeholder availability. Real content driver: legal engagement (Wave 0 stakeholder sync) + paying customer #1.

---

## Archive Policy

Move to `documents/07-archived/brd-YYYY/` khi:
- BRD version superseded by newer edition (add `superseded_by:` to new frontmatter)
- Persona retired (pivot away from tenant type)
- Doc >180 days stale AND no recent reference in PRs/commits

Quarterly review by Business Lead.

---

## Related

- **Gaps:** [GAP-049](../04-quality/gaps/GAP-049-business-logic-correctness-review.md) (business logic correctness), [GAP-050](../04-quality/gaps/GAP-050-persona-based-business-review.md) (persona-driven gap finder)
- **Consumer folder:** [`documents/01-business/`](../01-business/) — implements BRD rules per domain
- **Waves:** Wave 9 (Compliance MVP), Wave 0 (Stakeholder Sync) consume this folder
