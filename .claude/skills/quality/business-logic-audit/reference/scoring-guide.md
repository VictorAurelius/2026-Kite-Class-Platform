# Business Logic Audit — Scoring Guide

## Grading Scale

| Score | Grade | Meaning |
|-------|-------|---------|
| 90-100 | A | Production-ready business logic |
| 80-89 | B | Good — minor gaps, no blockers |
| 70-79 | C | Acceptable — gaps need tracking |
| 60-69 | D | Significant gaps — block release |
| <60 | F | Major rework needed |

---

## Category 1: Rule Coverage (20 pts)

Verify each `BR-xxx` in `rules.md` has corresponding code implementation.

| Score | Criteria |
|-------|----------|
| 20 | 100% BR-xxx have code implementation + test |
| 16 | ≥90% covered, missing ones are low-priority |
| 12 | ≥75% covered, some P1 rules missing |
| 8 | ≥50% covered, P0 rules missing |
| 4 | <50% covered |
| 0 | No traceability between rules and code |

**How to check:**
```
1. Read rules.md → list all BR-xxx
2. For each BR-xxx:
   a. Grep codebase for related class/method — use BROAD scope
      ✅ grep -rn "ClassName" --include="*.java"
      ❌ grep -r "ClassName" kitehub/ kiteclass/   # may miss -core submodules
   b. Verify logic matches rule description
   c. Check if test exists for the rule
3. Score: (matched / total) × 20
```

**Example:**
- `BR-001: Trial period = 14 days` → find `TRIAL_DAYS` config → verify `application.yml` = 14

**False-positive guard** (GAP-107 lesson): before scoring "rule has no implementation", re-run grep without dir restriction. Multi-module Maven projects put classes under `{project}-core/`, `{project}-gateway/` — narrow scope silently misses them.

---

## Category 2: Config Accuracy (20 pts)

Config keys referenced in `rules.md` must exist and have correct values in `application.yml`.

| Score | Criteria |
|-------|----------|
| 20 | 100% config keys match docs, test profile also correct |
| 16 | All keys exist, 1-2 value mismatches (non-critical) |
| 12 | Keys exist but ≥3 value mismatches |
| 8 | Some documented keys missing from config |
| 4 | Config and docs significantly diverged |
| 0 | No config externalization — hardcoded values |

**How to check:**
```
1. Extract config keys from rules.md (format: `config.key.name`)
2. Grep application.yml for each key — scope ALL module resource dirs:
   grep -rn "key.name" --include="*.yml"
   # or explicit: kiteclass/*/src/main/resources/ kitehub/*/src/main/resources/
3. Compare documented default vs actual value
4. Check application-test.yml has test-safe values
```

---

## Category 3: Edge Case Tests (20 pts)

Each error/edge case in `use-cases.md` should have a corresponding test.

| Score | Criteria |
|-------|----------|
| 20 | Every UC error path has test, including boundary cases |
| 16 | ≥90% error paths tested |
| 12 | Happy paths tested, ≥50% error paths |
| 8 | Only happy paths tested |
| 4 | Minimal tests, no edge cases |
| 0 | No tests for use cases |

**How to check:**
```
1. Read use-cases.md → list all error/edge cases per UC-xxx
2. Find corresponding *Test.java
3. Check test method names cover error scenarios
4. Score: (tested_edges / total_edges) × 20
```

---

## Category 4: Cross-Domain Consistency (20 pts)

Rules across domains must not contradict each other.

| Score | Criteria |
|-------|----------|
| 20 | No contradictions, shared concepts defined once |
| 16 | Minor naming inconsistencies (cosmetic) |
| 12 | 1 logical inconsistency (non-breaking) |
| 8 | 2-3 contradicting rules |
| 4 | Significant contradictions affecting user experience |
| 0 | Domains designed in isolation, many conflicts |

**Common contradictions to check:**
- Trial duration: same across subscription + billing + onboarding docs?
- Role names: OWNER vs ADMIN vs MANAGER — consistent?
- Status enums: same state names across lifecycle docs?
- Config key naming: `kiteclass.*` vs `kite.*` prefix consistency?

---

## Category 5: Stakeholder Alignment (20 pts)

Rules must reflect Vietnamese education market reality + legal requirements.

| Score | Criteria |
|-------|----------|
| 20 | Rules validated by domain expert, legal reviewed |
| 16 | Rules reasonable, no obvious market mismatch |
| 12 | Some rules need market validation (flagged) |
| 8 | Rules based on assumptions, not validated |
| 4 | Rules contradict known VN regulations |
| 0 | No consideration of market/legal context |

**Checklist (flag for human review):**
- [ ] Pricing tiers match VN education center budgets?
- [ ] GDPR + VN data privacy law (Nghị định 13/2023) considered?
- [ ] Tax invoice rules match VN tax law?
- [ ] Student age restrictions comply with VN child protection?
- [ ] Content moderation aligns with VN content regulations?

**Note:** Claude flags items for review, human makes final decision.
