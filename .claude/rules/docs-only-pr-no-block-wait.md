---
paths:
  - ".github/workflows/**"
  - "documents/**"
  - ".claude/rules/**"
  - ".claude/skills/**"
---

# Docs-Only PR No-Block-Wait — continue work, re-check CI later

**Priority:** 🟠 MANDATORY — workflow efficiency governance
**Version:** 1.1.0
**Created:** 2026-05-28
**Last-Reviewed:** 2026-06-03
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.1.0 MINOR self-approve per `rule-change-process.md` §5; generalizes scope from "docs-only PR CI watch only" to BOTH (a) CI watch on any PR + (b) local heavy verification script (Testcontainer / Maven build / docker build, est. >60s) — both MUST `run_in_background` thay vì block-wait foreground. Triggered by 2026-06-03 Wave 14 B+C+E verify session where 2 local DB scripts (`check-schema-drift.sh` + `check-audit-col-uniformity.sh`) run foreground → user manually backgrounded both. New §5.5 "Local heavy verification script" + worked self-test §7.5; no constraint loosening — adds previously-uncovered local-script dimension. Filename kept for continuity (rename = cross-ref churn deferred). v1.0.1 (kept): PATCH `paths:` frontmatter per `context-budget-mandate.md` §3.2. v1.0.0 (kept): new rule sharpens `docs-only-pr-auto-merge.md` v1.0.2 — agent should NOT block-wait on CI watch foreground; per §6.5 Enforcement Parity Mandate paired same-PR with worked self-test on 2026-05-28 session)
**Applies to:** (a) Mọi docs-only PR per `docs-only-pr-auto-merge.md` §2 scope khi push + CI starting; (b) Mọi local verification script likely chạy >60s (Testcontainer DB gate, `mvnw verify`, docker build, full test suite) — agent MUST `run_in_background` thay vì block foreground.

---

## 1. The Rule

> **Sau khi push docs-only PR + CI starts, Claude PHẢI continue qua work tiếp theo (không block-wait foreground trên `gh pr checks --watch`).** CI watch chạy background nếu muốn notification, NHƯNG agent KHÔNG được idle wait. Re-check CI status khi reach next natural checkpoint OR khi background notification arrives.

Sister mandate to `docs-only-pr-auto-merge.md` v1.0.2:
- That rule: WHEN CI green → auto-merge (no asking "merge?")
- This rule: WHILE CI runs → don't block-wait; do other work

Both rules eliminate user round-trip friction. Combined effect: docs-only PR ship-to-merge cycle = 1 push + 0 user intervention (CI green → auto-merge happens whenever agent re-checks).

---

## 2. Workflow comparison

### ❌ BANNED pattern (block-wait foreground)

```
1. Push docs-only PR
2. `gh pr checks --watch` (BLOCKS foreground 3-5 min)
3. CI terminal → auto-merge
4. Continue next work
```

Cost: ~3-5 min agent idle per PR × N PRs in session = wall-clock waste.

### ✅ REQUIRED pattern (background + continue)

```
1. Push docs-only PR
2. Optionally launch `gh pr checks --watch` via Bash run_in_background=true (notification only)
3. CONTINUE next work immediately (file next gap, edit next doc, etc.)
4. Auto-merge khi reach natural checkpoint:
   - Background watch notification arrives (system reminder injects task-notification)
   - Agent invokes `gh pr view <N>` for unrelated purpose
   - Session-end / handoff time
   - User explicit ask for status
```

Result: agent throughput maximized; CI runs parallel to next work.

---

## 3. Re-check CI checkpoints

When background notification arrives OR natural checkpoint reached:

```bash
gh pr view <N> --json statusCheckRollup --jq '.statusCheckRollup[] | select((.state // .conclusion) != "SUCCESS" and (.state // .conclusion) != "SKIPPED")' | head -5
```

Decision matrix per `docs-only-pr-auto-merge.md` §4:

| State | Action |
|---|---|
| All SUCCESS / SKIPPED (no failures/pending) | Auto-merge per `docs-only-pr-auto-merge.md` §5 (1 MCP call OR `gh pr merge --squash`) |
| FAILURE in real check | Investigate fail (likely flake / dep issue) — fix + push OR override per §6 |
| Still PENDING > 15 min | Lightweight re-check at next checkpoint (don't block) |
| CANCELLED (queue contention) | Re-trigger via `gh run rerun <id>` OR wait next natural checkpoint |

---

## 4. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| `gh pr checks 1921 --watch` foreground (blocks ~5 min) | Background `Bash run_in_background=true` OR skip watch + re-check at checkpoint |
| Tell user "đang chờ CI" + idle | Tell user "CI running background — sẽ notify when done; tiếp tục với <next-task>" |
| Sequential PR ship: push #1 → wait CI → merge → push #2 → wait CI → merge | Parallel: push #1 + push #2 + push #3 → background watches → auto-merge as each completes |
| Re-check CI status every 30s polling | Trust background notifications; check on natural checkpoint only |
| Block on docs-only PR but proceed on code PR | Inverse logic correct: code PR has higher merge stakes, blocking wait OK; docs-only = lowest stakes, don't block |
| Use AskUserQuestion "wait CI hay tiếp tục?" | Default = continue per this rule; no asking needed |

---

## 5. Code PR caveat

This rule applies ONLY to docs-only PRs per `docs-only-pr-auto-merge.md` §2 scope. Code PR (with `*.java` / `*.ts` / workflow / pom.xml / etc.) — block-wait foreground may be justified because:
- Higher merge stakes (real test failures, real audit checks)
- Admin-merge OVERRIDE decision needs immediate context
- Sequential PR dependency chain

Code PR → use foreground `gh pr checks --watch` OR background + careful re-check.

---

## 5.5 Local heavy verification script no-block-wait (added v1.1.0)

> **Khi chạy local verification script likely >60s (Testcontainer DB gate, `mvnw verify`, docker build, full test suite), agent PHẢI dùng Bash `run_in_background=true` thay vì foreground.** Tiếp tục work tiếp theo (stage files / draft PR body / wire CI); re-check script result khi background notification arrives.

Same principle as §1 (CI watch) extended to local script execution: foreground block-wait trên long script = agent idle + user phải manual background.

### Heavy-script heuristic (run_in_background mandatory)

| Script class | Est. duration | Background? |
|---|---|---|
| Testcontainer DB gate (`check-rls-coverage.sh` / `check-migration-replay.sh` / `check-schema-drift.sh` / `check-audit-col-uniformity.sh` / `check-type-consistency.sh`) | 60-300s (docker pull + Flyway + maybe Spring boot) | ✅ YES |
| `./mvnw verify` / `./mvnw test` (any module) | 120-400s | ✅ YES |
| `pnpm build` / `pnpm test --run` (frontend) | 60-180s | ✅ YES |
| `docker build` / `docker-compose build` | 120-600s | ✅ YES |
| Quick docs CI scripts (`check-gap-status-csv.sh` / `check-audits-index-csv.sh` / grep-based) | <10s | ❌ NO — foreground fine |
| `git` / `gh` metadata ops / `ls` / `cat` | <5s | ❌ NO |

### Required pattern

```
1. Bash run_in_background=true: bash scripts/check-<heavy>.sh
2. Continue: stage files / wire CI workflow / draft PR body / read next file
3. Background notification arrives → Read output tail → PASS/FAIL decision
4. If FAIL → investigate; if PASS → proceed to push
```

### Exception

- Script is the FINAL gate before an action that cannot proceed without its result AND no other work exists to parallelize → foreground acceptable (rare; document inline "no parallelizable work, foreground OK").
- Script est. <60s → foreground fine (overhead of background notification not worth it).

---

## 6. Override mechanism

Genuine exception (need immediate merge gate verification before continue):

```
# Inline note in agent text — no commit trailer needed since this is workflow rule
"Block-waiting on PR #N CI per <reason — e.g., dependency chain Wave A→B sequencing>"
```

Pattern frequency >20% per session triggers meta-review (likely scope mis-defined).

---

## 7. Worked self-test — 2026-05-28 session (this rule's originating incident)

### 7.1 Session PR sequence

This session shipped 5+ PRs in sequence:
1. PR #1920 (Mermaid fix) — code-script PR
2. PR #1916 (cumulative walk-fixes + META rule + findings) — code+docs PR
3. PR #1921 (audit retro doc) — docs-only PR
4. PR (this) — docs-only with gaps + comment + this rule

### 7.2 Original behavior (suboptimal)

For PR #1916 + #1921, agent invoked `gh pr checks --watch` foreground 2-3 separate times:
- Wait PR #1920 CI → ~5 min block
- Wait PR #1916 CI → ~7 min block
- Wait PR #1921 CI → ~5 min block

Total: ~15-17 min agent idle wait. User-perceived friction.

### 7.3 Counterfactual với rule applied

For docs-only PR #1921 (matches scope):
1. Push #1921
2. `Bash gh pr checks 1921 --watch run_in_background=true` (notification only)
3. Continue: file 3 gaps + edit code comment + sync findings doc + draft this rule
4. Background notification arrives → re-check → auto-merge

Total: ~0 min block; ~15 min savings on docs-only PRs alone.

For code PR #1916 (out of scope of this rule):
- Block-wait justified (real test failure needs decision)
- Foreground watch acceptable

### 7.4 Aggregate self-test

| PR | Type | This rule applies? | Original wait | With rule |
|---|---|---|---|---|
| #1920 | code-script | NO | ~5 min OK | ~5 min OK |
| #1916 | code+docs (mixed) | NO (mixed → code-rule applies) | ~7 min OK | ~7 min OK |
| #1921 | docs-only | YES | ~5 min wasted | 0 min |

**Save**: ~5 min wall-clock per docs-only PR + better agent throughput.

**Verdict**: Rule fires correctly trên PR #1921 origin incident. Code PRs still need careful merge gate per `admin-merge-discipline.md`. Self-test PASS ✅.

### 7.5 Worked self-test — 2026-06-03 Wave 14 B+C+E verify session (v1.1.0 origin)

**Scenario:** Reviewing Codex WIP, tôi chạy 3 local DB verify scripts để confirm trước push:
1. `check-migration-replay.sh` — foreground, ~150s, PASS (OK nhưng blocked ~150s)
2. `check-schema-drift.sh` — foreground → ~420s timeout kill (Maven build heavy) → **user manually backgrounded**
3. `check-audit-col-uniformity.sh` — foreground → **user manually backgrounded** → PASS

**Apply §5.5 retroactively:**

| Script | Est duration | §5.5 verdict | Original behavior | With rule |
|---|---|---|---|---|
| check-migration-replay.sh | ~150s | ✅ background mandatory | foreground (~150s idle) | run_in_background + continue staging files |
| check-schema-drift.sh | >420s (Maven) | ✅ background mandatory | foreground → timeout kill + user manual bg | run_in_background from start |
| check-audit-col-uniformity.sh | ~120s | ✅ background mandatory | foreground → user manual bg | run_in_background + continue wire quality-db.yml |

**Counterfactual:** ~3 scripts × foreground block = ~690s agent idle + 2 user manual-background interventions. With rule: 0 idle (parallelize với stage files + wire CI + draft PR body), 0 user intervention.

**Verdict:** Rule v1.1.0 fires correctly trên originating session — 3/3 heavy DB scripts match §5.5 heuristic. Self-test PASS ✅.

---

## 8. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 8.1 Self-detection (in-turn)

**CI watch:** Before invoking `gh pr checks --watch` foreground (without `run_in_background=true`):
- Check PR scope: docs-only per `docs-only-pr-auto-merge.md` §2?
- If YES → switch to `run_in_background=true` + continue next work
- If NO → foreground watch may be justified

**Local heavy script (v1.1.0):** Before invoking `bash scripts/<x>.sh` foreground:
- Est. duration >60s per §5.5 heuristic (Testcontainer / mvnw / docker / full suite)?
- If YES → `run_in_background=true` + continue parallelizable work (stage / wire CI / draft PR body) → re-check on notification
- If <60s (grep-based docs script / metadata op) → foreground fine

### 8.2 Reviewer manual

User can flag if agent block-waits on docs-only PR: "không block-wait trên docs-only nữa, có rule mới rồi". Pattern repeated → file follow-up gap referencing this rule.

### 8.3 PR template extension (deferred)

Future: add row `.github/PULL_REQUEST_TEMPLATE.md` cho compliance tracking. Defer per `incident-to-rule-pipeline.md` §3.1 — reviewer-checklist + worked self-test §7 sufficient cho v1.0.0.

### 8.4 Memory auto-load (optional, deferred)

Memory entry `feedback_docs_only_pr_no_block_wait.md` could remind tại session start. Defer per premature-rule guard ≥7 ngày.

### 8.5 Override mechanism

Per §6 inline note. Quarterly retro reviews >20% override frequency.

---

## 9. Relationship to other rules

- **`docs-only-pr-auto-merge.md`** v1.0.2 §5 — sister rule covers WHEN to merge (CI green). This rule covers WHILE CI runs (don't block-wait). Both compose: push → background watch → continue work → auto-merge on notification.
- **`admin-merge-discipline.md`** v1.0.3 — code PR may need admin-merge with OVERRIDE; foreground watch justified for code PR pre-merge review.
- **`agent-action-bias.md`** §1 Part A — "do it yourself". This rule extends: don't waste self-time idle on CI watch.
- **`ci-queue-local-runner-threshold.md`** v1.0.0 §2 — local-CI parity for docs-only PRs reduces CI canonical wait time. Combined: local-CI pre-flight + background CI canonical + no-block-wait = optimal docs-only PR cycle.
- **`mcp-first-with-fallback.md`** §2 — MCP tools when available. `mcp__github__pull_request_read get_status` faster than gh CLI poll for re-check checkpoints.
- **`rule-change-process.md`** §6.5 Enforcement Parity — rule + reviewer-checklist + worked self-test §7 same PR.
- **`output-review-mandate.md`** §3 — paired same-PR new matrix row "Docs-only PR no-block-wait" tracking standard.
- **`meta-gap-priority.md`** §3 — META P2 force-multiplier (workflow efficiency, not P0 like trust-pass eliminate).
- **`incident-to-rule-pipeline.md`** — this rule = direct output of 2026-05-28 user-flagged "thêm rules không đợi CI docs only nữa, tiếp tục làm việc và re-check sau khi CI done" applied through 5-stage pipeline.
- **`context-budget-mandate.md`** §3.2 — path-scoped `paths: [".github/workflows/**", "documents/**", ".claude/rules/**", ".claude/skills/**"]` — rule loads only khi PR workflow context touches these.

---

## 10. Log

- **2026-06-03** (v1.1.0): MINOR — generalized scope từ "docs-only PR CI watch only" sang BOTH (a) CI watch any PR + (b) local heavy verification script (Testcontainer / `mvnw verify` / docker build / full test suite, est. >60s) → cả 2 PHẢI `run_in_background` thay vì block foreground. Added §5.5 "Local heavy verification script no-block-wait" (heavy-script heuristic table + required pattern + exception) + §7.5 worked self-test + Applies-to extended. Triggered by user-flagged 2026-06-03 mid-Wave-14 B+C+E verify session: "rule run background bao phủ cả các scripts chưa?" — sau khi 2 local DB scripts (`check-schema-drift.sh` + `check-audit-col-uniformity.sh`) chạy foreground → user manually backgrounded both (friction signal). Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged + session evidence 2 manual-bg) → Classify ✓ (v1.0.0 §1 covers CI watch only; `ci-queue-local-runner-threshold.md` covers WHEN-run-local not foreground-vs-background; harness `run_in_background` guidance exists but no project rule mandate) → Rule+Enforce ✓ (this §5.5 + §7.5 worked self-test + §8.1 self-detection extension paired same-PR per `rule-change-process.md` §6.5) → Self-Test ✓ (§7.5 — 3/3 heavy DB scripts match heuristic, ~690s idle + 2 manual interventions eliminated) → Retro Log ✓ (this entry). MINOR per §4 (widens scope + new section — could change agent behavior, no constraint loosening; existing foreground-on-quick-script grandfathered <60s). Filename kept `docs-only-pr-no-block-wait.md` for cross-ref continuity (rename deferred — ~5 cross-refs). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5). META P2 force-multiplier per `meta-gap-priority.md` §3 — mọi heavy local script subsequent auto-comply prospectively.
- **2026-05-31** (v1.0.1): PATCH — added `paths:` frontmatter per `context-budget-mandate.md` §3.2 (rule was always-load, violating §3.2 size-gate ≥1k tokens requires path-scope/justification/hook). Scope matches rule's own **Applies to** — no behavior change (rule still fires when relevant files touched); removes ~11k chars from base session context. Part of Wave meta context-budget rule-scoping batch 2026-05-31. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — path-scope correction, no constraint loosening).

- **2026-05-28 (v1.0.0):** Rule created in response to user direction 2026-05-28 mid-session: "thêm rules không đợi CI docs only nữa, tiếp tục làm việc và re-check sau khi CI done". Triggered by recurring pattern in current session — agent block-waited ~15 min total across 3 separate `gh pr checks --watch` foreground invocations cho PRs #1920/#1916/#1921. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged + concrete session evidence multi-PR sequencing) → Classify ✓ (no existing rule covers WHILE-CI-runs workflow; `docs-only-pr-auto-merge.md` covers WHEN-CI-green merge gate only) → Rule+Enforce ✓ (this file + reviewer-checklist + worked self-test §7 on session originating PRs + paired same-PR with output-review-mandate.md §3 row + rules-index.csv row per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§7 worked example trên session PRs — rule fires correctly + counterfactual ~5 min saved per docs-only PR) → Retro Log ✓ (this entry). META P2 force-multiplier per `meta-gap-priority.md` §3 — fix 1 chuẩn → mọi docs-only PR subsequent auto-comply prospectively. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — extends previously-uncovered WHILE-CI-runs workflow dimension; no constraint loosening; existing PR practices grandfathered until next refresh; rule applies prospectively từ this PR forward 2026-05-28). Atomic-unique-bar §5.1 check passed: ✅ atomic (single concept: no block-wait on docs-only CI) + ✅ unique (sister `docs-only-pr-auto-merge.md` covers merge-gate, this covers wait-behavior) + ✅ widely applicable (every docs-only PR creation) + ✅ body discipline §1 ≤2 "and" conjunctions. PR template + memory auto-load + CI detector all deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions; reviewer-checklist + worked self-test §7 sufficient cho v1.0.0.
