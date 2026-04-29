# GAP-156: Business Rules Compliance Audit + Stakeholder Sign-offs

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (Business-Logic tier per `.claude/rules/meta-gap-priority.md` §3)
**Domain:** Product / Business / Compliance / Governance
**Detected:** 2026-04-29 (filed as Phase 2 of GAP-049 split)
**Parent:** GAP-049 (Phase 1 = `.claude/rules/business-logic-review.md` rule + matrix-row flip; this gap = Phase 2 = audit execution against existing rules.md + stakeholder sign-offs)
**Related PRs:** TBD (Wave Business Correctness Phase 1 — this PR by Agent B)
**Related Docs:**
- `.claude/rules/business-logic-review.md` (the standard this audit measures against)
- `.claude/rules/output-review-mandate.md` §3 row "Business logic CORRECTNESS" (currently ⚠️ PARTIAL pending this gap)
- `documents/00-brd/compliance-checklist.md` (deliverable of this gap, was originally in GAP-049 scope)

---

## Current State (verified 2026-04-29)

> Per `.claude/rules/audit-to-gap-pipeline.md` Step 2.5 — state-check before filing.

| Piece | File / Path | Status |
|-------|-------------|--------|
| Review standard | `.claude/rules/business-logic-review.md` | ✅ shipped 2026-04-29 (this PR, Agent B) |
| Per-domain rules.md inventory | `documents/01-business/{kiteclass,kitehub}/**/rules.md` | ✅ exist (45 files counted via `find documents/01-business -name rules.md \| wc -l`) |
| 5-attribute compliance per file | All 45 files | ❌ unknown — none have been audited against the standard yet |
| Compliance checklist | `documents/00-brd/compliance-checklist.md` | ❌ missing (originally in GAP-049 §3 deliverable, moved here) |
| Stakeholder sign-offs on representative rules | — | ❌ none recorded |
| First quarterly audit report | `documents/04-quality/audits/business-correctness/2026-Q3.md` | ❌ not yet run |
| Full block-mode `audit-gate.py` detector | `.claude/hooks/audit-gate.py` AUDIT_RULES `business-logic-review` | 🟡 partial (warn-mode lands with rule in this PR; block-mode awaits baseline pass-rate) |

**Grep commands run:**
```bash
find documents/01-business -name rules.md | wc -l
# → 45

ls documents/00-brd/ 2>/dev/null
# → folder exists per GAP-101 / Wave 8b but compliance-checklist.md not yet created
```

---

## Problem

Phase 1 of GAP-049 (this Wave) shipped `.claude/rules/business-logic-review.md` — the standard for business-rule documentation (5 attributes: Source, Rationale, Reviewer, Compliance check, Review cadence). The standard exists; the **audit and sign-off cycle has not yet run** against the 45 existing per-domain `rules.md` files.

Without Phase 2:
- We don't know which existing rules already meet the standard vs which need backfill
- No stakeholder has signed off on representative pricing/quota/compliance rules
- The compliance checklist (PDPL 2023, Tax Law, Consumer Protection, Education Law, Cybersecurity, Labor Code) is unbuilt
- The `audit-gate.py` block-mode detector cannot land until baseline pass-rate is known
- The `output-review-mandate.md` §3 matrix row stays ⚠️ PARTIAL until this gap closes

GAP-049 cannot transition to 🟢 DONE until this gap lands. Per `gap-done-discipline.md` §3, GAP-049 stays 🟡 PARTIAL.

---

## Context

GAP-049 was filed 2026-04-14 as a P0 Business-Logic gap covering both review-process scope AND audit-execution scope. Wave Business Correctness (2026-04-29) sliced it for parallel execution:

- **Phase 1 (this Wave, Agent B):** ship the rule file + flip the matrix row + file this follow-up
- **Phase 2 (this gap, future Wave):** execute audit against existing rules.md files + collect stakeholder sign-offs + build compliance checklist + upgrade detector to block-mode

The slice follows `gap-done-discipline.md` §3 PARTIAL exit-ramp pattern. Phase 1 ships measurable enforcement (warn-mode detector + PR template checkbox + reviewer-checklist line) per `rule-change-process.md` §6.5, while Phase 2 closes the audit-execution and stakeholder-signoff side.

---

## Evidence

- Phase 1 PR (this Wave, Agent B): TBD — see PR description for full §3 matrix row flip + GAP-049 PARTIAL status flip
- `.claude/rules/business-logic-review.md` §6.2 explicitly defers full block-mode detector to this gap
- 45 `rules.md` files identified via `find documents/01-business -name rules.md | wc -l`
- `documents/00-brd/compliance-checklist.md` flagged in GAP-049 §3 deliverable list, moved here (Phase 1 explicitly excluded it from scope)

---

## Proposed Fix

Five sequenced sub-tasks. Each is a separate Sub-PR in a future wave.

### Sub-task A: Audit existing 45 rules.md files against §2 standard

For each `documents/01-business/**/rules.md` file:

1. List each business rule entry (BR-xxx, AIB-01, TR-01, etc.)
2. Score each rule against 5 §2 attributes — present / partial / missing
3. Categorize: ✅ Compliant (5/5) / 🟡 PARTIAL (3-4/5) / ❌ Non-compliant (≤2/5)
4. Output: `documents/04-quality/audits/business-correctness/2026-Q3-baseline.md` with summary table + per-file delta list

Expected baseline (rough): 5-15% compliant, given no prior standard existed. This baseline drives Sub-task B priority.

### Sub-task B: Backfill missing attributes on highest-stakes rules

Stakes priority order:
1. Compliance-touching rules (data retention, PII, tax, consumer protection)
2. Pricing + tier quotas (financial impact + competitor sensitivity)
3. Trial mechanics + conversion levers (revenue impact)
4. Domain-specific (student age, teacher qualifications, attendance) — MoET/legal area
5. Engineering thresholds with user-visible impact

Each backfilled rule gets a Log entry citing source of the attribute (e.g., "Source attribute backfilled 2026-XX-XX from PR #XXX commit + author interview").

### Sub-task C: Build `documents/00-brd/compliance-checklist.md`

Single file enumerating each VN law applicable to KiteClass + which rules.md entries each law touches. Format:

```markdown
## PDPL 2023 (Personal Data Protection Law)
- Effective: 2026-07-01
- Applies to: any rule handling user PII, consent, retention
- Touched rules:
  - DR-03 (Data retention) → `documents/01-business/.../rules.md`
  - CO-02 (Consent collection) → ...
- Compliance status: TBD (per Sub-task A audit)
- Last legal-counsel review: pending — see GAP-XXX
```

Cover at minimum: PDPL 2023, Tax Law + Decree 123/2020, Consumer Protection Law 2023, Labor Code 2019, Education Law 2019, Cybersecurity Law 2018 + Decree 53/2022, Electronic Transactions Law 2023.

### Sub-task D: Stakeholder sign-offs on representative sample

Solo-dev mode (current): formal stakeholder sign-off is gated by team growth or external counsel engagement. For Phase 2:

1. Self-sign-off acceptable on rules where Reviewer field documents which role is worn (per `business-logic-review.md` §2.3 solo-dev exemption clause)
2. **At minimum 5 representative rules** must receive external review:
   - 1 pricing/tier rule (PO consultation)
   - 1 compliance/data rule (legal counsel — even informal advisory note)
   - 1 trial/conversion rule (PO + data review)
   - 1 education-domain rule (e.g., student age — MoET reference check)
   - 1 financial rule (e.g., late-fee — Consumer Protection scout)
3. Each external sign-off recorded as a Log entry on the affected rules.md entry + linked to a source artifact (interview note, advisory email, regulation citation)

### Sub-task E: Upgrade `audit-gate.py` detector from warn-mode to block-mode

Once Sub-task A baseline is known and Sub-task B brings highest-stakes rules to compliant:

1. Detector regex: per-attribute presence check on rules.md diff lines
2. BLOCK if all 5 attributes missing on a NEW business-rule entry
3. WARN if 1-4 attributes missing on a NEW or modified entry
4. Override trailer: `BUSINESS_RULE_OVERRIDE:` per `business-logic-review.md` §8
5. Self-test on 3 fixtures (good rule, partial rule, bare rule) committed temporarily during PR

---

## Acceptance Criteria

### Sub-task A — Baseline audit
- [ ] All 45 `documents/01-business/**/rules.md` files audited against `business-logic-review.md` §2 5-attribute standard
- [ ] Summary report `documents/04-quality/audits/business-correctness/2026-Q3-baseline.md` published with per-file pass/partial/fail table
- [ ] Top-10 highest-stakes non-compliant rules identified (compliance + pricing + revenue impact)

### Sub-task B — Backfill highest-stakes rules
- [ ] All compliance-touching rules brought to 5/5 attributes (or explicitly Compliance check N/A with rationale)
- [ ] All pricing/tier/quota rules brought to 5/5 attributes
- [ ] All trial/conversion rules brought to 5/5 attributes
- [ ] Each backfilled rule has a Log entry citing the attribute source

### Sub-task C — Compliance checklist
- [ ] `documents/00-brd/compliance-checklist.md` created covering 7 VN laws minimum (PDPL, Tax + Decree 123, Consumer Protection, Labor, Education, Cybersecurity + Decree 53, Electronic Transactions)
- [ ] Each law section cross-references the rules.md entries it touches
- [ ] Compliance status per law: Compliant / Partial / Unknown / Not-applicable

### Sub-task D — Stakeholder sign-offs
- [ ] Minimum 5 representative rules have external review notes recorded (pricing × PO, compliance × legal scout, trial × data, education × MoET ref, financial × Consumer Protection)
- [ ] Each sign-off linked to source artifact (interview, advisory note, regulation citation)
- [ ] Solo-dev exemption clauses (`business-logic-review.md` §2.3) explicitly cited where applied

### Sub-task E — Block-mode detector
- [ ] `audit-gate.py` AUDIT_RULES entry `business-logic-review` upgraded from warn-mode to block-mode (5/5 attributes required on NEW business-rule entries)
- [ ] WARN-mode retained for 1-4/5 partial entries (allows iteration)
- [ ] `BUSINESS_RULE_OVERRIDE:` trailer recognized
- [ ] 3-fixture self-test committed in PR description (good / partial / bare)

### Final closure
- [ ] `output-review-mandate.md` §3 matrix row "Business logic CORRECTNESS" flipped ⚠️ PARTIAL → ✅ DONE
- [ ] GAP-049 (parent) flipped 🟡 PARTIAL → 🟢 DONE per `gap-done-discipline.md` §2 (every AC verified)
- [ ] First quarterly audit cadence kicked off (next: 2026-Q4 = ~2026-10-29)

---

## Risks of NOT fixing

- `output-review-mandate.md` §4 VIOLATION matrix never reaches 0 (one row stuck at ⚠️ PARTIAL)
- Existing 45 rules.md files drift further from the standard with each new rule added without 5 attributes
- Compliance posture against PDPL 2023 (effective 2026-07-01) cannot be defended — auditor cannot point to a checklist
- Stakeholder sign-off process never operationalizes — solo-dev hat-wearing becomes permanent rather than transitional
- Block-mode detector never lands — warn-mode tolerates ongoing drift indefinitely

---

## Dependencies

- **Phase 1 (GAP-049 / `business-logic-review.md`)** — must land before this gap can start (✅ shipped 2026-04-29 this Wave)
- **GAP-150** — BRD docs completion (compliance-scope.md skeleton). Sub-task C `compliance-checklist.md` is a downstream deliverable; partial overlap with GAP-150 scope
- **GAP-151 / GAP-152** — persona AC framework + execution. Persona-based business review can surface additional business rules needing review
- Legal counsel engagement (operational, outside this gap) — needed for formal Sub-task D sign-offs once team grows

---

## Out-of-scope

- International compliance (GDPR, CCPA) — deferred until KiteClass scales beyond Vietnam
- A/B testing infrastructure (GAP-044) — referenced as future enabler, not a deliverable here
- Per-domain rules.md schema migration — assumed existing 3-layer structure (rules / use-cases / api-contract) is correct; this gap only adds attribute discipline to rules.md content

---

## Related

- **Parent:** GAP-049 (Business Logic Correctness Review) — this gap closes Phase 2 of that gap's scope split
- **Sibling Phase 1:** `.claude/rules/business-logic-review.md` (standard shipped 2026-04-29)
- **Matrix sync:** `.claude/rules/output-review-mandate.md` §3 row "Business logic CORRECTNESS" — flipped to ⚠️ PARTIAL by Phase 1, will flip to ✅ DONE by this gap
- **Audit pipeline:** `.claude/rules/audit-to-gap-pipeline.md` — quarterly audit cycle starts in 2026-Q3 baseline
- **Closure discipline:** `.claude/rules/gap-done-discipline.md` — drives Sub-task E + final GAP-049 closure timing
- **Priority:** `.claude/rules/meta-gap-priority.md` §3 — Business-Logic-P0 tier, ranks above Feature-P0
- **Compliance checklist source:** GAP-101 / GAP-150 (BRD folder + scope expansion)

---

## Log

- **2026-04-29** — Gap filed as Phase 2 of GAP-049 scope split (Wave Business Correctness, Agent B). State-check confirms 45 rules.md files exist but none audited against new `business-logic-review.md` standard. Sub-tasks A–E sequenced; first quarterly audit baseline targeted for 2026-Q3 (~2026-08).
