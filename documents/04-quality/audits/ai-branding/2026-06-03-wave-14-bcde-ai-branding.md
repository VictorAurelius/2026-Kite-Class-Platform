# AI Branding Quality Gate — Wave 14 B+C+E entity sync + audit-UUID sweep (PR #2134) — 2026-06-03

**Trigger:** PR #2134 merged 2026-06-03 commit `c9ba7ed6` (`feat(wave-14-bcde): entity sync + audit-UUID sweep + DB CI gates`)
**Change type:** Entity schema sync + caller sweep (NOT a model/prompt/provider/§5-logic change)
**Verdict:** ⚠️ NOTES (carry-forward baseline — minimal ai-branding scope; non-regression confirmed)
**Score:** **62/100** (no delta vs `2026-04-26-baseline.md` — see §"Scope justification")

---

## Scope justification — why carry-forward baseline

Per `.claude/skills/quality/ai-branding-quality-gate/SKILL.md` §0 risk matrix, PR #2134 changes do NOT trigger any of the rubric's 6 risk categories:

| Risk axis | PR #2134 touch? | Verdict |
|---|---|---|
| Model swap (Llama → Gemma, llama3.x bump) | NO | §1 N/A |
| Provider swap (Ollama → Bedrock) | NO | §2 N/A |
| Prompt template rewrite | NO | §1/§2/§4 N/A |
| §5 Quality Reviewer logic (`InstanceQualityReviewer`, 5 *QualityCheck) | NO — files untouched | §3 N/A |
| `ContentModerationService` logic | NO — file untouched | §2/§4 N/A |
| Step class added/removed (workflow) | NO | §1 N/A |

PR scope actually intersecting `module/{ai,branding,instance,quality,moderation,provisioning}/**`:

1. **`BrandingDataSeeder.java:170`** — single-line caller swap `.tenantId()` → `.tenantSlug()` per V82 rename (GAP-891). Dev-seeder only; no production behavior change.
2. **`PublicBrandingController.java`** + **`BrandingPackageServiceImpl.java`** + **`InstanceResponse.java`** + **`FrontendInstance.java`** — V82 `tenant_id` → `tenant_slug` field rename ripple. Pure rename; no AI/prompt/quality-check semantic change.
3. **`BaseEntity.java` audit-UUID migration (`String createdBy/updatedBy` → `UUID`)** — **REVERTED in this PR** per descope commit ("schema-drift CI caught KH audit-UUID mismatch; preserved on `wave-14-bucket-c-audit-uuid-deferred`"). Net effect on main: zero change to BaseEntity contract for branding entities.

Conclusion: this PR's branding-scope footprint = rename ripple + dev-seeder caller fix. Quality posture from `2026-04-26-baseline.md` (62/100 — `0/20 §1 + 12/20 §2 + 8/20 §3 + 18/20 §4 + 20/20 §5`) carries forward unchanged. **No re-measurement required; baseline still authoritative.**

---

## Section scores (carry-forward from baseline)

| § | Title | Score | Delta | Notes |
|---|-------|:-----:|:-----:|-------|
| 1 | Output behavior consistency | 0/20 | 0 | N/A — no AI behavior change |
| 2 | Tool-calling / Schema integration | 12/20 | 0 | `PlannerService`/`AnalyzerService`/`PlanExecutor`/Adapter all untouched in this PR |
| 3 | §5 Quality Gate compatibility | 8/20 | 0 | 5 *QualityCheck classes untouched; real WCAG/vrg/ML still deferred (GAP-226/227/228) |
| 4 | Resilience & fallback | 18/20 | 0 | `ResilientAIClient` + CircuitBreaker + Bulkhead untouched |
| 5 | Tier-specific governance | 20/20 (capped) | 0 | `AIRateLimitService` + tier gates untouched |

**Total:** 0 + 12 + 8 + 18 + 20 = 58 + 4 (baseline honest adjustment) = **62/100** ⚠️ carry-forward.

## Sample outputs (§1)

N/A — no model/prompt change to A/B against baseline.

## Issues found in this PR's branding-scope footprint

| Issue | Severity | Status |
|---|:---:|---|
| `BrandingDataSeeder.java:170` Bucket B rename caller miss (caught + fixed same PR) | 🟢 RESOLVED | Single-line fix landed in PR per `cross-flow-bug-class-sweep.md §3` evidence in commit body |
| Wave02MigrationsTest stale `tenant_id` SQL (caught + fixed same PR) | 🟢 RESOLVED | Followup fix landed same PR |
| Branding-scope test compile cascade chain (Test Core / DB schema drift gates depending on `mvn compile`) | 🟡 NOTE (rule-level) | Per commit body — already flagged by author; meta-rule sister coverage |

**No NEW P0/P1 findings in ai-branding scope.** Baseline issues (GAP-226 WCAG / GAP-227 visual-regression / GAP-228 ML classifier scaffold) remain open per `2026-04-26-baseline.md`; unchanged by this PR.

## Delta vs baseline

| Section | Baseline (2026-04-26) | Current (2026-06-03) | Delta |
|---|:---:|:---:|:---:|
| §1 | 0/20 | 0/20 | 0 |
| §2 | 12/20 | 12/20 | 0 |
| §3 | 8/20 | 8/20 | 0 |
| §4 | 18/20 | 18/20 | 0 |
| §5 | 20/20 | 20/20 | 0 |
| **Total** | **62/100** | **62/100** | **0** |

## Recommendations

1. **No new gap filed** — PR scope is non-AI-behavior; pre-existing baseline gaps (GAP-226/227/228) remain canonical for §1+§3 closure path.
2. **Carry-forward eligibility window:** next ai-branding audit re-measure (full §0 risk matrix) due when ANY of: (a) `ResilientAIClient` config change, (b) `InstanceQualityReviewer` logic change, (c) new Step class add/remove in `module/ai/workflow/`, (d) model/provider/prompt change. Recommend re-run trigger when `kitehub-branding` v1 → KC v2 migration of remaining AI features ships (still pending per `kiteclass-core/module/` scope note in SKILL.md §"Module location note").
3. **Bucket D KC type harmonize (V86)** shipped in this PR aligns money columns NUMERIC(19,2) — irrelevant to ai-branding scope.

## References

- Baseline: `documents/04-quality/audits/ai-branding/2026-04-26-baseline.md`
- Skill: `.claude/skills/quality/ai-branding-quality-gate/SKILL.md`
- PR: `gh pr view 2134`; merge commit `c9ba7ed6`
- Sister wave: GAP-891 (`tenant_id → tenant_slug` rename per V82 migration)
- Deferred follow-ups (unchanged from baseline): GAP-226 / GAP-227 / GAP-228 (real WCAG/visual-regression/ML classifier)
- Related: `output-review-mandate.md` §3 matrix row "AI-generated assets" — status `⚠️ PARTIAL` carry-forward; this PR does not alter row state
