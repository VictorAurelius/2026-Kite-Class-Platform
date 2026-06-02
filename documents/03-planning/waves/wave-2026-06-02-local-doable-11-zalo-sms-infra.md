---
title: Wave local-doable-11 — Zalo/SMS infra (GAP-063 unblock) + META rules consolidation
status: complete
created: 2026-06-02
updated: 2026-06-02
completed_at: 2026-06-02
tag_primary: local-doable
tags_secondary: [phase-1-beta, non-aws, zalo-sms-infra, meta]
counter: 11
waves: [local-doable-11]
gaps: [GAP-063, GAP-868]
---

# Wave local-doable-11 — Zalo/SMS infra unblock + META rules consolidation

**Goal:** File GAP-063 P0 (Zalo OA + SMS notification infra) + ship Zalo OA token scaffold + SMS provider eval research + 2 META rule consolidation tasks. Unblocks Wave 12+ GAP-286 OTP + GAP-297 invoice batch notification.

**Trigger:** Wave 11 outside-in persona simulation audit (PR #2085) surfaced 3 critical cross-cutting findings — top is **GAP-063 Zalo/SMS infra dependency** chặn 3/5 personas trên GAP-286 + GAP-297. User direction (Q2): "Block Wave 11 — file GAP-063 first" + scope alternative locked = Zalo/SMS infra scaffold.

**Estimated wall-clock:** ~3-4h parallel (5 Opus bg-agents, staggered 2+2+1). All buckets non-AWS scope.

---

## 1. Brainstorm

**Q1 (inside-out 3-source pull per `inside-out-completeness-trigger.md`):**
- **gap-status.csv non-AWS filter:** GAP-063 (existing per Wave 93 GAP-185 sister chain), GAP-868 P1 META end-session skill (NOT FILED yet — needs Bucket D)
- **inside-out-queue.md:** Zalo OA + group Zalo cho phụ huynh (active queue item per `thesis-as-future-state-mandate.md` v1.0.0 cited Ch1 §1.1.2 + §1.4 thesis claim "đã kết nối Zalo OA")
- **AskUserQuestion explicit:** User picked Q1 A1+A2 parallel + Q2 BLOCK + Q3 internal receipt only + Q4 expand P3+P5+P6; Q2 alternative = "GAP-063 file + Zalo OA scaffold + SMS provider research (META)"
- **Outside-in audit findings (PR #2085 — Wave 11 pre-lock persona simulation):** 10 critical (3 P0 + 7 P1) + 5 cross-cutting VN cultural blind spots + 7 new gap candidates. **Top finding X1 = Zalo OA dominance** (this wave addresses).

**Q2 (alternatives rejected):**
- Original Wave 11 = GAP-286 + GAP-297 FE features — REJECTED Q2 (GAP-063 dep blocks 3/5 personas)
- Audit suite refresh (Direction 1) — DEFER Wave 12 (compliance deadline 2026-06-05; can ship parallel với GAP-063 follow-up)
- P1 closure batch (Direction 3) — partial overlap: META D bucket includes GAP-868 file
- GAP-063 ship full impl Wave 11 — REJECTED: requires Zalo OA verified business account + carrier SMS contract; impl Wave 12 post-research

**Q3 (risks):**
- **R1 — GAP-063 file scope ambiguity:** scope = Zalo OA + SMS + email parallel notification channel. Risk: scope balloon to notification framework refactor. Mitigation: Bucket A scope = file gap với clear Phase 1 scaffold scope (Zalo OA + SMS); Wave 12 ship full impl
- **R2 — Bucket B Zalo OA scaffold dependency:** Zalo OA verified account required for real token call. Mitigation: scaffold = config skeleton + interface + mock impl; integration test với mock token; live verify defer
- **R3 — Bucket C SMS provider eval scope:** 3+ candidates (Twilio + Stringee + esms.vn + others). Mitigation: agent timeboxed 60min; output = comparison table + recommendation (no commitment)
- **R4 — Bucket D GAP-868 file:** end-session skill META P1 noted Wave 6 follow-up nhưng not filed CSV. Mitigation: bucket = file gap + scope; impl defer
- **R5 — Disjointness:** A docs gap file + B Zalo Java scaffold + C SMS docs research + D META rule file + E rules index sync script — verified disjoint

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-063 file (Zalo OA + SMS infra) | bg-agent Opus | ~60min | ✅ NEW gap file + audits-index row |
| B | Zalo OA token scaffold (Java interface + mock) | bg-agent Opus | ~75min | ✅ kc-core hoặc kitehub-email new module ZaloOAClient |
| C | SMS provider eval research (docs) | bg-agent Opus | ~60min | ✅ NEW `documents/02-architecture/sms-provider-eval.md` |
| D | GAP-868 file (end-session skill META) + skill scaffold | bg-agent Opus | ~60min | ✅ NEW gap file + `.claude/skills/workflow/end-session/` extension |
| E | Rules index sync script (META P1) | bg-agent Opus | ~75min | ✅ NEW `scripts/check-rules-index-sync.sh` + workflow wire |

**Disjoint check:** A docs gap + B Java scaffold + C arch docs + D META skill + E scripts — separate top-level paths.

**Cross-layer check per `contract-first-for-cross-layer.md` §2:** NO bucket touches both FE + BE same scope. Bucket B BE scaffold only; FE wait Wave 12.

---

## 3. Scope

**Stake tier:** MEDIUM-HIGH (infra unblock + META governance) → **Opus 4.7** mandatory per `agent-model-opus-default.md`
**Cross-layer?** NO → skip foundation

**Spawn strategy staggered 2+2+1:**
- Batch 1: A + B (Bucket A file GAP-063 unlocks Bucket B scope reference)
- Batch 2: C + D (post Batch 1 first notification)
- Batch 3: E (post Batch 2 first notification)

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-063 file | 🔴 P0 | `documents/04-quality/gaps/phase-1-beta/GAP-063-zalo-oa-sms-notification-infra.md` (NEW) + CSV row + audits-index reference Wave 11 outside-in audit | Batch 1 parallel |
| 2 | **B** | Zalo OA scaffold | 🟠 P1 | `kitehub/kitehub-email/.../ZaloOAClient.java` (interface + mock impl) OR `kiteclass-core/.../zalo/` package | Batch 1 parallel |
| 3 | **C** | SMS provider eval | 🟠 P1 | `documents/02-architecture/sms-provider-eval.md` (NEW — Twilio + Stringee + esms.vn) | Batch 2 parallel |
| 4 | **D** | GAP-868 file + skill | 🟠 P1 META | `documents/04-quality/gaps/phase-1-beta/GAP-868-end-session-skill-meta.md` (NEW) + `.claude/skills/workflow/end-session/SKILL.md` scaffold | Batch 2 parallel |
| 5 | **E** | Rules index sync | 🟠 P1 META | `scripts/check-rules-index-sync.sh` (NEW) + `.github/workflows/quality-rules-skills.yml` job | Batch 3 |

### Bucket A — GAP-063 file (Zalo OA + SMS notification infra)

- Files: NEW `documents/04-quality/gaps/phase-1-beta/GAP-063-zalo-oa-sms-notification-infra.md` + CSV row
- Scope: P0 — Zalo OA priority + SMS fallback + Email parallel; AC includes Phase 1 scaffold + Phase 2 live integration + Phase 3 monitoring
- Per `thesis-as-future-state-mandate.md` v1.0.0 — reference thesis Ch1 §1.1.2 "đã kết nối Zalo OA" + §1.4 (minimum interpretation = passive CTA shipped GAP-660; full interpretation = active push this gap)
- Reference Wave 11 outside-in audit findings PR #2085 §X1 + audits-index row
- Acceptance: gap file complete + CSV row + audits-index reference; gap → OPEN (file only); impl Wave 12+

### Bucket B — Zalo OA token scaffold

- Files: NEW `kitehub/kitehub-email/.../ZaloOAClient.java` (interface) + `MockZaloOAClient.java` (impl) + `ZaloOAConfig.java` (`@ConfigurationProperties`) + unit test
- Strict scope: interface + mock impl + config skeleton; NO live API calls; per `design-patterns.md` §3.10 domain types neutral
- Acceptance: code compiles + unit test PASS; Wave 12 live Zalo OA Business API integration

### Bucket C — SMS provider eval research

- Files: NEW `documents/02-architecture/sms-provider-eval.md` — comparison table 3+ providers (Twilio Vietnam + Stringee + esms.vn + viettel/vnpt brandname)
- Methodology: WebSearch + docs research; cost/latency/coverage/API/compliance
- Recommendation: pick MVP provider với rationale; defer commitment Wave 12 decision
- Per `dev-readable-doc-language.md` §2 VN narrative

### Bucket D — GAP-868 file + end-session skill scaffold

- Files: NEW `documents/04-quality/gaps/phase-1-beta/GAP-868-end-session-skill-meta.md` (P1 META end-session skill formalization) + extend `.claude/skills/workflow/end-session/SKILL.md` per Wave 6 notes
- Scope: file gap + add specific scaffold items (handoff template improvements, post-merge sync check, context-budget recalibration); impl Wave 12 follow-up
- Per `meta-csv-index-pattern.md` — file rule + skill update + CSV rows

### Bucket E — Rules index sync script

- Files: NEW `scripts/check-rules-index-sync.sh` validate `.claude/rules/rules-index.csv` ↔ filesystem `.claude/rules/*.md` 100% coverage parity per `meta-csv-index-pattern.md`
- Self-test fixtures: synthetic missing-row + missing-file FAIL cases
- CI wire: `.github/workflows/quality-rules-skills.yml` new job `rules-index-sync`
- Acceptance: script + self-test PASS + CI green

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| Wave 11 outside-in audit | docs | `ls documents/04-quality/audits/persona-review/2026-06-02-wave-11-pre-lock-persona-mobile-otp-batch-invoice.md` | merged PR #2085 (pending) | ✅ exists (audit shipped) |
| GAP-063 existing | gap file | `find documents/04-quality/gaps -name "GAP-063*.md"` | bg-agent verifies — may have sister gap | 🆕 to-be-created (Bucket A) OR extend existing |
| GAP-868 NOT FILED | gap CSV | `bash scripts/query-gaps.sh "" GAP-868` returns empty | confirmed Wave 8 brainstorm | 🆕 to-be-created (Bucket D) |
| Zalo OA existing surface | code | `grep -rn "ZaloOA\|zaloOA\|zalo.oa" kitehub/ kiteclass/` | bg-agent verifies — possibly existing config | 🆕 verify pre-existing scope (Bucket B) |
| `.claude/skills/workflow/end-session/SKILL.md` | skill | `ls .claude/skills/workflow/end-session/SKILL.md` | shipped Wave 7 v1.1.0 | ✅ exists (Bucket D extends) |
| `scripts/check-rules-index-sync.sh` | NEW script | `ls scripts/check-rules-index-sync.sh` | not exist | 🆕 to-be-created (Bucket E) |
| `.claude/rules/rules-index.csv` | CSV | `wc -l .claude/rules/rules-index.csv` | bg-agent verifies size + 100% parity expected | ✅ exists |

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `bash scripts/check-gap-status-csv.sh` + `bash scripts/check-gap-folder-location.sh` PASS | quality-docs.yml |
| B | `cd kitehub && ./mvnw -pl kitehub-email test -Dtest='ZaloOA*' -P strict-warnings` PASS | kitehub-email-ci |
| C | docs-only — `bash scripts/check-readme-freshness.sh` | quality-docs.yml |
| D | `bash scripts/check-gap-status-csv.sh` + `bash scripts/check-skill-conventions.sh` PASS | quality-docs.yml + quality-rules-skills.yml |
| E | `bash scripts/check-rules-index-sync.sh` self-test PASS + YAML lint | quality-rules-skills.yml + script-quality |

---

## 6. Agent Spawn Pattern (staggered 2+2+1)

Per `agent-model-opus-default.md` + `agent-background-spawn-default.md` + `feedback_parallel_agent_strategy.md`:
- All buckets `model: "opus"` + `run_in_background: true` + `isolation: "worktree"`
- Batch 1 (A+B): immediate post Wave 11 plan PR merge
- Batch 2 (C+D): post Batch 1 first completion
- Batch 3 (E): post Batch 2 first completion
- RELATIVE paths per `feedback_worktree_absolute_path_contamination.md`

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `wave-closure-scope-completeness.md` + `post-merge-sync-completeness.md` + `post-wave-cleanup.md`:
- Each bucket PR updates affected gap file Log + status + CSV row
- Wave plan frontmatter `status: complete` flip in closure PR
- `wave-history.jsonl` append (Rule 15)
- **Scope-Completeness Reconciliation table** per `wave-closure-scope-completeness.md` §3
- Bucket A: file gap + Wave 12 candidate for impl
- Bucket B: scaffold complete; Wave 12 live integration follow-up
- Bucket C: docs-only research; Wave 12 decision lock provider
- Run `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md`

---

## 8. Log

- **2026-06-02** (draft): Plan created per user Q2 alternative pick "GAP-063 file + Zalo OA scaffold + SMS provider research (META)". Wave 11 outside-in persona simulation findings (PR #2085) integrated §1 Brainstorm Q1. 5 non-AWS buckets: 1 P0 gap file + 2 P1 scaffold/research + 2 P1 META. Staggered 2+2+1 spawn. Outside-in audit DONE pre-lock per `outside-in-coverage-trigger.md` §1 (NOT skip — explicit run + findings drive scope pivot from features → infra).
- **2026-06-02** (complete): All 5 buckets shipped 5 PRs. Wall-clock ~4h actual (vs ~3-4h estimate). Staggered 2+2+1 spawn pattern executed: Batch 1 (A+B initial spawn killed mid-flight due to coordinator miss reading locked plan — see §9 Misses; salvaged Bucket B from worktree, re-spawn Bucket A) → Batch 2 (C+D parallel) → Batch 3 (E solo). State-check wins: Bucket A (GAP-063 already existed P1 PARTIAL Wave 18a EMAIL shipped — rename + re-scope in place P0 + 3-phase); Bucket D (end-session skill v1.1.0 already shipped — filed GAP-868 PARTIAL 20% + TODO markers for Wave 12+ extensions). Closure PR (this commit) flips status + appends wave-history per `wave-tag-numbering-convention.md` v1.0.0 + reconciliation table per `wave-closure-scope-completeness.md` §3.

---

## 9. Closure Reconciliation (per `wave-closure-scope-completeness.md` §3)

### 9.1 Scope-Completeness Reconciliation Table

| # | Plan §3 Scope item | PR | Verdict | Follow-up |
|---|---|---|---|---|
| 1 | Bucket A — GAP-063 file (Zalo OA + SMS notification infra) | #2093 | ✅ DONE (rescope + 3-phase) | GAP-063 Phase 2 live integration Wave 12+ |
| 2 | Bucket B — Zalo OA token scaffold (Java mock + IT + config) | #2094 | ✅ DONE | Live Zalo OA verification requires verified business account — Wave 12+ |
| 3 | Bucket C — SMS provider eval research | #2095 | ✅ DONE (Stringee primary recommendation) | Wave 12+ provider lock decision + PoC |
| 4 | Bucket D — GAP-868 file + end-session skill scaffold | #2096 | 🟡 PARTIAL (gap filed + TODO markers; full extensions defer) | GAP-868 Phase 2/3 Wave 12+ |
| 5 | Bucket E — Rules index sync script + CI wire | #2097 | ✅ DONE (WARN mode) | HARD STOP flip Wave 12+ per `incident-to-rule-pipeline.md` §3.1 |

**Aggregate:** 4 ✅ DONE + 1 🟡 PARTIAL (GAP-868 tracked) + 0 ❌ NOT-IMPLEMENTED.

### 9.2 Gaps shipped/filed this wave

- **DONE flips:** none Phase 1 only scaffold + research
- **PARTIAL filed:** GAP-868 (20% — Phase 1 scaffold; full impl Wave 12+)
- **PARTIAL extended:** GAP-063 (rescope P1→P0 + 3-phase; Phase 1 EMAIL preserved historical baseline + Zalo OA Phase 1 scaffold shipped this wave)
- **NEW gaps filed:** 0 (GAP-063 + GAP-868 both pre-existing or scoped from plan)

### 9.3 Cross-cutting closure misses (transparent log)

1. **Coordinator wave-name collision (Batch 1 initial spawn):** Tôi spawn 3 agents `wave-local-doable-11-bucket-{d2,e2,f2}` cho GAP-867 Phase 2 — chưa đọc kỹ handoff doc nên miss locked plan `wave-2026-06-02-local-doable-11-zalo-sms-infra.md` đã ship qua PR #2087. Killed 3 agents mid-flight, re-spawn theo locked scope. Per `outside-in-coverage-trigger.md` §1 + `pre-mutation-state-check.md` §3 — coordinator should have run state-check on locked Wave 11 plan FIRST.
2. **Bucket B agent killed prematurely:** Agent was self-correcting worktree contamination (writes leaked to main worktree during transition; agent moved files to worktree before kill). I observed stale `git status` snapshot during transition, killed prematurely. Manual salvage from worktree succeeded — mvn verify PASS 100 tests.
3. **Hook false-positive PR #2092:** post-merge `audit-gate.py` flagged "missing ai-branding-quality-gate audit" because diff touched Gemini/OpenAI client files (filename keyword match). Actual change was getter additions + IT cleanup, zero AI behavior change. Override comment posted on PR.

### 9.4 Cleanup per `post-wave-cleanup.md`

Run `bash scripts/prune-merged-worktrees.sh --yes` post-closure — prune worktree husks + merged branches from this wave's 7 PRs + Wave 7-10 residual (~22+ husks expected).
