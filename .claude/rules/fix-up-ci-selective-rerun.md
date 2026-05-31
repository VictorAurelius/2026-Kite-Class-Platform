---
paths:
  - ".github/workflows/**"
  - ".claude/rules/fix-up-ci-selective-rerun.md"
audience: dev
---

# Fix-up CI Selective Re-Run — cancel unrelated CI checks on targeted fix commits

**Priority:** 🟠 MANDATORY — CI throughput + wait-time governance
**Version:** 1.0.1
**Created:** 2026-05-24
**Last-Reviewed:** 2026-05-31
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement (reviewer-checklist + worked self-test on Wave br-4 Bucket E fix-up incident 2026-05-24) per `rule-change-process.md` §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-implicit "wait full CI" anti-pattern surfaced by user 2026-05-24)
**Applies to:** Every fix-up commit pushed to an existing PR branch where the previous CI run had ≥1 specific check FAIL + ≥1 check PASS. Out-of-scope: initial PR push (all checks needed); rebase/squash that changes many files (broad re-test warranted)

---

## 1. The Rule

> **Khi push fix-up commit lên existing PR branch để fix MỘT specific failed CI check, agent PHẢI: (a) cancel other in_progress / queued checks không relevant với fix scope, (b) wait only for the targeted check + any check whose path trigger matches the fix-up diff.**

Re-running 30 checks khi chỉ fix 1 = waste CI minutes + waste agent wait time. Most checks didn't see new code paths trong fix-up commit; rerunning gives same result.

Surface by user 2026-05-24 sau Wave br-4 Bucket E fix-up (#1785 commit `9c30fe70` — chỉ touch gap-status.csv + gap file rename, but ALL 31 CI checks re-ran).

---

## 2. Trigger pattern — khi nào rule fire

Rule fire khi:

| Pattern | Ví dụ |
|---|---|
| Push commit lên existing PR branch | `git push` sau commit nhỏ fix CI fail |
| Previous CI run có ≥1 FAIL + multiple PASS | PR #1785 trước fix-up: 30 PASS + 1 FAIL (Gap status CSV) |
| Fix-up diff narrow (≤5 files) | gap-status.csv + 1 rename = 2 files |
| Fix-up scope clearly targeted ONE failing check | CSV row missing → fix CSV validator only |

Rule **KHÔNG** fire khi:
- Initial PR push (no previous run to compare)
- Rebase trên main với many files changed (broad re-test warranted)
- Fix-up touches files trong multiple workflow path triggers (vd chạm Java + scripts + docs)
- Previous CI 100% FAIL (re-run all needed)

---

## 3. Required action sequence

Khi rule fires (agent push fix-up commit cho PR có previous fail):

### Bước 1: Identify target check + relevant paths

Pre-push, agent phải biết:
- Which check failed previously (e.g., `Gap status CSV`)
- Which workflow runs that check (e.g., `quality-rules-skills.yml` job `gap-status-csv`)
- Which path triggers that workflow (e.g., `documents/04-quality/gaps/**`)

### Bước 2: Push fix-up

Standard `git push`. GitHub will trigger full workflow set per push event.

### Bước 3: Identify cancel candidates (post-push)

Via `mcp__github__list_workflow_runs` or `gh run list`, list in_progress / queued workflow runs cho new commit SHA. For each:

- ✅ **KEEP** (wait completion): workflow contains the previously-failed check OR workflow's path trigger matches fix-up diff
- ❌ **CANCEL**: workflow's path trigger does NOT match fix-up diff (e.g., kitehub-frontend tests when fix-up only touched gap CSV)

### Bước 4: Cancel unrelated

```bash
# Per identified workflow run ID:
gh run cancel <run-id>
# OR MCP equivalent if available
```

For each canceled run, log reason in agent message:
> "Canceled <workflow-name> run <id> — path trigger <pattern> không match fix-up diff (<files>)."

### Bước 5: Wait only on kept runs

Poll only the kept workflows. Typically:
- Original failing check (verify fix passes)
- 1-2 broad workflows (gitleaks always re-runs; lint cũng cheap)

### Bước 6: Report to user

After fix verification:
> "Fix-up verified: <check-name> PASS. Canceled N unrelated re-runs (saved ~N min CI minutes + agent wait time)."

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Wait full CI re-run after CSV-only fix | Cancel kitehub-* test workflows; wait only `Gap status CSV` |
| Cancel critical workflows (gitleaks always) | Gitleaks + secret-scan ALWAYS keep (cheap + security mandate) |
| Cancel workflows that DID change paths | Inspect fix-up diff vs workflow path triggers carefully |
| Skip rule when PR has fail | Rule mandatory per §2 trigger pattern |
| Cancel without logging reason | Each cancel needs explicit path-mismatch evidence in agent message |

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 Self-detection (in-turn)

Sau khi push fix-up commit, agent self-check:
1. Diff narrow ≤5 files?
2. Previous CI có ≥1 fail + multiple PASS?
3. Fix-up scope target ONE check?

Nếu YES all 3 → apply §3 sequence.

### 5.2 Memory auto-load (optional, deferred)

Memory entry `feedback_fix_up_ci_selective_rerun.md` có thể remind tại session start. Defer per `incident-to-rule-pipeline.md` §3.1 tightened defer conditions; reviewer-checklist + self-test §6 sufficient cho v1.0.0.

### 5.3 Reviewer-checklist (manual)

Khi review PR có fix-up commit:
- Agent có cancel unrelated CI re-runs không?
- Cancel decisions có path-trigger evidence?

### 5.4 Override mechanism

Genuine exception (vd fix-up touches multiple subsystems, broad re-test legitimate):

```
git commit -m "...
CI_RERUN_OVERRIDE_ALL: <reason — vd broad refactor touch java+scripts+docs>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review.

### 5.5 CI auto-cancel detector (deferred)

Future: pre-push hook scan fix-up diff vs workflow path triggers → suggest cancel candidates. Defer until 2nd recurrence per `incident-to-rule-pipeline.md` §3 premature-rule guard.

---

## 6. Worked self-test — Wave br-4 Bucket E fix-up #1785 (2026-05-24)

**Bối cảnh:** PR #1785 first CI run had 30 PASS + 1 FAIL = `Gap status CSV`. Fix-up commit `9c30fe70` touched:
- `documents/04-quality/gaps/gap-status.csv` (+1 row)
- `documents/04-quality/gaps/phase-1-beta/GAP-NEW-...md` → `phase-1-beta/closed/GAP-736-...md` (rename + frontmatter edit)

= 2 files, both under `documents/04-quality/gaps/**`

**Apply rule retroactively:**

| Workflow | Path trigger | Fix-up diff match? | Action |
|---|---|---|---|
| `quality-rules-skills.yml` (Gap status CSV + Meta CSV + frontmatter etc.) | `documents/04-quality/gaps/**` + `.claude/rules/**` | ✅ MATCH (gaps touched) | KEEP — wait |
| `core-ci.yml` Test KiteHub Subscription/Admin/Branding/Gateway/Email/Platform | Java service paths | ❌ NO MATCH | CANCEL |
| `frontend-ci.yml` (if triggered) | Frontend paths | ❌ NO MATCH | CANCEL |
| `quality-code.yml` (ShellCheck + Ruff + Script tests + Cross-layer drift + Mermaid) | scripts + .md | partial (rule .md changed in *this* meta rule PR; not in #1785) | KEEP for #1785 (gap .md change triggers) |
| `lint-yaml` (Lint GitHub Actions workflows) | `.github/workflows/**` | ❌ NO MATCH | CANCEL |
| `gitleaks` (secret scan) | always | always | KEEP (security mandate) |

**Counterfactual:** ~28 unrelated checks cancelled → save ~20-30 min CI minutes + reduce agent wait từ ~10 min xuống ~2 min cho `Gap status CSV` complete.

**Verdict:** Rule fires correctly trên originating incident. Self-test PASS ✅

---

## 7. Auto-load justification (per `context-budget-mandate.md` §3.2)

Rule này dùng `paths:` frontmatter (`.github/workflows/**` + self) — path-scoped MANDATORY, KHÔNG always-load. Lý do:
- Rule fires chỉ tại fix-up CI decision moment
- CI workflow context = path trigger relevance
- Token cost ~1k × moments when fix-up commits push (low frequency, but high value)
- Re-evaluate priority bump nếu 3rd recurrence within 60 ngày

---

## 8. Relationship to other rules

- **`docs-only-pr-auto-merge.md`** — sister rule cho post-CI-green auto-merge; rule này covers pre-CI-green selective re-run
- **`mcp-first-with-fallback.md`** §4.1 — GitHub MCP preferred cho cancel operations (mcp__github__cancel_workflow_run if available, else gh CLI)
- **`admin-merge-discipline.md`** — sister rule cho admin merge với override trailer
- **`release-fix-retry-budget.md`** v1.1.0 §4 — pivot matrix; rule này extends với cancel-unrelated discipline
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier (1 chuẩn → mọi fix-up subsequent auto-comply)
- **`incident-to-rule-pipeline.md`** v1.1 — applied 5-stage: Detect ✓ (user-flagged 2026-05-24 Bucket E fix-up wait) → Classify ✓ (no existing rule covers selective CI cancel; sister rules cover related-but-different) → Rule+Enforce ✓ (this file + reviewer-checklist + worked self-test §6 paired same PR per §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 retroactive on Bucket E fix-up — rule fires correctly + counterfactual eliminates ~28 unrelated re-runs) → Retro Log ✓ (§9 below)
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test §6 paired same PR

---

## 9. Log

- **2026-05-31** (v1.0.1): PATCH — fixed 1 stale CI reference(s) `script-quality.yml` → `quality-code.yml` (workflow was split into quality-{code,docs,rules-skills,infra}.yml 2026-05-22; this rule's §Enforcement still pointed to the removed file). Historical Log entries left unchanged per `rule-change-process.md` §7 append-only. No constraint change. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — broken-link fix).

- **2026-05-24 (v1.0.0):** Rule created in response to user-flagged 2026-05-24 Wave br-4 Bucket E fix-up #1785 commit `9c30fe70` — fix-up touched 2 files (gap-status.csv + GAP-NEW rename) but ALL 31 CI checks re-ran (waste ~28 unrelated). User direction "thêm rule chỉ check CI fail xem pass chưa, còn CI khác, không ảnh hưởng thì cancel để tránh phí time wait". Per `incident-to-rule-pipeline.md` v1.1 5-stage applied: Detect ✓ → Classify ✓ (no existing rule mandates selective CI cancel; `docs-only-pr-auto-merge.md` covers post-CI-green flow; `release-fix-retry-budget.md` covers retry budget not selective scope) → Rule+Enforce ✓ (this file + reviewer-checklist + worked self-test §6 + rules-index.csv row) → Self-Test ✓ (§6 retroactive on Bucket E fix-up — counterfactual: ~28 cancels save ~20-30 min CI minutes + ~8 min agent wait time) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint adds previously-uncovered CI throughput discipline; no constraint loosening; existing PR fix-up flows grandfathered; rule applies prospectively từ Wave br-5+ forward). Detector wiring (§5.5 pre-push hook) deferred per `incident-to-rule-pipeline.md` v1.1 §3.1 tightened defer conditions (heuristic complexity moderate + recurrence count 1 + honest defer documented inline); reviewer-checklist + worked self-test + memory mirror sufficient cho v1.0.0.
