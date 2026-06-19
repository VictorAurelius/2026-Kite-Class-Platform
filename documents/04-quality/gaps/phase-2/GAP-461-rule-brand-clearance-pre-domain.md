# GAP-461: Meta-rule — brand-clearance check pre-domain decision

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 META (rule + enforcement) — per `meta-gap-priority.md` §3 Meta-tier above Feature-P1; preventing recurrence of GAP-460 class incidents
**Domain:** Meta-governance / `.claude/rules/`
**Found:** 2026-05-10 (incident-to-rule-pipeline Stage 3 trigger from GAP-460)
**Affects:** every future brand decision, domain acquisition, platform rename, decision-doc landing config-shaped name value

## Problem

GAP-460 (brand pivot KiteHub → KiteClass.me) was triggered by user-flagged collision: searching "kitehub" on Google surfaced existing `kitehub.eu` (Czech water sports SaaS) as #1 result. Investigation revealed adjacent collisions also exist (KU Kite® US K-12 assessment + International Kiteboarding Association + Kerala IT).

**Root cause:** GAP-458 (chose `kitehub.me` 2026-05-09) shipped without trademark/brand-clearance verification — no rule mandated WIPO/USPTO/EUIPO/NOIP search before domain decision OR landing platform name in code/docs.

**Cost of the miss:**
- AWS Activate Founder application denied 2026-05-10 (~$1k credit)
- GAP-459 sweep work scoped to `.vn` → `.me` becomes scope-changed to `kitehub` → `kiteclass` customer surface
- Tier 1/2/3 cutover work (PR #1084/#1085) partially superseded
- Cloudflare Origin Cert (15-year validity) for `kitehub.me` retained but only useful for internal admin
- Branding decision must redo with proper clearance

Without a rule, the next decision-doc proposing a new platform name, domain, brand, or rename would repeat the same miss.

## Proposed Fix

File new rule `.claude/rules/brand-clearance-pre-domain.md` v1.0.0 with:

### Scope
- Every PR landing a NEW platform/product name in code, docs, infrastructure
- Every domain acquisition decision
- Every platform rename or rebrand decision
- Every decision-doc (gap, ADR, runbook) introducing a config-shaped brand value

### Mandate (the rule)
> Before landing any decision touching brand name (platform / product / domain / rename), verify:
> 1. **WIPO Madrid Protocol** registry search (Madrid Monitor)
> 2. **USPTO TESS** (US trademark database) — Class 9 software + Class 41 education + Class 42 SaaS
> 3. **EUIPO eSearch** (EU trademark database) — same classes
> 4. **NOIP Vietnam** (target market trademark search) — same classes
> 5. **Google search** for top 10 results across `<name>` + `<name> + sector keyword`
> 6. **Domain availability** check via RDAP across `.com / .vn / .com.vn / .me / .io` minimum
> 7. **Document findings** in 5-attribute review per `business-logic-review.md` §2

If any database returns prior-use mark or commercial collision in same/adjacent vertical, escalate to:
- **Same vertical + same class:** BLOCK decision, mandate alternative name
- **Adjacent vertical + same class:** WARN, document differentiation argument, queue legal scout review
- **Different vertical or different class:** PROCEED with documented coexistence rationale + 5-attribute review

### Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity)

- **PR template checkbox** (lands same PR as rule):
  > - [ ] **Brand-clearance** — if PR introduces NEW platform name / product name / domain / rename, brand-clearance verification per `brand-clearance-pre-domain.md` complete: WIPO + USPTO + EUIPO + NOIP + Google + RDAP findings documented in 5-attribute review
- **Reviewer-checklist line** for decision-doc PRs:
  > Did this PR introduce a brand-shaped name (platform / domain / rebrand)? If yes — verify §X databases checked + 5-attribute review documented + collision risk classified per matrix
- **Override mechanism:** `BRAND_CLEARANCE_OVERRIDE: <reason — emergency hotfix / legacy preservation / etc>` commit trailer; logged quarterly retro
- **Detector deferred** to 2nd recurrence per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days

### Self-test (worked example — applied retroactively to GAP-458)

State at GAP-458 decision time (2026-05-09):
- Domain `kitehub.me` acquisition proposed
- ❌ WIPO check NOT run
- ❌ USPTO check NOT run (would have surfaced KU Kite®)
- ❌ EUIPO check NOT run (would have surfaced KiteHub.eu)
- ❌ NOIP check NOT run
- ❌ Google "kitehub" check NOT run (would have surfaced kitehub.eu #1)
- ✅ RDAP check run (kitehub.me available)

Counterfactual with rule applied: Step 5 Google search would have surfaced kitehub.eu in <30 seconds → escalate to "Adjacent vertical + same Class 42 SaaS" → WARN + document differentiation OR mandate alternative → no GAP-460 cascade, no AWS Activate denial.

→ Rule fires correctly on the originating incident. Self-test PASS ✅

## Acceptance Criteria

- [ ] `.claude/rules/brand-clearance-pre-domain.md` v1.0.0 filed with full content per §Proposed Fix above
- [ ] PR template `.github/pull_request_template.md` extended with brand-clearance checkbox
- [ ] Reviewer-checklist line documented in rule §Enforcement
- [ ] Worked self-test §Proposed Fix preserved in rule body (§6 standard structure)
- [ ] `output-review-mandate.md` §3 matrix row added for "Brand decisions / domain acquisitions"
- [ ] Cross-link added to `audit-to-gap-pipeline.md` §2.7 (Decision-Doc Code-Sync) — brand-shaped values trigger brand-clearance check
- [ ] Memory entry `feedback_brand_clearance_pre_domain.md` paired same PR (auto-load enforcement)
- [ ] Rule shipped same PR as enforcement artifacts per `rule-change-process.md` §6.5 Enforcement Parity Mandate

## Related

- Parent: [GAP-460](GAP-460-brand-pivot-kiteclass-me-customer-facing.md) (incident this rule prevents recurrence of)
- Pipeline: `incident-to-rule-pipeline.md` (5-stage applied to this gap)
- Cross-link: `business-logic-review.md` §3 Identity (covers brand under "Identity & access" but does not enforce trademark clearance specifically — this rule fills that gap)
- Cross-link: `audit-to-gap-pipeline.md` §2.7 (Decision-Doc Code-Sync; brand decisions are a class of config-shaped value)
- Cross-link: `rule-change-process.md` §6.5 (Enforcement Parity Mandate — rule + detection same PR)

## Log


- 2026-06-14: phase re-triage — n/a→phase-1-beta (meta-rule brand-clearance pre-domain; rule file missing).
- 2026-06-01 — **Wave meta-8 Bucket B SCOPE-REVISE:** SCOPE-REVISE: rule file genuinely missing. Either ship `.claude/rules/brand-clearance-pre-domain.md` next wave OR re-frame gap as "deferred — solo-dev no brand-clearance flow pre-Phase-2". CSV completion_pct adjusted to 0%; gap body Status/AC reflect documented scope BEFORE Wave meta-7 audit — re-read audit artifact for current empirical reality. Source: `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-c-p1-open-2.md`.

- **2026-05-10**: Filed at GAP-460 decision time per `incident-to-rule-pipeline.md` Stage 3 (Rule + Enforce). Rule itself deferred to follow-up wave (Wave 53+ when current Wave 50/51/52 closure load reduces); meta-priority P1 per `meta-gap-priority.md` §3 (rule fix has 1 to N future-PR leverage). Detector wiring (audit-gate.py AUDIT_RULES) deferred to 2nd recurrence per premature-rule guard.
