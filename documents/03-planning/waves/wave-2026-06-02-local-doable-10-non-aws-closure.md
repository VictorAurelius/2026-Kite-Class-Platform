---
title: Wave local-doable-10 — non-AWS P0 closure + Wave 9 META/AI continuation
status: draft
created: 2026-06-02
updated: 2026-06-02
waves: [local-doable-10]
gaps: [GAP-730, GAP-656, GAP-530, GAP-823, GAP-867]
---

# Wave local-doable-10 — non-AWS P0 closure + Wave 9 META/AI continuation

**Goal:** Close 3 P0 PARTIAL/OPEN non-AWS gaps + continue Wave 9 META (GAP-823 Phase 3) + AI (GAP-867 Phase 2) via 5 parallel buckets.

**Trigger:** Wave 9 in flight (Batch 1+2 spawned). User direction: "tạm thời chưa pick AWS stack". Direction 1 locked (closure pattern, no outside-in overhead).

**Estimated wall-clock:** ~3-4h parallel (5 Opus bg-agents, staggered 2+2+1). **Dependencies:** D depends Wave 9 B merge; E depends Wave 9 C merge.

---

## 1. Brainstorm

**Q1 (inside-out 3-source pull per `inside-out-completeness-trigger.md`):**
- **gap-status.csv non-AWS P0/P1 OPEN+PARTIAL filter:** GAP-730 P0 OPEN, GAP-656 P0 PARTIAL 80%, GAP-530 P0 PARTIAL 10%, GAP-823 PARTIAL post Wave 9 B, GAP-867 PARTIAL post Wave 9 C
- **inside-out-queue.md:** active items reviewed — Phase 1.5+ scope defer (premium plan, OCR, QR upload); Wave compliance-1 (PDPL), thesis-2 phase-mismatch
- **AskUserQuestion explicit:** User picked Direction 1 (META+AI continuation) — Wave 9 closure pattern continues
- **Outside-in audit:** SKIP per `outside-in-coverage-trigger.md` §4 row 4 — Wave 100% tech-debt closure + scaffold extension (no new architecture decision; Wave 8 D ADR-038 already locked AI direction)

**Q2 (alternatives rejected — Direction 2/3):**
- Direction 2 (GAP-286 + GAP-297 FE OPEN features) — outside-in audit overhead per `outside-in-coverage-trigger.md` §2.1; user pattern = closure not new features
- Direction 3 (mixed 1+1) — hybrid loses Wave 9 dependency chain coherence

**Q3 (risks):**
- **R1 — D + E dependencies:** D (GAP-823 Phase 3) depends Wave 9 B merge cho Instance.slug field; E (GAP-867 Phase 2) depends Wave 9 C merge cho AIClient scaffold. Mitigation: Batch 3 spawn ONLY after Wave 9 B+C merge confirmed (use MCP `pull_request_read` polling)
- **R2 — Bucket A scope (GAP-730 3 endpoints):** OPEN 0%, narrow idempotency on 3 POST controllers (signup + enrollment + beta-request). Risk: scope balloon to wider idempotency framework. Mitigation: agent reads gap §Proposed Fix verbatim; scope = narrow per-endpoint Idempotency-Key header pattern
- **R3 — Bucket C MailHog dependency:** local MailHog reachable required cho live verify. Mitigation: PRE_HANDOFF_PARTIAL trailer if MailHog down + paired follow-up gap
- **R4 — Bucket B near-done (80%):** remaining 20% may need browser visual verify. Mitigation: per `pre-handoff-self-test-completeness.md` §2.1 — UI walk OR PARTIAL with explicit deferred AC
- **R5 — Disjointness:** A kc-core + kitehub-subscription BE controllers / B FE Coordinator / C kitehub-email + MailHog config / D META scripts/audit / E kc-core ai-branding (post Wave 9 C) — verified disjoint at module level

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-730 P0 OPEN Idempotency POST narrow | bg-agent Opus | ~90min | ✅ 3 controllers BE |
| B | GAP-656 P0 80% UI Coordinator widget collision | bg-agent Opus | ~60min | ✅ FE Coordinator |
| C | GAP-530 P0 10% Email-driven flow live verify | bg-agent Opus | ~75min | ✅ kitehub-email + MailHog config + IT |
| D | GAP-823 Phase 3 META detector + sweep | bg-agent Opus | ~75min | ✅ META scripts/audit (depends Wave 9 B merge) |
| E | GAP-867 Phase 2 Resilience4j wiring | bg-agent Opus | ~60min | ✅ kc-core ai-branding (depends Wave 9 C merge) |

**Disjoint check:** A 3-controller BE / B FE / C email IT / D META scripts / E kc-core ai-branding — separate files.

**Cross-layer check per `contract-first-for-cross-layer.md` §2:** NO bucket touches both FE + BE same scope. Bucket 0 Foundation NOT required.

---

## 3. Scope

**Stake tier:** MEDIUM (closure + scaffold extension) → model: **Opus 4.7** mandatory
**Cross-layer?** NO → skip foundation

**Spawn strategy staggered 2+2+1 + dependency-aware:**
- Batch 1: A + B (immediate post plan merge)
- Batch 2: C + D (post Batch 1 first notification; D depends Wave 9 B merge — verify via MCP)
- Batch 3: E (post Batch 2 first notification; E depends Wave 9 C merge — verify via MCP)

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-730 | 🔴 P0 | kc-core signup + enrollment controllers + kitehub-subscription beta-request controller + Idempotency-Key header pattern | Batch 1 parallel |
| 2 | **B** | GAP-656 | 🔴 P0 | FE Coordinator widget collision (Wave 78 follow-up — locate via Glob) | Batch 1 parallel |
| 3 | **C** | GAP-530 | 🔴 P0 | `kitehub/kitehub-email/...` + MailHog config + IT extending Wave 7 A audit | Batch 2 parallel |
| 4 | **D** | GAP-823 Phase 3 | 🔴 P0 META | `scripts/check-instances-triad-trust-pass.sh` (NEW) + Wave 77 cross-flow sweep | Batch 2 parallel (depends Wave 9 B merge) |
| 5 | **E** | GAP-867 Phase 2 | 🟠 P1 | kc-core ai-branding Resilience4j CircuitBreaker wiring on AIClient (mock-based, no live API) | Batch 3 (depends Wave 9 C merge) |

### Bucket A — GAP-730 Idempotency POST narrow (3 endpoints)

- Files: kc-core signup controller + enrollment controller + kitehub-subscription beta-request controller — add `Idempotency-Key` header support per gap §Proposed Fix
- Per `design-patterns.md` §3.5 — Outbox preserved; idempotency = client-side de-dupe via Redis cache
- IT: 3 endpoints × 2 scenarios (first call success + replay with same key returns cached response, not duplicate persist)
- Per `pre-handoff-self-test-completeness.md` §2.9 (background-job sister) — N/A; §2.4 admin-flow N/A; this = POST endpoint idempotency simple class

### Bucket B — GAP-656 UI Coordinator widget collision (Wave 78 follow-up)

- Files: locate FE Coordinator via `Glob` (likely `kitehub/kitehub-frontend/src/...coordinator/**` or similar) — staggered first-login reveal + widget conflict resolution
- Acceptance: 4 remaining AC ticked (out of ~20); per `pre-handoff-self-test-completeness.md` §2.1 — UI walk evidence OR PARTIAL với deferred AC

### Bucket C — GAP-530 Email-driven flow live verify (MailHog)

- Files: extends Wave 7 A audit artifact `documents/04-quality/audits/email/2026-06-02-gap-543-email-content-mailhog-verify.md`; adds end-to-end flow verify per `pre-handoff-self-test-completeness.md` §2.3
- Acceptance: 5 email types flow verified (welcome / verify-email / password-reset / beta-approve / staff-invite) via MailHog API; PRE_HANDOFF_PARTIAL trailer if stack down

### Bucket D — GAP-823 Phase 3 META detector + Wave 77 cross-flow sweep

- Depends Wave 9 B merge (Instance.slug field shipped)
- Files: `scripts/check-instances-triad-trust-pass.sh` (NEW) — detect trust-pass anti-pattern trên instances table (decision-doc citing GAP-823 v1.0.0 rule + paired CI gate); Wave 77 cross-flow sweep audit
- Acceptance: detector script + CI wire + cross-flow sweep findings table; GAP-823 30→100% PARTIAL → DONE

### Bucket E — GAP-867 Phase 2 Resilience4j wiring (mock-based)

- Depends Wave 9 C merge (AIClient interface + GeminiClient scaffold shipped)
- Files: kc-core ai-branding — Resilience4j CircuitBreaker + Retry + Bulkhead annotations on AIClient impl; mock fallback method; unit test verifying breaker opens after N failures (no live API call)
- Acceptance: Resilience4j wired + unit test PASS; GAP-867 60→80% PARTIAL; observability metrics (Phase 3) defer follow-up gap

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `GAP-730` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-730*.md` | 1 file P0 OPEN | ✅ exists |
| `GAP-656` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-656*.md` | 1 file P0 PARTIAL 80% | ✅ exists |
| `GAP-530` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-530*.md` | 1 file P0 PARTIAL 10% | ✅ exists |
| `GAP-823` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-823*.md` | 1 file PARTIAL (Wave 9 B in flight) | ✅ exists |
| `GAP-867` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-867*.md` | 1 file PARTIAL (Wave 9 C in flight) | ✅ exists |
| Wave 9 B merge | dependency | `gh pr list --state merged --search "wave-local-doable-9-bucket-b"` | bg-agent verifies pre-spawn Batch 2 D | 🆕 to-be-verified (Batch 2 spawn gate) |
| Wave 9 C merge | dependency | `gh pr list --state merged --search "wave-local-doable-9-bucket-c"` | bg-agent verifies pre-spawn Batch 3 E | 🆕 to-be-verified (Batch 3 spawn gate) |
| `Idempotency-Key` pattern | new code | `grep -rn "Idempotency-Key" kiteclass/kiteclass-core/src/main/java` | bg-agent verifies | 🆕 to-be-created (Bucket A) |
| `scripts/check-instances-triad-trust-pass.sh` | new script | `ls scripts/check-instances-triad-trust-pass.sh` | not yet exist | 🆕 to-be-created (Bucket D) |
| Resilience4j Circuit Breaker on AIClient | new code | `grep -rn "@CircuitBreaker.*AIClient\|@Retry" kiteclass/kiteclass-core/src/main/java/.../ai/` | bg-agent verifies post Wave 9 C merge | 🆕 to-be-created (Bucket E) |

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kiteclass/kiteclass-core && ./mvnw test -Dtest='*Idempotent*'` + `cd kitehub && ./mvnw -pl kitehub-subscription test` PASS | core-ci + kitehub-subscription-ci |
| B | `pnpm -F kitehub-frontend test --run` + visual verify OR PARTIAL | frontend-ci |
| C | MailHog API `curl http://localhost:8025/api/v2/messages` OR PRE_HANDOFF_PARTIAL trailer | quality-docs.yml (audit doc) |
| D | `bash scripts/check-instances-triad-trust-pass.sh` self-test PASS + workflow YAML lint clean | quality-rules-skills.yml + script-quality |
| E | `cd kiteclass/kiteclass-core && ./mvnw test -Dtest='AIClient*CircuitBreaker*'` PASS | core-ci |

---

## 6. Agent Spawn Pattern (staggered + dependency-aware)

Per `agent-model-opus-default.md` + `agent-background-spawn-default.md` + Wave 7+8+9 rate-limit lesson:
- All buckets `model: "opus"` + `run_in_background: true` + `isolation: "worktree"`
- Batch 1 (A+B): immediate post Wave 10 plan PR merge
- Batch 2 (C+D): post Batch 1 first completion + verify Wave 9 B merged (D blocker)
- Batch 3 (E): post Batch 2 first completion + verify Wave 9 C merged (E blocker)
- RELATIVE paths per `feedback_worktree_absolute_path_contamination.md`
- MCP `pull_request_read get_status` polling cho dependency verify

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `wave-closure-scope-completeness.md` + `post-merge-sync-completeness.md` + `post-wave-cleanup.md`:
- Each bucket PR updates affected gap file Log + status + CSV row
- Wave plan frontmatter `status: complete` flip in closure PR
- `wave-history.jsonl` append (Rule 15)
- **Scope-Completeness Reconciliation table** per `wave-closure-scope-completeness.md` §3
- Sub-gaps filed for any deferral (Bucket E Phase 3 observability metrics)
- Run `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md`

---

## 8. Log

- **2026-06-02** (draft): Plan created. 5 non-AWS buckets per user direction "tạm thời chưa pick AWS stack" + Direction 1 (META+AI continuation). 3 Feature-P0 OPEN/PARTIAL + 1 META-P0 + 1 P1. Staggered 2+2+1 spawn với dependency gating D/E on Wave 9 B/C merge. Outside-in SKIP per `outside-in-coverage-trigger.md` §4 row 4.
