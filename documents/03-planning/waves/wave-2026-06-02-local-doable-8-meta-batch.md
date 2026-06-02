---
title: Wave local-doable-8 — META P0 batch + Wave 6 follow-up + gateway IDOR fix
status: complete
created: 2026-06-02
updated: 2026-06-02
completed_at: 2026-06-02
waves: [local-doable-8]
gaps: [GAP-823, GAP-788, GAP-622, GAP-867, GAP-814]
---

# Wave local-doable-8 — META P0 batch + Wave 6 follow-up + gateway IDOR fix

**Goal:** Close 3 META P0 force-multipliers + 1 P0 75% PARTIAL gateway IDOR + 1 P1 Wave 6 follow-up via 5 parallel buckets.

**Trigger:** Wave 7 complete (5/5 PRs merged); AWS prod still suspended (GAP-612) → continue local-doable wave series. Priority matrix per `meta-gap-priority.md` §3 favors META P0 lead.

**Estimated wall-clock:** ~3-4h parallel (5 Opus bg-agents, staggered 2+2+1 per Wave 7 rate-limit lesson); longest-bucket ~90min (Bucket E gateway code + IT verify).

---

## 1. Brainstorm

**Q1 (inside-out 3-source pull per `inside-out-completeness-trigger.md`):**
- **gap-status.csv P0/P1 OPEN+PARTIAL filter:** 5 candidates picked (GAP-823/788/622/867/814); other P0 candidates (GAP-117/286/297/353/502/530/533/566/567/608/610/693) AWS-blocked or external dep-blocked → defer Wave 9+ post-AWS-restore
- **inside-out-queue.md:** 5 active items reviewed; user-manual P2 / thesis Phase 3 / PDPL Wave compliance-1 / QR upload Phase 1.5 / OCR Phase 1.5 — all phase-mismatch hoặc explicit defer
- **AskUserQuestion explicit:** User locked option (a) — proposed 5 buckets accepted as-is
- **Outside-in audit:** SKIP per `outside-in-coverage-trigger.md` §4 row 4 (Wave 100% internal — META governance + bug fix scope). Bucket D (GAP-867 AI external API) borderline architecture-decision (§2.1 keyword "integration"), nhưng design phase shipped per existing Wave 6 rescope memo (GAP-005 → external API only) — không new architecture decision

**Q2 (alternatives rejected):**
- GAP-868 P1 META end-session skill — NOT FILED in CSV/filesystem (verified via `bash scripts/query-gaps.sh "" GAP-868` returns empty)
- GAP-608 P0 ses:SendEmail IAM — DEFERRED Wave 9+ post-AWS-restore
- GAP-610 P0 95% beta-signup prod — same defer
- GAP-117 P0 Backup Restore Drill — same defer

**Q3 (risks):**
- **R1 — Bucket A GAP-823 META scope ambiguity:** "instances table triad drift + trust-pass anti-pattern" requires both rule write AND code-sweep. Risk: agent scope drift. Mitigation: agent reads gap file §Proposed Fix verbatim; scope minimal v1 rule + 1 code-sweep round
- **R2 — Bucket E GAP-814 gateway filter changes shared SecurityConfig:** risk regression to existing gateway filters. Mitigation: agent adds NEW `RemoveRequestHeader` global filter + IT verifies all existing routes still pass; per `api-contract-change-caller-sweep.md` §3 sweep callers
- **R3 — Bucket D GAP-867 design scope:** AI external API integration design — could balloon if agent tries full impl. Mitigation: scope = ADR + scaffold + observability plan; actual impl defer follow-up gap
- **R4 — Disjoint guarantee:** A (kitehub-subscription/instances), B (audit docs), C (planning docs), D (ai-branding new + ADR), E (kitehub-gateway) — verified disjoint at module level

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-823 P0 META | bg-agent Opus | ~75min | ✅ kitehub-subscription/instances + new rule + code-sweep |
| B | GAP-788 P0 META | bg-agent Opus | ~60min | ✅ audit docs (retro walk batch) |
| C | GAP-622 P0 META | bg-agent Opus | ~60min | ✅ planning consolidation (docs) |
| D | GAP-867 P1 | bg-agent Opus | ~75min | ✅ ai-branding new module + ADR + observability plan |
| E | GAP-814 P0 75% | bg-agent Opus | ~90min | ✅ kitehub-gateway filter + IT |

**Disjoint check:** A `kitehub-subscription/src/main/java/.../instance/**` + new rule, B `documents/04-quality/audits/retro/**`, C `documents/03-planning/roadmap/**` + ROADMAP, D `documents/02-architecture/adr/**` + `documents/01-business/ai-branding/**` + maybe `kitehub-ai-external/` skeleton, E `kitehub/kitehub-gateway/src/main/.../filter/**` + SecurityConfig. No file overlap.

**Cross-layer check per `contract-first-for-cross-layer.md` §2:** NO bucket touches both FE + BE same scope. Bucket 0 Foundation NOT required.

---

## 3. Scope

**Stake tier per `wave-pack-planner/SKILL.md` §Step 4.6:** MEDIUM-HIGH (META rule shipping + gateway IDOR fix touch governance + security) → model: **Opus 4.7** mandatory per `agent-model-opus-default.md`
**Cross-layer?** NO → skip foundation

**Spawn strategy per Wave 7 rate-limit lesson:** Staggered batches:
- Batch 1: A + B (fire immediately)
- Batch 2: C + D (fire after batch 1 confirmation or +90s delay)
- Batch 3: E (fire after batch 2)

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-823 | 🔴 P0 META | `kitehub/kitehub-subscription/src/main/java/.../instance/**` + `.claude/rules/instances-table-triad-discipline.md` (NEW) | Batch 1 parallel |
| 2 | **B** | GAP-788 | 🔴 P0 META | `documents/04-quality/audits/retro/2026-06-02-wave-80-plus-retro-walk-batch.md` (NEW) + per-wave retro-walk artifacts | Batch 1 parallel |
| 3 | **C** | GAP-622 | 🔴 P0 META | `documents/03-planning/roadmap/pre-launch-readiness-consolidation.md` (NEW) + ROADMAP § sync | Batch 2 parallel |
| 4 | **D** | GAP-867 | 🟠 P1 | `documents/02-architecture/adr/ADR-NNN-ai-external-provider-strategy.md` (NEW) + `documents/01-business/ai-branding/` updates + observability plan | Batch 2 parallel |
| 5 | **E** | GAP-814 | 🔴 P0 75% | `kitehub/kitehub-gateway/src/main/.../filter/` + `application.yml` `default-filters` + new gateway IT | Batch 3 |

### Bucket A — GAP-823 instances table triad drift + trust-pass anti-pattern

- Files: kitehub-subscription instance entity + Flyway migration + mapper (sweep findings); NEW rule `.claude/rules/instances-table-triad-discipline.md` codifying entity↔migration↔mapper invariants for instances table specifically
- Acceptance: rule written với reviewer-checklist + worked self-test on originating drift incident; code-sweep audit table in PR body listing FIX/EXEMPT verdict per site; gap file → DONE

### Bucket B — GAP-788 META Wave 80+ retro-walk batch

- Files: NEW audit artifact `documents/04-quality/audits/retro/2026-06-02-wave-80-plus-retro-walk-batch.md` cataloging Wave 80+ DONE features missing retroactive feature-ship-runtime-walk evidence; identify high-priority candidates for follow-up walk gaps
- Acceptance: batch retro doc lists 10+ Wave 80+ DONE features + risk/priority categorization; gap file → DONE (or PARTIAL if execution work split out)

### Bucket C — GAP-622 META pre-launch readiness consolidation

- Files: NEW `documents/03-planning/roadmap/pre-launch-readiness-consolidation.md` consolidating Phase 1 BETA gate (PDPL + AWS GAP-612 + 27 P0 blockers); cross-link to ROADMAP §🎯 + ROADMAP update
- Acceptance: consolidation doc lists all blockers + dependencies + estimated unblock paths; gap file → DONE

### Bucket D — GAP-867 AI external API integration observability + load verify

- Files: NEW ADR `documents/02-architecture/adr/ADR-NNN-ai-external-provider-strategy.md` (Gemini vs OpenAI cost/latency/observability) + `documents/01-business/ai-branding/api-contract.md` update (provider-agnostic interface) + observability plan (metrics/logs/cost tracking) + scaffold optional
- Acceptance: ADR ACCEPTED status + business docs sync + observability plan documented; gap file → DONE OR PARTIAL với follow-up scaffold gap
- Note: per Wave 6 rescope (PR #2060), AI strategy = external API ONLY (Ollama deferred); this bucket consolidates design

### Bucket E — GAP-814 gateway X-Tenant-Id strip (cross-tenant IDOR P0)

- Files: `kitehub/kitehub-gateway/src/main/resources/application.yml` add `RemoveRequestHeader=X-Tenant-Id, X-User-Id` to `default-filters` (global) + verify `TenantResolverGatewayFilterFactory` covers ALL tenant-scoped routes (not just instance-apis/staff-invitations/onboarding-progress) + new IT verifying client-sent headers stripped
- Tests: NEW `kitehub-gateway/src/test/.../filter/TenantHeaderStripIT.java` — assert client header `X-Tenant-Id: <attacker-uuid>` stripped before reach core
- Acceptance: gateway strips client headers globally; tenant-context only from gateway-resolved value; IT passes; per `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist; gap file → DONE
- Per `api-contract-change-caller-sweep.md` §3: sweep TenantResolver routes; verify no existing route depends on client-sent X-Tenant-Id

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `GAP-823` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-823*.md` | 1 file OPEN P0 META | ✅ exists |
| `GAP-788` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-788*.md` | 1 file OPEN P0 | ✅ exists |
| `GAP-622` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-622*.md` | 1 file OPEN P0 | ✅ exists (15d old → §2.8 state-check at fix time) |
| `GAP-867` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-867*.md` | 1 file OPEN P1 | ✅ exists |
| `GAP-814` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-814*.md` | 1 file PARTIAL P0 75% | ✅ exists |
| `instances` table | DB entity | `grep -rn "class Instance\|@Entity.*instance" kitehub/kitehub-subscription/src/main/java` | bg-agent verifies | 🆕 to-be-verified (Bucket A) |
| `TenantResolverGatewayFilterFactory` | gateway filter | `grep -rn "TenantResolverGatewayFilterFactory" kitehub/kitehub-gateway/src/main/java` | bg-agent verifies | 🆕 to-be-verified (Bucket E) |
| `default-filters` in application.yml | gateway config | `grep -n "default-filters" kitehub/kitehub-gateway/src/main/resources/application.yml` | bg-agent verifies | 🆕 to-be-verified (Bucket E) |
| `.claude/rules/instances-table-triad-discipline.md` | NEW rule | `ls .claude/rules/instances-table-triad-discipline.md` | not yet exist | 🆕 to-be-created (Bucket A) |
| `documents/02-architecture/adr/ADR-NNN-ai-external-provider-strategy.md` | NEW ADR | `ls documents/02-architecture/adr/ADR-*-ai-external*.md` | not yet exist (number TBD by agent) | 🆕 to-be-created (Bucket D) |

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `bash scripts/check-rule-frontmatter.sh` + entity-mapper drift script | quality-rules-skills.yml + entity-mapper-consistency |
| B | docs-only — `bash scripts/check-audits-index-csv.sh` | quality-docs.yml |
| C | docs-only — `bash scripts/check-readme-freshness.sh` + ROADMAP grep verify | quality-docs.yml |
| D | docs-only initial (ADR + business docs); if scaffold added: `cd kitehub && ./mvnw -pl <new-module> compile` | quality-docs.yml + (conditional) core-ci |
| E | `cd kitehub && ./mvnw -pl kitehub-gateway verify -P strict-warnings` + new IT passing | gateway-ci |

---

## 6. Agent Spawn Pattern (staggered per Wave 7 lesson)

Per `agent-model-opus-default.md` + `agent-background-spawn-default.md` + `feedback_parallel_agent_strategy.md`:
- All buckets `model: "opus"` + `run_in_background: true` + `isolation: "worktree"`
- **Staggered batches (NEW)** per Wave 7 rate-limit storm lesson:
  - Batch 1: A + B (immediate)
  - Batch 2: C + D (post Batch 1 first notification OR +90s delay)
  - Batch 3: E (post Batch 2 first notification)
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequentially per `docs-only-pr-auto-merge.md` (B/C/D docs-only auto-merge; A code+rule manual; E gateway code manual)

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `wave-closure-scope-completeness.md` + `post-merge-sync-completeness.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md`:
- Each bucket PR updates affected GAP file Log + status + CSV row
- ROADMAP §🚀 Next Action updated in closure PR
- Wave plan frontmatter `status: complete` flip in closure PR
- `wave-history.jsonl` append in closure PR (Rule 15)
- **Scope-Completeness Reconciliation table** in closure PR body per `wave-closure-scope-completeness.md` §3
- Sub-gaps filed for any deferral (Bucket D scaffold work likely defers)
- Run `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md`

---

## 8. Log

- **2026-06-02** (draft): Plan created. 3 META P0 + 1 Feature P0 PARTIAL + 1 Feature P1 follow-up. Staggered 2+2+1 spawn strategy to avoid Wave 7 rate-limit storm. Outside-in audit SKIP per `outside-in-coverage-trigger.md` §4 row 4.
- **2026-06-02** (complete): All 5 buckets shipped via 5 PRs (all docs-only auto-merged). Notable findings: Bucket E state-check win (IDOR fix already shipped Wave tenant-domain-1); Bucket C state-check correction (AWS GAP-612 actually RESOLVED 2026-05-26 + 24 P0 active not 27); Bucket A META rule shipped + Phase 1+2 code residual defer Wave meta-9.

---

## 9. Scope-Completeness Reconciliation (per `wave-closure-scope-completeness.md` §3)

| # | Plan §3 Scope item | PR | Verdict | Follow-up |
|---|---|---|---|---|
| 1 | Bucket A — GAP-823 P0 META instances table triad drift + trust-pass anti-pattern | #2072 ✅ merged | 🟡 PARTIAL 30% | META rule `instances-table-triad-discipline.md` v1.0.0 shipped + 8-site sweep (1 FIX-tracked V40, 7 EXEMPT). Phase 1+2 code residual (Instance.slug field + IT) defer Wave meta-9 |
| 2 | Bucket B — GAP-788 P0 META Wave 80+ retro-walk batch | #2073 ✅ merged | ✅ DONE | 92-feature catalog (16 ✅ / 37 ⚠️ / 34 ❌); post-rule improvement trend confirmed (50% PARTIAL post-2026-05-28); 11 NO_EVIDENCE candidates queued Phase 2 BETA retro-walk batch |
| 3 | Bucket C — GAP-622 P0 META pre-launch readiness consolidation | #2075 ✅ merged | ✅ DONE | Consolidation doc 8 sections + 2 Mermaid diagrams; state-check corrections: AWS GAP-612 RESOLVED 2026-05-26, 24 P0 active, PDPL 29d countdown, Quality 90/110 B+ PASS gate |
| 4 | Bucket D — GAP-867 P1 AI external API ADR + observability | #2076 ✅ merged | 🟡 PARTIAL 40% | NEW ADR-038 (Gemini Free Tier primary + OpenAI fallback) + observability plan + api-contract update; implementation (AIClient impls + Resilience4j wiring + Micrometer metrics + k6 execution) defer future wave |
| 5 | Bucket E — GAP-814 P0 75% gateway X-Tenant-Id strip IDOR | #2074 ✅ merged | 🟡 PARTIAL 75% | State-check win: IDOR fix actually shipped PR #1991 Wave tenant-domain-1 Bucket A 2026-06-01 (RemoveRequestHeader + TenantHeaderGuardFilter + 11 unit tests PASS); 3 AC unchecked → GAP-825 P1 follow-up; live verify gated GAP-612 |

**Wave outcome:** 5/5 buckets shipped; 5/5 PRs merged; 2 gaps DONE (GAP-622/788) + 3 PARTIAL (GAP-823 30%, GAP-867 40%, GAP-814 75%). NEW artifacts: 1 META rule + 1 ADR + 1 consolidation doc + 1 audit batch + 1 observability plan. State-check wins surfaced AWS-unblocked status (correcting Wave 7+8 "AWS-blocked" filter assumption).
