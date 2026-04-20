# GAP-154: BRD Scope Expansion (Umbrella — 22 Missing BRD Docs)

**Status:** 🔵 OPEN (umbrella)
**Priority:** 🔴 P0 (business-logic tier — 7 sub-items GA-blocking)
**Domain:** Business / BRD / Legal / Governance
**Found:** 2026-04-20 (simulation gap-finder run per user request)
**Affects:** GA legal readiness, enterprise sales, VN PDPL + Consumer Protection Law compliance, K-12 child protection
**Report:** [`documents/04-quality/audits/business/brd-simulation-gap-finder-2026-04-20.md`](../audits/business/brd-simulation-gap-finder-2026-04-20.md)

## Problem

GAP-150 scoped 5 BRD skeleton docs (business-objectives, compliance-scope, pricing-model, nfr-catalog, go-to-market). Simulation gap-finder run 2026-04-20 via `.claude/skills/quality/simulation-gap-finder.md` (3-axis matrix) surfaced **22 additional missing BRD docs** — 7 P0 legally mandatory, 7 P1 pre-scale, 5 P2 defensible scale, 3 P3 maturity.

**Net BRD scope:** 27 docs (5 in GAP-150 + 22 via this gap). Current state: 1 published (personas-catalog.md), 5 tracked (GAP-150), 22 untracked (this gap).

User's framing: *"BRD là điểm khởi đầu quan trọng nhất của dự án"* — current coverage ~4% of needed docs (1/27).

## Root Cause

1. GAP-150 scope derived from `00-brd/README.md` "Current Gaps (Planned)" section — that list was **incomplete** (written 2026-04-14, not validated against BRD best practices)
2. No prior systematic simulation → BRD gaps accumulated as implicit assumptions
3. Critical compliance docs (VN PDPL privacy, consumer protection refund policy, child protection for K-12) never surfaced as gaps despite being GA blockers

## Proposed Fix — Phased sub-gap creation

**Phase 0 (this PR):** Report + umbrella gap + phasing plan.

**Phase 1 (next session — Wave 8 scope):** File 7 P0 sub-gaps. Reserve GAP numbers in range `GAP-180..186` (to avoid collision with Wave 8b output-review gaps GAP-170..175).

| Sub-gap (reserved) | Scope |
|--------------------|-------|
| GAP-180 | B. Terms of Service (TOS) |
| GAP-181 | C. Acceptable Use Policy (AUP) |
| GAP-182 | D. Privacy Policy (VN PDPL + GDPR) |
| GAP-183 | K. Refund + Dispute Resolution Policy |
| GAP-184 | L. Data Retention + Deletion Policy |
| GAP-185 | N. Billing Terms + VAT/TCT Invoice Compliance |
| GAP-186 | Z. Child Protection Policy (K-12 minors) |

**Phase 2 (post Wave 8):** 7 P1 sub-gaps (GAP-187..193 reserved).
- A. MRD (Market Requirements Doc)
- E. Data Classification + Handling Policy
- F. Customer-facing SLA + Uptime Commitment
- H. Incident Response + Breach Notification
- M. Data Export / Portability Policy
- X. MOET Regulatory Alignment Matrix
- Y. Academic Year + Curriculum Structure Policy

**Phase 3 (post-GA):** 5 P2 sub-gaps (numbers assigned when phase starts).
- G. Support SLA / SOP
- I. Customer-facing DR / BCP
- J. API Terms of Use / Developer License
- Q. Security Posture Summary
- AA. Vendor Management / 3rd Party Risk Policy

**Phase 4 (maturity):** 3 P3 sub-gaps.
- O. Versioning + Deprecation Policy
- P. Accessibility Statement
- R. Brand Guidelines / Trademark Policy

## Acceptance Criteria

### Phase 0 (this PR)

- [x] Simulation report published (`audits/business/brd-simulation-gap-finder-2026-04-20.md`)
- [x] 22 missing docs catalogued with priority + stage + category + matrix cell
- [x] Duplicate check vs existing gaps completed (7 partial overlaps documented, 15 net new)
- [x] Phased sub-gap creation plan (GAP-180..186 reserved for Phase 1)
- [x] GAP-150 cross-reference updated (5 of 27 = skeleton subset, not full BRD)
- [x] This umbrella gap created
- [x] ROADMAP updated

### Phase 1 (this session 2026-04-20 — FILED)

- [x] 7 P0 sub-gaps filed (GAP-180..186) — 2026-04-20
  - GAP-180 Terms of Service
  - GAP-181 Acceptable Use Policy
  - GAP-182 Privacy Policy (VN PDPL)
  - GAP-183 Refund + Dispute Resolution (VN Consumer Protection)
  - GAP-184 Data Retention + Deletion (VN PDPL Art 6)
  - GAP-185 Billing Terms + VAT/TCT (Circular 78/2021)
  - GAP-186 Child Protection (K-12 minors, Law on Children)
- [x] Each sub-gap follows `audit-to-gap-pipeline.md` §3 template
- [x] Sub-gaps assigned to Wave 8 Business Governance (master plan updated)
- [ ] Legal counsel engagement scheduled for drafts (operational — Phase 2)

### Phase 2/3/4 (future sessions)

- [ ] P1 sub-gaps filed + Wave assignment
- [ ] P2 sub-gaps filed + post-GA scheduling
- [ ] P3 sub-gaps filed + backlog
- [ ] Quarterly re-run of simulation-gap-finder against BRD scope (catch new gaps as personas/features expand)

## Out of Scope

- **Writing actual BRD content** — sub-gaps handle content creation (skeleton + stakeholder engagement)
- **Legal counsel engagement** — operational, not a doc deliverable
- **Redrafting GAP-150 scope** — GAP-150 stays as "Phase 1 skeleton for first 5 strategic docs" (objectives, compliance-scope, pricing, nfr, GTM) — those are different concerns than the 22 new docs
- **Filing P0 sub-gaps in this PR** — Phase 1 explicitly next session to avoid scope creep

## Dependencies

- GAP-150 (Phase 1 BRD skeleton — complementary, not replaced)
- GAP-151/153 (persona AC — consume BRD values like pricing, SLA targets)
- Legal counsel (Phase 1+ gating for content fill)
- VN PDPL 2023, Consumer Protection Law 2023, Education Law, Child Protection Law reference materials

## Related

- Report: `documents/04-quality/audits/business/brd-simulation-gap-finder-2026-04-20.md` — full simulation output
- Parent GAP-049 — business-logic correctness review (process scope)
- Skill: `.claude/skills/quality/simulation-gap-finder.md` — method used
- Rule: `.claude/rules/meta-gap-priority.md` §3 — business-logic tier justification
- Rule: `.claude/rules/audit-to-gap-pipeline.md` — gap creation process
- Existing adjacent gaps: GAP-018 (content safety — feeds AUP), GAP-042 (legal/IP), GAP-108 (billing config — feeds billing terms), GAP-135 (SLOs — feeds customer SLA), GAP-174 (marketing-legal review)

## Log

- 2026-04-20 (later session) — Phase 1 FILED. 7 P0 sub-gap files created (GAP-180..186). Wave 8 master plan updated to include these gaps. Phase 2 P1 sub-gaps (GAP-187..193) still reserved for future session.
- 2026-04-20 — Umbrella created. Simulation surfaced 22 missing BRD docs. Phase 0 artifacts shipped in this PR. Phase 1 P0 sub-gaps (GAP-180..186) reserved for next session.
