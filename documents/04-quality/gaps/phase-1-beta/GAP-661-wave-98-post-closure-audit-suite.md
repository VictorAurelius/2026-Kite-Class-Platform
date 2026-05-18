# GAP-661: Wave 98 post-closure audit suite — UI /128 + Quality /100 within 3 days

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Meta
**Detected:** 2026-05-18 (Wave 98 closure compliance per `post-wave-audit-mandate.md` §2.2)
**Parent:** [Wave 98 plan §9 Scope-Completeness Reconciliation](../../03-planning/waves/wave-2026-05-18-98-cluster-b-beta-cohort-polish.md)

## Problem

Per `post-wave-audit-mandate.md` §2.2 — every wave merge requires audit suite within **3 days post-wave-merge cadence**. Wave 98 shipped 8/8 buckets 2026-05-18; audit suite due ≤2026-05-21.

Required audits per §2.1 file-pattern matrix matching Wave 98 changes:

| File pattern changed | Audit required | Skill |
|---|---|---|
| `kitehub-frontend/src/` (B0/B3/B4/B5/B6 + B7 FE) | **UI /128** | `quality/ui-review/SKILL.md` |
| `kitehub/{email,platform,subscription}/.../*.java` + `*Controller.java` (B0/B1/B7) | **API Contract /100** | `quality/api-contract-audit/SKILL.md` |
| `documents/01-business/.../rules.md` + new domain doc (B0/B1/B2) | **Business Logic /100** | `quality/business-logic-audit/SKILL.md` |
| `pom.xml` updates (B0 + B1 deps) | **Security /100** | `quality/security-audit/SKILL.md` |
| Wave merge | **Quality /100 refresh** | `quality-audit/SKILL.md` |

Wave 98 also introduced new artifacts requiring fresh audit:
- `documents/01-business/kitehub/{preferences,email,seed}/{rules,use-cases,api-contract}.md` (B0+B1+B2 3-layer docs)
- New controller endpoint `POST /api/v1/preferences/dismiss-banner-state` (PreferencesController)
- New BE service `VietnamSampleDataGenerator` (kitehub-platform)
- New FE component cluster `SupportMenu` + `FeedbackForm` + `OnboardingCoordinator` + `useOnboardingPhase`
- New Tone enum + EmailTemplateRenderer (kitehub-email)

## Proposed Fix

### Step 1: Run UI /128 audit (sample 3-5 screens)

Per `quality/ui-review/SKILL.md`:
- Sample screens: dashboard layout (banner mount), /beta-status, /legal/terms, /legal/privacy, SupportMenu mobile ≤375px
- Score: 5 dimensions × per-screen rubric
- Save report: `documents/04-quality/audits/ui/2026-05-2X-wave-98-cluster-b-sample.md`

### Step 2: Run Quality /100 refresh

Per `quality-audit/SKILL.md` v1.1:
- 11 categories /110 (10 tech + 1 persona coverage)
- Compare to Wave 53 baseline 85/110 (87/100 / 80 tech-only) B+
- Save report: `documents/04-quality/audits/quality/2026-05-2X-wave-98-refresh.md`

### Step 3: Run API Contract /100 (deferred audits from Wave 98 AUDIT_OVERRIDE)

Per `quality/api-contract-audit/SKILL.md`:
- Focus on 3 new api-contract.md files (preferences + email + seed)
- Verify endpoint shape + error codes + auth requirements
- Save report: `documents/04-quality/audits/api-contract/2026-05-2X-wave-98-new-contracts.md`

### Step 4: Run Business Logic /100 (deferred audits)

Per `quality/business-logic-audit/SKILL.md`:
- Verify business rule IDs (BR-PREFERENCES-* / BR-EMAIL-* / BR-SEED-*) consistent across docs + code
- Save report: `documents/04-quality/audits/business/2026-05-2X-wave-98-new-domains.md`

### Step 5: Update audits-index.csv

Append rows for 4 new audit artifacts per `meta-csv-index-pattern.md` §3.

### Step 6: Update output-review-mandate.md §3 matrix rows

Update REFRESHED markers + scores for matrix rows touched by audits.

### Step 7: File any P0/P1 findings as follow-up gaps

Per `audit-to-gap-pipeline.md` §3 — audit findings → gap files → ROADMAP §🚀.

## Acceptance Criteria

- [ ] UI /128 sample audit shipped to `documents/04-quality/audits/ui/`
- [ ] Quality /100 refresh shipped to `documents/04-quality/audits/quality/`
- [ ] API Contract /100 audit shipped to `documents/04-quality/audits/api-contract/`
- [ ] Business Logic /100 audit shipped to `documents/04-quality/audits/business/`
- [ ] `audits-index.csv` synced (4 new rows)
- [ ] `output-review-mandate.md` §3 matrix rows updated (REFRESHED markers + scores)
- [ ] Any P0/P1 findings filed as new gaps per `audit-to-gap-pipeline.md`
- [ ] Completed within 3-day cadence: ≤2026-05-21

## Effort estimate

~3-5h (4 audit suites + 1 quality refresh + cross-references + matrix updates). Could be parallelized into 2-agent run (each handles 2 audit suites).

## Related

- **Parent:** Wave 98 closure (this gap captures the orphan post-wave audit cadence item per `wave-closure-scope-completeness.md` v1.0.0 §3)
- **Cadence rule:** `post-wave-audit-mandate.md` §2.2 (≤3 days post-wave-merge)
- **Override trailers** on Wave 98 PRs cite this gap as deferral target:
  - PR #1548 (B0): `AUDIT_OVERRIDE: api-contract-audit DEFER → Wave 98 closure suite`
  - PR #1553 (B1): `AUDIT_OVERRIDE: business-logic-audit + api-contract-audit DEFER`
- **Phase 1 BETA gate impact:** Quality /100 ≥80 required per CLAUDE.md §CURRENT PHASE Phase 1 progression triggers
- **Wave 98 final tally:** 5 gaps DONE 100% + 5 PARTIAL ≥80% + 1 orphan-audit-cadence (this gap)
