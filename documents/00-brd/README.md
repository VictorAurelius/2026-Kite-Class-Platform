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

## Current Gaps (Planned)

Thiếu 5 core BRD documents:

| File (planned) | Owner | Priority | Tracked in |
|----------------|-------|:--------:|------------|
| `business-objectives.md` | PM | 🟡 P2 | — |
| `compliance-scope.md` | Legal + PM | 🟠 P1 | GAP-049 |
| `pricing-model.md` | PM + Finance | 🟠 P1 | Wave 6 dependency |
| `nfr-catalog.md` | Architect + PM | 🟡 P2 | — |
| `go-to-market.md` | PM | 🟢 P3 | — |

Thứ tự ưu tiên khi build: compliance → pricing → business-objectives → NFR → GTM.

**Why low overall priority:** Engineering MVP runs without formal BRD (placeholder rules). Real driver là legal engagement (Wave 0 stakeholder sync blocker) + paying customer #1.

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
