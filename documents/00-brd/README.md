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

Thứ tự ưu tiên khi fill content (Phase 2): compliance → pricing → business-objectives → NFR → GTM.

**BRD scope expansion:** Simulation gap-finder 2026-04-20 found 22 additional BRD docs needed (TOS, Privacy, AUP, Refund, Data Retention, Child Protection…) — tracked by umbrella GAP-154.

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
