---
paths:
  - "documents/04-quality/**"
  - ".claude/rules/**"
  - ".claude/skills/**"
  - ".github/workflows/**"
---

# CI Queue → Local Runner Threshold — run quality jobs locally khi đáp ứng tiêu chí

**Priority:** 🟠 MANDATORY — CI compute + wait-time governance
**Version:** 1.0.0
**Created:** 2026-05-26
**Last-Reviewed:** 2026-05-26
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + worked self-test on Wave rst-cascade-1 4-PR experience 2026-05-26) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-implicit best practice surfaced khi 3/4 PRs CI fail same audits-index.csv issue caught by local pre-flight)
**Applies to:** Mọi PR creation step — Claude PHẢI evaluate §2 trigger pattern trước khi push branch + open PR

---

## 1. The Rule

> **Khi PR diff fully covered by local-equivalent scripts (docs-only OR CI queue >5 concurrent runs in_progress) OR hotfix urgency, Claude PHẢI run local CI parity TRƯỚC khi push branch. CI vẫn chạy as backup; local-CI = filter, CI = canonical.**

Local-CI parity = run same scripts CI invokes locally on YOUR worktree state. Saves CI queue wait time + early-catch fails before reviewer notice + reduces CI compute waste cho jobs that local can fully cover.

CI là source of truth (canonical). Local-CI là smart filter để avoid round-trip retry cycle khi local quickly detects an issue CI would flag anyway.

---

## 2. Trigger pattern — khi nào local-CI mandatory

| Pattern | Local mandatory? | Cost-benefit |
|---|---|---|
| **Docs-only PR** (diff ∈ `documents/**` + `.claude/rules/**` + `.claude/skills/**` + `*.md` only) | ✅ YES | Save 5-10 min queue wait + free CI compute |
| **CI queue heavy** (`gh run list --status in_progress` >5 concurrent) | ✅ YES | Avoid queue contention; local-CI = 0 wait |
| **Hotfix urgency** (P0 production incident) | ✅ YES | Speed-to-merge critical |
| **Parallel batch PRs** (vd Wave parallel buckets ≥3 PRs same wave) | ✅ YES — local first | Avoid sequential queue-thrash |
| **Code PR ≤20 LOC + only `**/*.md` references** | ✅ YES | Diff fully docs-equivalent |
| **Code PR > 20 LOC touching `**/*.java`/`**/*.ts`/`**/*.tsx`** | ❌ NO | CI = canonical source for build+test verification |
| **Workflow change** (`.github/workflows/**`) | ❌ NO | CI logic change must be verified by CI |
| **New CI job/script chưa stable** | ❌ NO | CI = source of truth cho new infrastructure |
| **Migration PR** (`*.sql`/Flyway migration) | ❌ NO | DB-bound verification needs full CI environment |

Rule **KHÔNG** fire khi PR diff includes any out-of-scope class (code/migration/workflow).

---

## 3. Required action when rule fires

### Bước 1: Identify workflow jobs cho PR diff

Per `.github/workflows/` `paths:` filters, determine which jobs CI will trigger:

```bash
# For docs PRs touching documents/**:
# → quality-docs.yml triggers (8 scripts):
#   check-readme-freshness.sh / check-gap-status-csv.sh / check-gap-folder-location.sh
#   check-audits-index-csv.sh / check-wave-plan-completeness.sh
#   check-3-layer-completeness.sh / check-docs-archival-stale.sh
#   check-docs-folder-volume.sh / check-docs-subfolder-maturity.sh
```

### Bước 2: Run all relevant scripts locally

```bash
FAIL=0
for s in check-gap-status-csv check-gap-folder-location check-audits-index-csv \
         check-wave-plan-completeness check-3-layer-completeness \
         check-docs-archival-stale check-docs-folder-volume check-docs-subfolder-maturity; do
  bash "scripts/$s.sh" > /tmp/local-ci.log 2>&1
  RC=$?
  [ $RC -eq 0 ] && echo "✅ $s" || { echo "❌ $s"; tail -3 /tmp/local-ci.log; FAIL=$((FAIL+1)); }
done
[ $FAIL -eq 0 ] && echo "🟢 PASS — ready to push" || echo "🔴 $FAIL FAIL"
```

### Bước 3: Document local-CI evidence trong PR body

Add section `## Local CI parity (per ci-queue-local-runner-threshold.md)`:

```markdown
## Local CI parity

N/N scripts PASS locally before push (saves ~5-10 min CI queue wait):
- check-script-A ✅ / check-script-B ✅ / ...
```

### Bước 4: Push only sau khi local 100% PASS

If local fail → fix + re-run local → only push when 100% PASS.

### Bước 5: CI still runs as canonical backup

Local-CI does NOT replace CI. CI confirms canonical result. If local pass + CI fail → investigate divergence (likely flaky CI, runner state issue, OR script behavior difference between local + CI environment).

---

## 4. Force-multiplier rationale

Wave rst-cascade-1 4-PR experience 2026-05-26 demonstrated:
- 3/4 PRs đáng lẽ fail CI vì missing `audits-index.csv` row (cluster-1, cluster-3, cluster-4 worktree agents miss CSV row mandate)
- Local pre-flight (per `meta-csv-index-pattern.md` 100% coverage parity script) caught all 3 BEFORE CI report
- Saved ~3 × 5 min CI retry cycle = ~15 min wall-clock
- Plus 1 CI flake on `check-wave-plan-completeness.sh` (self-hosted runner concurrent race) → local-CI confirms local PASS → re-trigger CI bypass

Per `meta-gap-priority.md` §3 META P1 force-multiplier — 1 chuẩn local-CI parity → mọi PR subsequent auto-comply prospectively → eliminate retroactive CI retry cycle cost.

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Push docs-only PR + chờ CI report fail | Local-CI run TRƯỚC khi push; fix + re-run local; push when 100% PASS |
| Skip local-CI "vì CI sẽ catch" | CI queue wait + retry cycle = 5-10 min per round-trip; local = ~30s |
| Use `--admin` flag để bypass CI gate after local PASS | BANNED per `admin-merge-discipline.md`; CI vẫn là canonical |
| Run local-CI nhưng không document trong PR body | PR body section `## Local CI parity` mandatory cho transparency |
| Treat local PASS as authoritative khi CI fail | CI = canonical source; local PASS + CI fail = investigate divergence (don't override) |
| Skip CI entirely (push --no-verify or branch protection bypass) | CI runs unconditionally; local-CI is filter not replacement |
| Run local-CI cho code PR (>20 LOC Java/TS) | Code PR needs full CI build+test environment; local-CI scope limited to docs scripts |

---

## 6. Worked self-test — Wave rst-cascade-1 (2026-05-26 origin incident)

Retroactive apply rule to 4-PR session:

### 6.1 PR #1861 (Cluster 1 Email)
- Diff scope: `documents/04-quality/audits/quality/*.md` (NEW) + `documents/04-quality/gaps/gap-status.csv` + 1 gap file move → **docs-only** ✅
- Rule §2 trigger: YES (docs-only)
- Original behavior: pushed without local-CI; CI ran; pre-flight detected missing `audits-index.csv` row only AFTER seeing CI fail elsewhere
- Counterfactual với rule: local-CI run reveals missing row → fix + push → CI green first try → save ~5 min

### 6.2 PR #1862 (Cluster 2 Auth+admin)
- Diff scope: docs-only ✅
- Rule §2 trigger: YES
- Cluster 2 agent (verify) ran local-CI implicitly + included `audits-index.csv` row in commit → CI green first try
- Demonstrates rule already works in practice → make explicit

### 6.3 PR #1863 (Cluster 3 Onboarding)
- Diff scope: docs-only ✅
- Rule §2 trigger: YES
- Original behavior: pushed without local-CI → CI fail on `audits-index.csv` missing row
- Counterfactual với rule: local-CI catches BEFORE push → save ~5 min retry

### 6.4 PR #1864 (Cluster 4 Infra+UI)
- Diff scope: docs-only ✅
- Rule §2 trigger: YES
- Original behavior: same as #1863 — local pre-flight caught missing row AFTER initial push
- Counterfactual với rule: local-CI catches BEFORE push

### 6.5 Aggregate self-test

| PR | Original outcome | With rule applied |
|---|---|---|
| #1861 | CI fail catch via parallel pre-flight | Push 1 PASS — save 5 min retry |
| #1862 | Already followed rule | Same |
| #1863 | CI fail catch via parallel pre-flight | Push 1 PASS — save 5 min retry |
| #1864 | CI fail catch via parallel pre-flight | Push 1 PASS — save 5 min retry |

**Save**: ~3 × 5 min = 15 min wall-clock + 3 × ~21 CI jobs queue contention eliminated = ~63 CI job slots freed.

**Verdict**: Rule fires correctly trên 4 PR originating incidents. Self-test PASS ✅.

---

## 7. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 7.1 Reviewer-checklist (active now)

Pre-merge review cho PR creation step:

- [ ] PR diff fits §2 trigger pattern (docs-only / queue heavy / hotfix / batch parallel / code ≤20 LOC docs-equivalent)?
- [ ] Nếu CÓ — PR body có section `## Local CI parity` với N/N scripts PASS list?
- [ ] Nếu KHÔNG match trigger — pass through to CI canonical flow (no local-CI required)?
- [ ] CI canonical run still triggered (rule không bypass CI)?

### 7.2 PR template extension (deferred per `incident-to-rule-pipeline.md` §3.1)

Future enhancement: add row `.github/PULL_REQUEST_TEMPLATE.md` Output Review section:
```markdown
- [ ] **Local CI parity** (if PR matches `ci-queue-local-runner-threshold.md` §2 trigger): `## Local CI parity` section trong PR body documents local script PASS list
```

Defer ≥7 ngày post-rule landing per `incident-to-rule-pipeline.md` §3.1 tightened conditions (reviewer-checklist + worked self-test sufficient cho v1.0.0).

### 7.3 CI grep detector (deferred per `incident-to-rule-pipeline.md` §3.1)

Future detector: scan recent PR bodies for docs-only diff signal but missing `## Local CI parity` section → WARN. Defer until recurrence-count ≥2 post-rule.

Heuristic regex (when eventually implemented):

```bash
# Detect docs-only PR without Local CI parity section
gh pr view <N> --json files,body --jq \
  'select(.body | contains("## Local CI parity") | not) | select(.files | all(.path | test("^(documents|\\.claude/(rules|skills))/")))'
```

### 7.4 Memory auto-load (optional, deferred)

Memory entry `feedback_ci_queue_local_runner_threshold.md` could remind tại session start trước push step. Defer per premature-rule guard ≥7 ngày; reviewer-checklist + worked self-test §6 đủ cho v1.0.0.

### 7.5 Override mechanism

Genuine exception (vd local environment broken, CI is only viable verifier):

```
git commit -m "...
CI_LOCAL_RUNNER_OVERRIDE: <reason — e.g., 'local Docker broken; CI canonical fallback'>
CI_LOCAL_RUNNER_FOLLOWUP: <gap link để fix local environment khi feasible>"
```

Trailer logged trong quarterly retro. Pattern frequency >10%/quarter triggers meta-review (likely local env reliability issue OR rule scope mis-defined).

---

## 8. Relationship to other rules

- **`docs-only-pr-auto-merge.md`** §2 scope = identical class to this rule §2 row 1 (docs-only PR). Both apply same diff scope. Auto-merge mandate triggers AFTER local-CI confirms PASS + CI confirms green.
- **`admin-merge-discipline.md`** v1.0.3 — `--admin` flag BANNED post-rebase. This rule complements: local-CI is the pre-push gate, NOT a substitute cho CI canonical gate via `--admin`.
- **`mcp-first-with-fallback.md`** §2 — MCP → dedicated tools → Bash tier priority. Local-CI scripts via Bash (Tier 3) là legitimate use case.
- **`agent-action-bias.md`** §1 Part A — "do it yourself". This rule extends: do CI verification yourself locally khi feasible.
- **`gap-done-discipline.md`** — production-equivalent verify required cho DONE flip. Local-CI parity helps catch CSV/format issues that block DONE flip ship.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test all paired same PR (Wave rst-cascade-1 closure).
- **`output-review-mandate.md`** §3 — paired same-PR new matrix row "CI queue threshold → local runner".
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier (1 chuẩn → mọi PR subsequent auto-comply prospectively).
- **`incident-to-rule-pipeline.md`** — this rule = direct output of 2026-05-26 user-flagged "CI queue lâu nhỉ, nên runner local không?" applied through 5-stage pipeline.
- **`context-budget-mandate.md`** §3.2 — path-scoped `paths: [documents/**, .claude/rules/**, .claude/skills/**, .github/workflows/**]` (rule loads only khi PR creation context touches these paths).

---

## 9. Log

- **2026-05-26 (v1.0.0):** Rule created in response to user direction 2026-05-26 mid-Wave-rst-cascade-1 closure: "WF Quality lâu nhỉ, nên runner local không?" + follow-up "đưa về chạy hết ở local để tránh đợi queue mất thời gian" + threshold confirm "Balanced: docs-only always local + queue >5 concurrent". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged CI wait friction during Wave rst-cascade-1 4-PR session) → Classify ✓ (no existing rule codifies local-CI parity policy; `docs-only-pr-auto-merge.md` covers merge gate only, not pre-push CI runner choice; `mcp-first-with-fallback.md` covers tool selection, not CI runner choice) → Rule+Enforce ✓ (this file + reviewer-checklist + worked self-test §6 on Wave rst-cascade-1 4-PR origin incidents + paired same-PR with closure audit + `output-review-mandate.md` §3 row + `rules-index.csv` row per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example on 4 originating PRs — rule fires correctly + counterfactual ~15 min wall-clock + ~63 CI job slots saved) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — fix 1 chuẩn → mọi PR subsequent auto-comply prospectively. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-implicit best practice surfaced during Wave rst-cascade-1; no constraint loosening for prior PRs; existing PRs grandfathered until next refresh; rule applies prospectively từ this PR forward 2026-05-26). Atomic-unique-bar §5.1 check passed: ✅ atomic (single concept: CI queue threshold → local runner) + ✅ unique (sister `docs-only-pr-auto-merge.md` covers merge-time, this rule covers pre-push-time) + ✅ widely applicable (every PR creation step) + ✅ body discipline §1 ≤2 "and" conjunctions. PR template + CI detector + memory auto-load deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions; reviewer-checklist + worked self-test §6 sufficient cho v1.0.0.
