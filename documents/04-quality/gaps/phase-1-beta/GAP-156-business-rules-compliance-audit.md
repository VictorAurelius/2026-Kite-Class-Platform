# GAP-156: Business Rules Compliance Audit + Stakeholder Sign-offs

**Status:** 🟡 PARTIAL (A + C + E DONE 2026-06-21; B in-progress; D = legal counsel, BLOCKED Phase 2)
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
| Review standard | `.claude/rules/business-logic-review.md` | ✅ shipped 2026-04-29 |
| Per-domain rules.md inventory | `documents/01-business/{kiteclass,kitehub}/**/rules.md` | ✅ **75 files** (gap text "45" stale; re-counted 2026-06-21: 48 kiteclass + 25 kitehub + 2 shared) |
| 5-attribute compliance per file | All 75 files | ✅ **AC-A audited 2026-06-21** — 68/75 = 91% have full 5/5 block (GAP-433 prior backfill); structural ≈91%, independent-verification ≈0% (all Reviewer solo-dev placeholder, Compliance "Considered" not counsel-verified, 47/75 Source "informed gut") |
| Compliance checklist | `documents/00-brd/compliance-checklist.md` | ✅ **AC-C DONE 2026-06-21** — 7 VN laws L1-L7 + domain→law matrix + per-law posture (self-assessed) |
| Stakeholder sign-offs on representative rules | — | ❌ AC-D BLOCKED — legal counsel not engaged (Phase 2); REAL-USER-ACTION, not Claude-closable |
| First quarterly audit report | `documents/04-quality/audits/business-correctness/2026-Q3.md` | ✅ **AC-A DONE 2026-06-21** — baseline scorecard + highest-stakes backfill queue + VN-law mappings |
| 5-attribute detector (warn→block) | `scripts/check-business-rule-attributes.sh` + CI job `business-rule-attributes` | ✅ **AC-E DONE 2026-06-21** — real detector (the §6.2 `audit-gate.py` partial detector never existed); block-on-ADDED / warn-on-MODIFIED + 3 fixtures (self-test 3/3 PASS) + override trailer |

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

### Sub-task A — Baseline audit ✅ DONE 2026-06-21
- [x] All **75** `documents/01-business/**/rules.md` files audited against `business-logic-review.md` §2 5-attribute standard (gap text "45" was stale)
- [x] Summary report `documents/04-quality/audits/business-correctness/2026-Q3.md` published with per-file scorecard + metrics
- [x] Highest-stakes backfill queue identified (payroll / payment-invoice / subscription-billing / payment-record / course-pricing / child-protection / data-retention) with VN-law mappings

### Sub-task B — Backfill highest-stakes rules 🟡 mostly DONE 2026-06-21 (15 files backfilled)
- [x] All compliance-touching rules brought to 5/5 (or N/A with rationale) — 7 non-compliant files brought to 5/5 + 8 HIGH-stakes Compliance citations upgraded to specific VN laws (per `compliance-checklist.md`); baseline 68/75 already had blocks. Over-claimed "Compliant" honestly downgraded → "Considered (self-assessed, counsel pending AC-D)"
- [x] All pricing/tier/quota rules brought to 5/5 — `course-pricing` + `subscription-billing` upgraded; rest pre-compliant (baseline)
- [x] All trial/conversion rules brought to 5/5 — pre-compliant per baseline audit
- [ ] Each backfilled rule has a per-file Log entry citing attribute source — DEFERRED (blocks edited in-place; per-file Log = low-value follow-up). NOTE honesty flags from backfill folded into AC-D: (1) `child-protection` per-rule blocks cite "NĐ13/2023 Art 16" but canonical is Điều 20 → counsel reconcile; (2) `payment-record` cites Luật PCRT 2022 (AML overlay, outside 7-law checklist) — both need counsel confirm

### Sub-task C — Compliance checklist ✅ DONE 2026-06-21
- [x] `documents/00-brd/compliance-checklist.md` created covering 7 VN laws (PDPL/NĐ13, Tax + NĐ123, Consumer Protection, Labor, Education, Cybersecurity + NĐ53, Electronic Transactions)
- [x] Each law section cross-references domains (§3 per-law tables + §4 domain→law matrix)
- [x] Compliance posture per law: N/A / Considered / Compliant / GAP (self-assessed v1; counsel review = AC-D)

### Sub-task D — Stakeholder sign-offs
- [ ] Minimum 5 representative rules have external review notes recorded (pricing × PO, compliance × legal scout, trial × data, education × MoET ref, financial × Consumer Protection)
- [ ] Each sign-off linked to source artifact (interview, advisory note, regulation citation)
- [ ] Solo-dev exemption clauses (`business-logic-review.md` §2.3) explicitly cited where applied

### Sub-task E — Block-mode detector ✅ DONE 2026-06-21
- [x] Detector built as `scripts/check-business-rule-attributes.sh` + CI job `business-rule-attributes` (NOTE: §6.2 `audit-gate.py` partial detector never actually existed — `audit-gate.py:30` only triggers the skill; §6.2 corrected this PR). Block-on-ADDED rules.md (born-compliant) — safer than blanket block
- [x] WARN-mode retained for MODIFIED existing files (grandfathered; flip via `BUSINESS_RULE_BLOCK_MODIFIED=1` after bucket-B backfill)
- [x] `BUSINESS_RULE_OVERRIDE:` trailer recognized (downgrades block→warn)
- [x] 3-fixture self-test (`--self-test`): compliant→PASS / missing→BLOCK / missing+override→WARN, all 3/3 green

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

- **2026-06-21** — Status PENDING → 🟡 PARTIAL (70%). Executed Sub-tasks A + C + E + most of B in one session (2 Opus agents + inline coordinator per `agent-concurrency-budget-inline-hybrid.md`):
  - **A (baseline audit):** `documents/04-quality/audits/business-correctness/2026-Q3.md` — re-counted **75 rules.md** (gap text "45" stale). Key reframing: structural 5-attribute presence ≈ **91%** (68/75 have full 5/5 block from prior GAP-433 backfill), but **independent-verification ≈ 0%** (all Reviewer = solo-dev placeholder, all Compliance = "Considered" not counsel-verified, 47/75 Source = "informed gut"). The REAL remaining gap is verification (AC-D legal counsel, BLOCKED), not missing docs.
  - **C (compliance checklist):** `documents/00-brd/compliance-checklist.md` — 7 VN laws L1-L7 + domain→law matrix + per-law obligation posture (self-assessed).
  - **E (detector):** built the REAL detector `scripts/check-business-rule-attributes.sh` + CI job `business-rule-attributes` + 3 fixtures (self-test 3/3). Discovery: `business-logic-review.md` §6.2 claimed an `audit-gate.py` partial detector that never existed (`audit-gate.py:30` only triggers the skill) — §6.2 corrected + rule bumped v1.0.1→v1.1.0. Detector blocks on ADDED rules.md (born-compliant), warns on MODIFIED (grandfathered); `BUSINESS_RULE_OVERRIDE:` trailer downgrades.
  - **B (backfill):** 15 `rules.md` edited — 8 Tier-1 HIGH-stakes Compliance citations upgraded to specific VN laws + 7 non-compliant files brought to 5/5. Honest: no fabrication, "informed gut" declared, over-claimed "Compliant" downgraded → "Considered (counsel pending)". 2 honesty flags folded into AC-D.
  - **D (legal sign-off):** BLOCKED — counsel not engaged (Phase 2); REAL-USER-ACTION per memory `feedback_real_user_action_not_a_gap.md`, not Claude-closable.
  GAP-049 (parent) stays PARTIAL (GAP-156 cannot close while AC-D blocked). CSV synced PENDING→PARTIAL/70%. Audits-index row added.
- **2026-04-29** — Gap filed as Phase 2 of GAP-049 scope split (Wave Business Correctness, Agent B). State-check confirms 45 rules.md files exist but none audited against new `business-logic-review.md` standard. Sub-tasks A–E sequenced; first quarterly audit baseline targeted for 2026-Q3 (~2026-08).
