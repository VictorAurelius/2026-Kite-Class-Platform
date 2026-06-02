---
title: Wave local-doable-9 — non-AWS P0/P1 batch (workflow + META residual + AI scaffold + docs + PDPL FE)
status: draft
created: 2026-06-02
updated: 2026-06-02
waves: [local-doable-9]
gaps: [GAP-870, GAP-823, GAP-867, GAP-693, GAP-353]
---

# Wave local-doable-9 — non-AWS P0/P1 batch

**Goal:** Ship 5 disjoint non-AWS gaps — unblock PR #2070+#2064 via workflow fix, close Wave 8 META residuals (instances code + AI Phase 1 scaffold), ship AWS rebuild SOP playbook docs, advance PDPL Cookie banner FE 73→100%.

**Trigger:** Wave 7+8 closure shipped (PR #2078). User direction: filter AWS-touching gaps until ready. Per Wave 8 C state-check correction — AWS GAP-612 actually resolved 2026-05-26 nhưng user defer AWS scope.

**Estimated wall-clock:** ~3-4h parallel (5 Opus bg-agents, staggered 2+2+1 per Wave 7+8 lesson).

---

## 1. Brainstorm

**Q1 (inside-out 3-source pull per `inside-out-completeness-trigger.md`):**
- **gap-status.csv non-AWS P0/P1 OPEN+PARTIAL filter:** GAP-870 P1 (NEW Wave 7+8 batch), GAP-823 PARTIAL 30%, GAP-867 PARTIAL 40%, GAP-693 P0 PARTIAL 70%, GAP-353 P0 PENDING 73%
- **inside-out-queue.md:** PDPL items align với GAP-353 (29 days countdown per Wave 8 C consolidation) → priority signal
- **AskUserQuestion explicit:** User picked option (a) — 5 buckets accepted as-is; user directive "tạm thời chưa pick các gaps cần động đến aws stack"
- **Outside-in audit:** SKIP per `outside-in-coverage-trigger.md` §4 row 4 — Wave 100% scope = META residual + scaffold + docs + FE compliance closure (no new architecture decision)

**Q2 (alternatives rejected):**
- GAP-502/756/608/610 — AWS-touching, defer per user direction
- GAP-730 P0 Idempotency narrow — alternative to C; rejected vì C unlocks ADR-038 implementation path (higher leverage)
- GAP-656 P0 80% UI Coordinator widget collision — alternative to D; D priority lower (docs not code)

**Q3 (risks):**
- **R1 — Bucket B GAP-823 Phase 1+2 scope ambiguity:** Wave 8 A deferred "Instance.slug field + repository slug method + service normalizer wiring + IT" to Wave meta-9. Risk: ambiguous AC. Mitigation: agent reads gap §Proposed Fix verbatim + Wave 8 A closure §9 reconciliation note for context
- **R2 — Bucket C GAP-867 Phase 1 impl scope balloon:** ADR-038 design phase 40% done; Phase 1 = AIClient + GeminiClient scaffold WITHOUT live API calls + WITHOUT Resilience4j/Micrometer/k6 (defer). Mitigation: agent strict scope = interface + 1 impl class + 1 unit test + config skeleton; runtime smoke = mock-based
- **R3 — Bucket A GAP-870 workflow change:** `.github/workflows/*.yml` edit per `docs-only-pr-auto-merge.md` §3 out-of-scope → manual review. Mitigation: PR opens, CI runs, user reviews manually
- **R4 — Bucket E GAP-353 PDPL scope:** 8/11 AC done (73%); 3 deepening items GAP-353b followups. Risk: scope drift to b followups. Mitigation: agent scope = remaining 3 AC of GAP-353 main; b followups defer separate gap
- **R5 — Disjointness:** A `.github/workflows` / B kitehub-subscription Java / C kc-core or new ai-external module / D docs runbook / E FE marketing kits → verified disjoint at module+file level

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-870 P1 workflow Maven cache | bg-agent Opus | ~45min | ✅ `.github/workflows/*.yml` |
| B | GAP-823 Phase 1+2 instances code residual | bg-agent Opus | ~90min | ✅ kitehub-subscription instance code |
| C | GAP-867 Phase 1 scaffold (AIClient + GeminiClient) | bg-agent Opus | ~90min | ✅ kc-core ai-branding OR new module |
| D | GAP-693 AWS rebuild SOP playbook docs | bg-agent Opus | ~60min | ✅ `documents/05-guides/operations/` docs |
| E | GAP-353 PDPL Cookie Consent Banner FE | bg-agent Opus | ~75min | ✅ FE marketing kits |

**Disjoint check:** A workflow / B BE subscription / C BE kc-core ai-branding / D docs runbook / E FE marketing — separate top-level paths.

**Cross-layer check per `contract-first-for-cross-layer.md` §2:** NO bucket touches both FE + BE same scope. Bucket 0 Foundation NOT required (Bucket C is BE-only scaffold; FE consumes future wave).

---

## 3. Scope

**Stake tier per `wave-pack-planner/SKILL.md` §Step 4.6:** MEDIUM (META residual closure + new scaffold + workflow fix + compliance FE) → model: **Opus 4.7** mandatory per `agent-model-opus-default.md`
**Cross-layer?** NO → skip foundation bucket

**Spawn strategy per Wave 7+8 rate-limit lesson:** Staggered 2+2+1:
- Batch 1: A + B (immediate)
- Batch 2: C + D (post Batch 1 first notification)
- Batch 3: E (post Batch 2 first notification)

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-870 | 🟠 P1 | `.github/workflows/<security>.yml` (locate) + maven cache step | Batch 1 parallel |
| 2 | **B** | GAP-823 | 🔴 P0 META residual | `kitehub/kitehub-subscription/src/main/java/.../instance/**` + Flyway V + IT | Batch 1 parallel |
| 3 | **C** | GAP-867 | 🟠 P1 | `kiteclass/kiteclass-core/.../ai/**` OR new `kitehub-ai-external/` skeleton + AIClient interface + GeminiClient impl + unit test | Batch 2 parallel |
| 4 | **D** | GAP-693 | 🔴 P0 | `documents/05-guides/operations/aws-rebuild-sop.md` (NEW) + cross-links | Batch 2 parallel |
| 5 | **E** | GAP-353 | 🔴 P0 | `kitehub/kitehub-frontend/src/...marketing/**` + `kiteclass/kiteclass-frontend/src/...marketing/**` cookie consent banner + remaining 3 AC | Batch 3 |

### Bucket A — GAP-870 workflow Maven cache pre-populate

- Files: locate Trivy workflow via `Glob ".github/workflows/*security*.yml"` HOẶC `.github/workflows/*scan*.yml`
- Approach: add `mvn dependency:resolve` step + `actions/cache@v4` với `key: maven-${{ hashFiles('**/pom.xml') }}` TRƯỚC Trivy step
- Acceptance: workflow edited; PR description links to GAP-870; rerun trigger verifies 0 `429` errors; gap → DONE; sister effect = PR #2070 + #2064 unblocked

### Bucket B — GAP-823 Phase 1+2 instances code residual

- Read GAP-823 §Proposed Fix Phase 1+2 verbatim (deferred to Wave meta-9 per Wave 8 A closure §9 reconciliation)
- Files: `kitehub/kitehub-subscription/src/main/java/.../instance/Instance.java` (add `slug` field) + `InstanceRepository.java` (slug method) + `InstanceService.java` (normalizer wiring) + Flyway V migration + IT
- Acceptance: Instance.slug NOT NULL + DB column + repository findBySlug + service normalizer + IT verifying slug unique constraint; per `design-patterns.md` §3.12 triad atomicity; per `api-contract-change-caller-sweep.md` §3 caller sweep
- Per `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist applies (subscription endpoints)

### Bucket C — GAP-867 Phase 1 scaffold (AIClient + GeminiClient)

- Strict scope: interface + 1 impl + unit test + config; NO live API calls + NO Resilience4j + NO Micrometer + NO k6
- Files: `kiteclass-core/.../ai/client/AIClient.java` (interface per `design-patterns.md` §3.10 + ADR-038 §Decision) + `GeminiAIClient.java` (skeleton with method stubs returning mock data) + `AIClientConfig.java` (config skeleton wiring `ai.provider.primary=gemini`) + `AIClientTest.java` (Mockito unit verifying interface contract)
- Acceptance: code compiles + unit test passes; ADR-038 status PROPOSED → ACCEPTED post merge; gap → PARTIAL 60% (40→60); follow-up impl phases tracked separate gap
- Per `outside-in-coverage-trigger.md` §2.1 architecture keyword — borderline NEW scope; rule applies BUT design phase shipped Wave 8 D so this bucket = pure execution per locked decision

### Bucket D — GAP-693 AWS rebuild SOP playbook

- Files: NEW `documents/05-guides/operations/aws-rebuild-sop.md` — 13 steps + 5 gates + 8 failure-mode prevention per gap §Proposed Fix
- Acceptance: playbook complete + cross-links to runbooks (terraform-apply / start-stack / stop-stack / fetch-secrets / cloudwatch dashboards); gap → DONE
- Note: docs describe AWS rebuild process; agent does NOT touch AWS stack itself

### Bucket E — GAP-353 PDPL Cookie Consent Banner — KH + KC marketing kits

- Read gap §Acceptance Criteria verbatim — 3 remaining AC of 11 (8/11 done per Wave br-4 PR #1782 + Wave beta-prep-1 PR #1874 + fix #1939)
- Files: `kitehub/kitehub-frontend/src/...marketing/**` + `kiteclass/kiteclass-frontend/src/...marketing/**` cookie banner components
- Acceptance: 3 remaining AC ticked; component meets PDPL 2023 + Decree 13 disclosure requirements; visual verify in browser OR PRE_HANDOFF_PARTIAL trailer; gap → DONE; b followups (multi-device + audit chain + server API audit log) defer GAP-353b sister gaps (separate)

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `GAP-870` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-870*.md` | 1 file P1 OPEN (filed this batch) | ✅ exists |
| `GAP-823` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-823*.md` | 1 file P0 META PARTIAL 30% | ✅ exists |
| `GAP-867` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-867*.md` | 1 file P1 PARTIAL 40% | ✅ exists |
| `GAP-693` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-693*.md` | 1 file P0 PARTIAL 70% | ✅ exists |
| `GAP-353` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-353-pdpl*.md` | 1 file P0 PENDING 73% (main; b/c/d followups separate) | ✅ exists |
| `.github/workflows/<security>.yml` | workflow file | `find .github/workflows -name "*security*.yml" -o -name "*scan*.yml"` | bg-agent locates | 🆕 to-be-verified (Bucket A) |
| `Instance.java` (kitehub-subscription) | entity class | `grep -rn "class Instance" kitehub/kitehub-subscription/src/main/java` | bg-agent verifies (Wave 8 A noted exists) | ✅ exists (Wave 8 A confirmed) |
| `AIClient` interface | new code | `grep -rn "interface AIClient" kiteclass/kiteclass-core/src/main/java` | bg-agent verifies | 🆕 to-be-created (Bucket C) |
| `aws-rebuild-sop.md` | new doc | `ls documents/05-guides/operations/aws-rebuild-sop.md` | not yet exist | 🆕 to-be-created (Bucket D) |
| Cookie banner FE component | FE component | `grep -rn "CookieBanner\|cookie-consent" kitehub/kitehub-frontend/src kiteclass/kiteclass-frontend/src` | bg-agent verifies (Wave br-4 + beta-prep-1 + fix #1939 shipped) | ✅ exists partial (Bucket E completes 3 AC) |

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/<file>.yml'))"` + dry-run trigger workflow on test PR | quality-code.yml (workflow edit) |
| B | `cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings` + entity-mapper-consistency CI | core-ci + entity-mapper-consistency |
| C | `cd kiteclass/kiteclass-core && ./mvnw test -Dtest='AIClient*'` PASS + compile clean | core-ci |
| D | `bash scripts/check-readme-freshness.sh` + cross-link grep verify | quality-docs.yml |
| E | `pnpm -F kitehub-frontend test --run` + visual verify in browser OR PRE_HANDOFF_PARTIAL | frontend-ci + kitehub-frontend-ci |

---

## 6. Agent Spawn Pattern (staggered 2+2+1)

Per `agent-model-opus-default.md` + `agent-background-spawn-default.md` + `feedback_parallel_agent_strategy.md` + Wave 7+8 rate-limit lesson:
- All buckets `model: "opus"` + `run_in_background: true` + `isolation: "worktree"`
- Batch 1 (A+B) immediate
- Batch 2 (C+D) post Batch 1 first completion notification
- Batch 3 (E) post Batch 2 first completion notification
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `wave-closure-scope-completeness.md` + `post-merge-sync-completeness.md` + `post-wave-cleanup.md`:
- Each bucket PR updates affected gap file Log + status + CSV row
- Wave plan frontmatter `status: complete` flip in closure PR
- `wave-history.jsonl` append (Rule 15)
- **Scope-Completeness Reconciliation table** per `wave-closure-scope-completeness.md` §3
- Sub-gaps filed for any deferral (Bucket C Phase 2+3 impl + Bucket E b followups)
- Run `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md`
- Bucket A closure: re-trigger PR #2070 + #2064 Security Scan; expect green

---

## 8. Log

- **2026-06-02** (draft): Plan created. 5 non-AWS buckets per user direction "tạm thời chưa pick các gaps cần động đến aws stack". 1 P1 workflow + 1 P0 META residual + 1 P1 scaffold + 1 P0 docs + 1 P0 FE compliance. Staggered 2+2+1 spawn. Outside-in audit SKIP per `outside-in-coverage-trigger.md` §4 row 4.
