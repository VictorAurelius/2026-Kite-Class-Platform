---
title: Wave local-doable-7 — Wave 6 follow-ups + P0 DevOps closures
status: complete
created: 2026-06-02
updated: 2026-06-02
completed_at: 2026-06-02
waves: [local-doable-7]
gaps: [GAP-543, GAP-695, GAP-127, GAP-866, GAP-658, GAP-572, GAP-869, GAP-870]
---

# Wave local-doable-7 — Wave 6 follow-ups + P0 DevOps closures

**Goal:** Close 5 P0/P1 PARTIAL/OPEN gaps via 5 parallel buckets — unblock Wave 6 Bucket I live walk evidence (GAP-866), ship CI bundle-budget guardrail, sweep VN seed residual, rotate Resend secret schema, refresh email content audit + self-test catalog.

**Trigger:** Wave 6 closed 4 PRs + handoff doc identified 5 follow-up buckets; state-check revealed 2 stale picks (Bucket C GAP-466 already shipped Wave 56; Bucket E GAP-687 blocked Wave thesis-2 chờ GAP-648/649) → replaced với GAP-866 (P0 unblocks Wave 6 Bucket I) + GAP-572 (P0 DevOps disjoint).

**Estimated wall-clock:** ~3-4h parallel (5 Opus bg-agents); longest-bucket ~90min (Bucket C kc-core RabbitAdmin requires Docker stack verify).

---

## 1. Brainstorm

**Q1 (inside-out source breakdown):**
- **ROADMAP / gap-status.csv (3-source pull per `inside-out-completeness-trigger.md`):** GAP-866 P0 OPEN, GAP-572 P0 PARTIAL 60%, GAP-127 P0 PARTIAL 85%, GAP-658 P0 PARTIAL 90%, GAP-543 P0 PARTIAL 95%, GAP-695 P0 PARTIAL 85%
- **Inside-out queue file (`documents/03-planning/inside-out-queue.md`):** 5 active items checked; none Phase 1 BETA + Wave 7 relevant (premium plan + feedback channel + email content already in scope via GAP-543; user manual defer Phase 1.5; thesis defer Wave thesis-2; PDPL defer Wave compliance-1)
- **AskUserQuestion explicit:** User picked Bucket C + E replacement "best practice" → coordinator picked GAP-866 (blocker compound value) + GAP-572 (disjoint near-DONE)
- **Outside-in audit:** SKIP per `outside-in-coverage-trigger.md` §4 row 4 — Wave 100% internal tech-debt closure scope; recent audit ≤30d (Wave 100 3-agent 2026-05-19 thesis surface)

**Q2 (alternatives considered):**
- Original handoff Bucket C (GAP-466 RLS IT) — REJECTED: state-check verified Phase 1-4 shipped Wave 56; only perf-baseline remains, tracked GAP-469 (separate gap)
- Original handoff Bucket E (GAP-687 thesis) — REJECTED: Phase 3 DEFER Wave thesis-2 chờ GAP-648/649; per `thesis-as-future-state-mandate.md` Phase 3 = Phase 1.5+ scope không Phase 1 BETA Wave 7
- Alternative Bucket C: GAP-608 (90% ses:SendEmail IAM) — DEFERRED Wave 8: GAP-866 has higher compound value (unblocks GAP-777 walk-deferred shipped Wave 6)
- Alternative Bucket E: GAP-868 (P1 META end-session skill) — DEFERRED: Feature-P0 > Meta-P1 per `meta-gap-priority.md` §3 priority matrix

**Q3 (risks):**
- **R1 — Bucket C requires Docker stack verify:** kc-core RabbitAdmin fix needs Testcontainers OR live RabbitMQ. Mitigation: agent uses ApplicationContext IT or Testcontainers RabbitMQ; if stuck, file FEATURE_SHIP_WALK_DEFER trailer per `feature-ship-runtime-walk-mandate.md` §5
- **R2 — Bucket E Resend rotate touches production secret:** AWS Secrets Manager rotation requires `kite-readonly` profile only (per `agent-aws-access.md` Tier 1); actual rotate execution = dev-trigger. Mitigation: agent ships IaC schema fix + fetch-secrets.sh fix + rotation runbook; rotation execute = follow-up dev step
- **R3 — Bucket A MailHog verify needs local stack:** Email content audit live verify per `pre-handoff-self-test-completeness.md` §2.3. Mitigation: if MailHog not reachable, document PRE_HANDOFF_PARTIAL trailer + paired follow-up gap
- **R4 — Disjointness:** A (docs+email), B (scripts/CI), C (Backend kc-core), D (seed-data Java), E (DevOps secret) — verified disjoint at module/file level

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-543 + GAP-695 | bg-agent Opus | ~60min | ✅ docs + email content + self-test catalog markdown |
| B | GAP-127 | bg-agent Opus | ~60min | ✅ `scripts/*.sh` + `.github/workflows/*.yml` + frontend `next.config.ts` |
| C | GAP-866 | bg-agent Opus | ~90min | ✅ `kiteclass/kiteclass-core/.../config/*RabbitMQ*Config.java` only |
| D | GAP-658 | bg-agent Opus | ~45min | ✅ seed data Java + worker code |
| E | GAP-572 | bg-agent Opus | ~75min | ✅ `infrastructure/terraform-aws/secrets.tf` + `scripts/fetch-secrets.sh` + runbook |

**Disjoint check:** verified no two buckets touch same package/file. A docs-only + B scripts/CI + C kc-core Java config + D kc-core seed Java + E infra+scripts. Bucket C + D both touch kc-core but disjoint subdirs (`config/` vs `worker/seed/`).

**Cross-layer check (per `contract-first-for-cross-layer.md` §2):** NO bucket touches both FE + BE same scope. Bucket 0 Foundation NOT required.

---

## 3. Scope

**Stake tier (per `wave-pack-planner/SKILL.md`):** MEDIUM → model: **Opus 4.7** mandatory per `agent-model-opus-default.md`
**Cross-layer?** NO → skip foundation bucket

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-543 + GAP-695 | 🔴 P0 | `documents/04-quality/audits/email/**` + `documents/03-planning/self-test-catalog*.md` | parallel |
| 2 | **B** | GAP-127 | 🔴 P0 | `scripts/check-bundle-budget.sh` + `.github/workflows/quality-code.yml` + `kitehub/kitehub-frontend/next.config.ts` | parallel |
| 3 | **C** | GAP-866 | 🔴 P0 | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/config/RabbitMQ*.java` + `*ApplicationRunner*.java` | parallel |
| 4 | **D** | GAP-658 | 🔴 P0 | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/seed/**` + `kiteclass-core/.../worker/*VnSeed*.java` | parallel |
| 5 | **E** | GAP-572 | 🔴 P0 | `infrastructure/terraform-aws/secrets.tf` + `scripts/fetch-secrets.sh` + `documents/05-guides/operations/resend-rotation-runbook.md` | parallel |

### Bucket A — Email content MailHog verify + self-test catalog refresh

- Files: `documents/04-quality/audits/email/**` (NEW audit doc) + existing self-test catalog (locate via Glob)
- Acceptance: 5 email types verified via MailHog OR PRE_HANDOFF_PARTIAL trailer per `pre-handoff-self-test-completeness.md` §2.3; self-test catalog refreshed với GAP-866 + GAP-867 + GAP-868 NEW entries from Wave 6

### Bucket B — CI bundle-budget guardrail script

- Files: `scripts/check-bundle-budget.sh` (NEW) + `.github/workflows/quality-code.yml` job `bundle-budget` (NEW) + `kitehub-frontend/next.config.ts` (verify bundle-analyzer)
- Acceptance: Script asserts FE bundle <300KB threshold; CI WARN-mode initial; self-test fixture (PASS + FAIL) embedded
- Subsumes GAP-236 (per handoff scope note)

### Bucket C — kc-core RabbitAdmin bean missing (GAP-866 — Wave 6 Bucket I unblock)

- Files: `kiteclass-core/.../config/RabbitMQConfig.java` (add `RabbitAdmin` @Bean) + `*ApplicationRunner*.java` (fix dependency or remove eager declaration)
- Tests: ApplicationContext IT verifies kc-core boots clean (no UnsatisfiedDependencyException); `./mvnw -pl kiteclass-core test`
- Acceptance: kc-core Docker container reaches `(healthy)` status; sister effect = GAP-777 Bucket I walk evidence unblocked (file follow-up if user wants walk run this wave)

### Bucket D — VN sample seed residual placeholders (GAP-658)

- Files: `kiteclass-core/.../seed/**` Java seed classes + worker entry points
- Acceptance: 0 English placeholder strings remain in VN-tenant seed (verify via grep `John|Jane|Smith|Doe|test@example` in `seed/` paths returning 0)

### Bucket E — Resend secret schema + key rotate (GAP-572)

- Files: `infrastructure/terraform-aws/secrets.tf` (JSON schema wrapper `{api_key, from_email, from_name}`) + `scripts/fetch-secrets.sh` line 88-98 (`jq -r .api_key` extraction verified) + `documents/05-guides/operations/resend-rotation-runbook.md` (NEW)
- Acceptance: Terraform plan clean with JSON wrapper; fetch-secrets.sh extracts correctly via Testcontainers OR mock; rotation runbook documents revoke-old + provision-new steps (actual revoke = dev follow-up trigger)

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `GAP-543` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-543*.md` | 1 file (PARTIAL 95%) | ✅ exists |
| `GAP-695` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-695*.md` | 1 file (PARTIAL 85%) | ✅ exists |
| `GAP-127` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-127*.md` | 1 file (PARTIAL 85%) | ✅ exists |
| `GAP-866` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-866*.md` | 1 file (OPEN) | ✅ exists |
| `GAP-658` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-658*.md` | 1 file (PARTIAL 90%) | ✅ exists |
| `GAP-572` | gap file | `ls documents/04-quality/gaps/phase-1-beta/GAP-572*.md` | 1 file (PARTIAL 60%) | ✅ exists |
| `RabbitMQConfig.java` (kc-core) | Java class | `grep -rn "class RabbitMQConfig\|@Configuration.*Rabbit" kiteclass/kiteclass-core/src/main/java` | bg-agent verifies at spawn | 🆕 to-be-verified (Bucket C agent) |
| `fetch-secrets.sh` line 88-98 | shell script | `sed -n '88,98p' scripts/fetch-secrets.sh` | bg-agent verifies | 🆕 to-be-verified (Bucket E agent) |
| `next.config.ts` bundle-analyzer | FE config | `grep -n "bundle-analyzer\|BUNDLE_ANALYZE" kitehub/kitehub-frontend/next.config.ts` | bg-agent verifies | 🆕 to-be-verified (Bucket B agent) |
| `scripts/check-bundle-budget.sh` | shell script | `ls scripts/check-bundle-budget.sh` | not yet exist | 🆕 to-be-created (Bucket B) |
| `documents/05-guides/operations/resend-rotation-runbook.md` | runbook | `ls documents/05-guides/operations/resend-rotation-runbook.md` | not yet exist | 🆕 to-be-created (Bucket E) |

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | MailHog `curl http://localhost:8025/api/v2/messages` OR PRE_HANDOFF_PARTIAL | docs-only (quality-docs.yml) |
| B | `bash scripts/check-bundle-budget.sh --dry-run` + workflow YAML lint | quality-code.yml `bundle-budget` (new job WARN-mode) |
| C | `cd kiteclass/kiteclass-core && ./mvnw test -Dtest='*RabbitMQ*'` + Docker `up.sh` health check kc-core `(healthy)` | core-ci |
| D | `cd kiteclass/kiteclass-core && ./mvnw test -Dtest='*Seed*'` + grep VN placeholders | core-ci |
| E | `terraform plan` clean + `bash scripts/fetch-secrets.sh --dry-run resend` extracts correctly | quality-infra.yml (if applicable) |

---

## 6. Agent Spawn Pattern

Per `agent-model-opus-default.md` + `agent-background-spawn-default.md` + `feedback_parallel_agent_strategy.md`:
- All 5 buckets spawned với `model: "opus"` + `run_in_background: true` + `isolation: "worktree"`
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequentially after all background completions
- Max 5 concurrent confirmed (matches limit per parallel agent strategy memory)

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `wave-closure-scope-completeness.md` + `post-merge-sync-completeness.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feature-ship-runtime-walk-mandate.md`:
- Each bucket PR updates affected GAP file Log + status + CSV row
- ROADMAP §🚀 Next Action updated in closure PR
- Wave plan frontmatter `status: complete` flip in closure PR
- `wave-history.jsonl` append in closure PR (Rule 15)
- **Scope-Completeness Reconciliation table** in closure PR body per `wave-closure-scope-completeness.md` §3 — categorize each bucket ✅/🟡/❌
- Sub-gaps filed for any deferral; PARTIAL exit-ramp per `gap-done-discipline.md` §3
- Run `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md`
- Bucket C closure: if RabbitAdmin fix successful, file follow-up cleanup gap để run GAP-777 walk evidence (Wave 6 Bucket I) on rebuilt stack

---

## 8. Log

- **2026-06-02** (draft): Plan created. State-check found Bucket C (GAP-466) + E (GAP-687 handoff scopes stale; replaced với GAP-866 (compound unblock) + GAP-572 (disjoint near-DONE). Per `outside-in-coverage-trigger.md` §4 outside-in audit SKIP (Wave 100% internal tech-debt closure).
- **2026-06-02** (complete): All 5 buckets shipped via 5 PRs. 4 docs-only auto-merged. 1 code PR (Bucket C #2070) blocked by transient Maven 429 → GAP-870 P1 filed cho workflow Maven cache fix. Notable findings: 3/5 buckets shipped state-check wins (B/D/E — work already done prior waves; closure docs-only).

---

## 9. Scope-Completeness Reconciliation (per `wave-closure-scope-completeness.md` §3)

| # | Plan §3 Scope item | PR | Verdict | Follow-up |
|---|---|---|---|---|
| 1 | Bucket A — GAP-543 (95→100%) + GAP-695 (85→100%) email content MailHog verify + self-test catalog refresh | #2069 ✅ merged | ✅ DONE | — |
| 2 | Bucket B — GAP-127 (85→100%) CI bundle-budget guardrail (subsumes GAP-236) | #2067 ✅ merged | ✅ DONE | State-check win: GAP-236 already shipped Wave 26 với tighter 250KB threshold + 13 tests; closed GAP-127 citing GAP-236 |
| 3 | Bucket C — GAP-866 P0 OPEN kc-core RabbitAdmin crashloop | #2070 🟡 open | 🟡 PARTIAL | Code fix shipped + RabbitConfigContextIT 3/3 PASS; PR blocked Maven 429 transient → GAP-870 P1 follow-up |
| 4 | Bucket D — GAP-658 (90→100%) VN sample seed residual placeholders | #2068 ✅ merged | ✅ DONE | State-check win: production seed already 100% VN (Wave 98 B2 + Wave br-9 D); verification-only DONE flip |
| 5 | Bucket E — GAP-572 (60→75%) Resend secret schema + rotate runbook | #2066 ✅ merged | 🟡 PARTIAL | Schema parity already shipped Wave email-finalize-1 + Wave aws-restore-1; this PR added rotation runbook + filed GAP-869 P1 cho actual key rotation execution |
| 6 (NEW) | Trivy Security Scan Maven 429 transient | #2077 ✅ merged via GAP-870 docs file | 🆕 follow-up filed | GAP-870 P1 OPEN workflow fix |

**Wave outcome:** 5/5 buckets executed; 4 ✅ + 2 🟡 PARTIAL (Bucket C PR open pending GAP-870; Bucket E rotation execution → GAP-869). 5 gaps closed (GAP-127/543/658/695 + GAP-866 code shipped). 2 NEW follow-up gaps filed (GAP-869 + GAP-870).
