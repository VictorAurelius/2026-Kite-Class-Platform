---
title: Business Logic Audit — Wave 98 New Domains (preferences/email/seed)
status: complete
created: 2026-05-19
phase: phase-1-beta
wave: 98
gaps: [GAP-661, GAP-656, GAP-657, GAP-658, GAP-659, GAP-639, GAP-640]
audit_skill: business-logic-audit
audit_version: v2 (per-check rubric)
baseline_audit: documents/04-quality/audits/business-logic/2026-05-18-wave-92-business-logic-audit.md
baseline_score: 70/100 C
---

# Business Logic /100 — Wave 98 Post-Closure Audit

## Scope

Wave 98 Cluster B beta-cohort polish shipped 8 buckets including **3 NEW business domains** dưới `documents/01-business/kitehub/`:

| Domain | Wave 98 Bucket | Gap | Commit |
|---|---|---|---|
| **preferences** | B0 UI Coordinator | GAP-656 | b12ea568 |
| **email** | B1 email layer hardening | GAP-657 + GAP-659 | 8d0e4fb9 |
| **seed** | B2 VN sample seed worker | GAP-658 | 54d23b3f |

Audit scope:
1. 3-layer completeness per `documents/01-business/README.md` mandate (rules.md + use-cases.md + api-contract.md per domain)
2. BR-ID coverage + cross-file consistency (BR-PREFERENCES-* / BR-EMAIL-* / BR-SEED-*)
3. 5-attribute coverage per `business-logic-review.md` §2 (Source / Rationale / Reviewer / Compliance / Review cadence)
4. Code ↔ rules.md sync (Living Docs rule)
5. Delta vs Wave 92 baseline 70/100 C — assess drift OR improvement
6. Compare to Wave 92 carry-forward findings (GAP-639 ABORTED enum orphan / GAP-640 admin-audit 3-layer missing)

Out of scope: Wave 92 rules.md 5-attr backfill (covered by quarterly GAP-156), code-level full domain re-audit of existing 22 domains.

## Bug list (primacy: bug-finding > scoring per `audit-skill-rubric-business-logic-audit.md` §4)

### P0 — none

No P0 (no production runtime business rule violation; all 3 new domains have functioning code paths matching contracts).

### P1 — Living Docs 3-layer incomplete (2 of 3 new domains)

**Finding 1 — preferences/ missing rules.md + use-cases.md (P1 META)**
- Evidence: `ls documents/01-business/kitehub/preferences/` → only `api-contract.md` ships; rules.md + use-cases.md MISSING
- Rule violated: `documents/01-business/README.md` §2 "Mỗi domain = 1 folder với 3 files" mandate + CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure"
- Impact: no canonical business rule statement for banner dismissal cookie policy (30-day Max-Age, cookie naming `kite-banner-dismissed-{key}`, httpOnly=false rationale, in-memory Phase 1 vs persistence Phase 2 split). Future reader/dev must reverse-engineer from controller comments.
- Gap to file: **GAP-662 — preferences/ 3-layer doc completion (rules.md + use-cases.md)**

**Finding 2 — email/ missing use-cases.md (P1 META)**
- Evidence: `ls documents/01-business/kitehub/email/` → rules.md + api-contract.md ship; use-cases.md MISSING. Existing legacy `email-lifecycle/` (full 3-layer per README.md index) is a DIFFERENT domain; new `email/` is sister scope (deliverability + tone register).
- Rule violated: same as Finding 1
- Impact: no canonical actor/flow walkthrough for 5 critical template send paths + tone resolution flow + scheduler observability path. BR-EMAIL-004 tone resolution logic exists in code (`Tone.fromRole`) but no use-case narrative.
- Gap to file: **GAP-663 — email/ use-cases.md completion**

### P2 — code traceability missing

**Finding 3 — BR-ID code references absent (P2)**
- Evidence: `grep -rnE "BR-(EMAIL|SEED|PREFERENCES)-[0-9]+" kitehub/ --include="*.java"` → **0 hits**
- Rule violated: `documents/01-business/README.md` §4 "Verification chain: BR-xxx → UC-xxx → endpoint → @Mapping → @Test" + CLAUDE.md §"Verification chain"
- Impact: code-doc drift detection capability degraded. Reader cannot grep from controller to canonical business rule. Affects all 3 new domains uniformly.
- Mitigation: Tone.java javadoc + VietnamSampleDataGenerator.java javadoc + PreferencesController.java javadoc DO reference contracts/rules text-search style; partial credit.
- Gap to file: **GAP-664 — BR-ID javadoc annotation backfill for preferences/email/seed (P2 batch)**

**Finding 4 — 5-attribute coverage = 0% for all 3 new domains' rules.md (P2)**
- Evidence: BR-EMAIL-001..007 + BR-SEED-001..010 lack Source / Rationale / Reviewer / Compliance / Review cadence attributes per `business-logic-review.md` §2 mandate
- Rule violated: `business-logic-review.md` §2 (5 mandatory attributes per business rule entry)
- Impact: same pattern as Wave 92 baseline finding GAP-156 quarterly audit scope. Wave 98 introduced 17 new business rules WITHOUT 5-attribute discipline = expanded scope of GAP-156 backlog.
- Note: solo-dev exemption §2.3 applies but Reviewer field must declare role-hat. None do.
- Gap to file: **rolled into existing GAP-156 quarterly business correctness backlog scope** (not a new gap; Wave 98 rules join queue)

**Finding 5 — documents/01-business/README.md index NOT updated (P2)**
- Evidence: README.md §5 KiteHub index table shows 8 domains; 3 new domains (preferences/email/seed) missing rows
- Rule violated: CLAUDE.md §"Living Documents" — `documents/01-business/README.md` must update when new business doc added
- Gap to file: **GAP-665 — business docs README.md index sync for Wave 98 new domains (P2 doc hygiene)**

## Per-domain audit table

| Domain | rules.md | use-cases.md | api-contract.md | BR-IDs present | Code refs (grep) | 5-attr coverage | Code ↔ doc sync |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **preferences** | ❌ MISSING | ❌ MISSING | ✅ | N/A (no rules.md) | 0 hits | 0% (N/A) | ✅ contract matches controller |
| **email** | ✅ 7 BR-IDs | ❌ MISSING | ✅ | BR-EMAIL-001..007 consistent | 0 hits | 0% | ✅ Tone enum + headers wired |
| **seed** | ✅ 10 BR-IDs | ✅ 3 UCs | ✅ | BR-SEED-001..010 consistent | 0 hits | 0% | ✅ VietnamSampleDataGenerator matches 10 BRs; 6 CSVs ship |

Cross-file consistency within `seed/`: BR-IDs in rules.md ↔ use-cases.md ↔ api-contract.md all align (UC-SEED-001 cites BR-SEED-001..010; api-contract.md method signatures match each BR). ✅

## Category scores (per `audit-skill-rubric-business-logic-audit.md` §2)

| # | Category (20 pts) | Sub-check verdict | Score | Notes |
|---|---|---|:---:|---|
| 1 | **Rule Coverage** | 2 P1 FAIL (preferences rules.md missing + email use-cases.md missing); 1 P2 FAIL (BR-ID code refs) | **12/20** | P0 count = 0, P1 count = 2 (×3 = -6), P2 count = 1 (×1 = -1) — cap below 16 because not P0; 20-6-1 = 13; rounded conservative 12 |
| 2 | **Config Accuracy** | seed.locale wired (`@Value` confirmed); aws.ses.reply-to-email + unsubscribe-mailto + from-email wired (SESEmailService.java); email.provider wired; PASS | **20/20** | All config keys in rules.md match `@Value` in Java code |
| 3 | **Edge Case Tests** | UC-SEED-002 covers English locale test fixture path; preferences sanitizeBannerKey defensive; email tone fallback `FORMAL_SAFE_DEFAULT` for anonymous/null. PARTIAL — no IT tests visible for VietnamSampleDataGenerator round-trip | **14/20** | 1 P1 PARTIAL (-3); 1 P2 (Testcontainers IT for new Postgres-side artifacts deferred) — assume 17 then round down for unverified |
| 4 | **Cross-Domain Consistency** | seed/ refs `user-manual-content-standard.md` §2 row 7 ✅; email/ refs `audit-service-isolation.md` + `postgres-specific-type-testcontainers.md` ✅; preferences/ refs `contract-first-for-cross-layer.md` + `pre-launch-auth-hardening-checklist.md` ✅; documents/01-business/README.md index NOT updated (Finding 5) | **17/20** | P2 doc-hygiene -1; cross-rule citation clean |
| 5 | **Stakeholder Alignment** | 5-attribute coverage = 0% on all 17 new business rules; no Reviewer role-hat declaration; falls into GAP-156 quarterly review queue | **10/20** | Documented pattern carry-forward from Wave 92 baseline; solo-dev exemption applies but mandate per `business-logic-review.md` §2.3 requires explicit role-hat |

## Overall score

```
Total = 12 + 20 + 14 + 17 + 10 = 73/100
Grade = C+
```

## Delta vs Wave 92 baseline

| Metric | Wave 92 (2026-05-18) | Wave 98 (2026-05-19) | Delta |
|---|:---:|:---:|:---:|
| Overall score | 70/100 C | **73/100 C+** | **+3** |
| Cat 1 Rule Coverage | PARTIAL FAIL (2 P1 NEW) | PARTIAL FAIL (2 P1 NEW different) | recurrence pattern |
| Cat 2 Config Accuracy | not separately scored | 20/20 PASS | ✅ |
| Cat 5 Stakeholder | 5-attr backfill carry | same carry + 17 new BRs | scope grew |
| 3-layer completeness | admin-audit 3-layer missing (GAP-640) | preferences 2/3 missing + email 1/3 missing (GAP-662 + GAP-663) | recurrence |
| BR-ID code refs | not measured | 0% (3 new domains) | new finding GAP-664 |

**Delta drivers (+3):**
- +2: seed/ shipped full 3-layer with cross-file BR consistency (model citizen for new domains)
- +2: Code ↔ doc sync verified across all 3 (Tone enum, VietnamSampleDataGenerator, PreferencesController all match contracts)
- +1: Cat 2 Config Accuracy strong (all config keys wired and verified)
- -2: Cat 1 Rule Coverage still PARTIAL FAIL (2 new P1 missing 3-layer files)

## Comparison to Wave 92 carry-forward findings

| Wave 92 finding | Wave 98 status |
|---|---|
| GAP-639 — ABORTED enum orphan beta-access/rules.md (Living Docs sync) | Not re-checked Wave 98 scope; still OPEN |
| GAP-640 — admin-audit domain 3-layer docs missing (META P1) | Pattern RECURRED — preferences/ + email/ also missing 3-layer files (different domains, same class of miss) |

**Pattern signal:** 3-layer completeness violation is recurring across waves. Suggests need for pre-commit hook enforcing 3-file mandate at first commit OR `pre-flight-check domain` skill auto-invocation when new `documents/01-business/{domain}/` folder created. Logged as observation; tracked in Cat 1 P1 findings for both waves; meta-fix candidate for future wave.

## Findings table

| Severity | Description | Gap filed |
|---|---|---|
| **P1 META** | preferences/ missing rules.md + use-cases.md (2/3 layers) | **GAP-662** (new) |
| **P1 META** | email/ missing use-cases.md (1/3 layers) | **GAP-663** (new) |
| **P2 META** | BR-ID javadoc refs missing in 3 new domain Java code | **GAP-664** (new) |
| **P2 (rolled-up)** | 5-attribute coverage 0% on 17 new business rules | GAP-156 (existing quarterly backlog scope expansion) |
| **P2 doc-hygiene** | documents/01-business/README.md index not updated for 3 new domains | **GAP-665** (new) |

Total: **3 new gaps filed (P1×2 + P2×1 + P2-hygiene×1)**, 1 existing backlog scope expansion noted.

## Verdict — Phase 1 BETA gate ≥80

```
Score: 73/100 C+
Gate threshold: 80/100
Verdict: FAIL Phase 1 BETA gate by -7 points
```

**Path to PASS (≥80):**
- Fix GAP-662 (preferences 3-layer completion) → +3 (Cat 1: -6→-3)
- Fix GAP-663 (email use-cases.md) → +2 (Cat 1: PARTIAL→PASS)
- Fix GAP-664 (BR-ID javadoc) → +1 (Cat 1: -1→0)
- Fix GAP-665 (README index sync) → +1 (Cat 4: -1→0)
- Subtotal: +7 → 80 — **PASS by 0**
- Combined GAP-156 quarterly progress → +3-5 → solid PASS 83-85

**Audit-level verdict per `audit-skill-rubric-business-logic-audit.md` §4:** PARTIAL FAIL (Cat 1 has 2 P1 sub-checks failing). NOT audit-level FAIL because zero P0 sub-checks fail. Path to PASS is concrete and short (4 small follow-up gaps).

## References

- Baseline audit: `documents/04-quality/audits/business-logic/2026-05-18-wave-92-business-logic-audit.md`
- Wave 98 closure PR: #1558 (commit 7b2f4301)
- Wave 98 plan: `documents/03-planning/waves/wave-2026-05-18-98-cluster-b-beta-cohort-polish.md`
- Living Docs rule: CLAUDE.md §"Business Logic Documents — 3-Layer Structure"
- 5-attribute rule: `.claude/rules/business-logic-review.md` §2
- Audit rubric: `.claude/rules/audit-skill-rubric-business-logic-audit.md`
- Parent gap: GAP-661 Wave 98 post-closure audit suite

## Log

- **2026-05-19**: Audit shipped. Score 73/100 C+ (+3 vs Wave 92 baseline). 3 new gaps filed (GAP-662/663/664/665). Phase 1 BETA gate FAIL by -7; path to PASS = small follow-up cluster (4 docs gaps + GAP-156 quarterly progress).
