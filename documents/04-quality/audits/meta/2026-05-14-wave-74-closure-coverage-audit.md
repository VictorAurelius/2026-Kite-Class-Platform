---
title: Wave 74 Closure — Hook Coverage Audit (inside-out + outside-in)
status: complete
created: 2026-05-14
phase: post-wave-74
wave: 74
gaps_filed: [GAP-529]
prs: ["#1320", "#1321", "#1322", "#1323"]
---

# Wave 74 Closure — Hook Coverage Audit

## Scope

Đóng Wave 74 — Hook Coverage. Audit tổng hợp:
- 4 PR shipped (1 plan + 3 buckets song song)
- Inside-out rubric 8 điểm (Bucket A skill)
- Outside-in benchmark audit (8 dự án ngành) — surface 32% coverage gap
- Hook bug discovery (Bucket C empirical) → GAP-529 P1

## Wave outcome

| Bucket | PR | Merge SHA | Result |
|---|---|---|---|
| Plan | #1320 | `c09b675d` | wave plan merged (admin-merge với trailer do Vercel rate-limit) |
| A — Hook Review Skill | #1322 | `45efea57` | 4 files: SKILL.md (61 lines) + rubric-checklist (8 points) + edge-case-catalog (9 EC entries) + index entry |
| B — audit-gate.py tests | #1321 | `f8bbaace` | 23 tests covering 7 decision functions (is_docs_only, has_audit_override, has_domain_milestone_*, check_gap_doc_drift, compute_score, detect_pr_merge, AUDIT_RULES match) |
| C — Edge tests | #1323 | `d111d40a` | 17 tests added: 10 post-tool-guard + 6 stop-handoff-check + 1 bonus pre-tool-guard documenting hook bug |
| Outside-in benchmark | n/a (artifact) | `documents/04-quality/audits/meta/2026-05-14-wave-74-outside-in-benchmark.md` | 32% coverage gap surfaced; 5 HIGH + 5 sharpening + 3 CRITICAL classes |
| D — Closure | (this PR) | (TBD) | audit artifact + Wave 75 stub + wave-history append + plan flip |

Wall-clock total: ~3h (3 buckets parallel + benchmark parallel + closure serial). Vs serial estimate ~6-8h.

## Test counts post-Wave-74

| Hook | Pre-W74 tests | Post-W74 tests | Delta |
|---|---:|---:|---:|
| `audit-gate.py` (779 lines) | 0 ❌ | 23 ✅ | +23 |
| `pre-tool-guard.py` (243 lines) | 20 | 21 | +1 (bonus bug doc) |
| `inject-rule-digest.py` (260 lines) | 20 | 20 | 0 (out of scope) |
| `post-tool-guard.py` (164 lines) | 6 ⚠️ | 16 | +10 |
| `stop-handoff-check.py` (142 lines) | 7 ⚠️ | 13 | +6 |
| `session-lock-guard.py` (195 lines) | 4 (bash) | 4 (bash) | 0 (out of scope) |
| **Total** | **57** | **97** | **+40** |

Coverage qualitative shift: HIGH-risk `audit-gate.py` từ "0 tests, 779 lines" → "23 tests, key decision branches covered". MEDIUM-risk hooks sharpened với edge cases.

## Outside-in findings absorbed (per Bucket D fold-in strategy)

### Nhóm 1 — HIGH-confidence additions (absorb vào skill via follow-up)

Outside-in benchmark surface 5 additions ngành chuẩn nhưng Wave 74 inside-out miss. Wave 74 Bucket A skill chưa cover — sẽ extend qua **Wave 75 follow-up PR** (sửa nhỏ skill, không phải mini-wave):

1. **Bảng exit code rõ ràng + cảnh báo `exit 1` trap** — non-blocking hooks có thể bị BLOCK accidentally
2. **True-positive + true-negative fixture parity per BLOCK condition** (chuẩn ESLint)
3. **Stdin malformed JSON test** — fail-safe degradation
4. **JSON stdout contract schema compliance** (khớp Anthropic spec)
5. **Hardware-pinned performance baseline cold-start vs steady-state**

Track Wave 75 task: extend `hook-review/reference/rubric-checklist.md` + `edge-case-catalog.md`.

### Nhóm 2 — Sharpening existing 5 points

Tương tự, Wave 75 PR cùng follow-up: enumerate cụ thể event types, exit code mapping, Semgrep-style fixture annotations (`# ruleid:` / `# ok:` / `# todo:`), fail-safe degradation cases, settings precedence chain.

### Nhóm 3 — 3 CRITICAL untested classes (Wave 75 P0 candidates)

1. **Hook ordering** — multi-hook match same event → race? Anthropic chưa document. KiteHub có 2 hooks listen Bash (`pre-tool-guard.py` + `audit-gate.py`). Empirical test needed.
2. **Concurrent race conditions** — wave-pack parallel agents = N concurrent tool calls = race trên hook state (audit-gate.py ghi `pr-logs/*.json`; inject-rule-digest.py có thể cache). Real production bug risk.
3. **Coverage measurement** — `coverage.py` + mutation testing chưa setup. Không biết % branch chưa test trong 779-dòng audit-gate.py.

→ Each = Wave 75 candidate. File separately khi prioritize.

## Hook bug discovered → GAP-529

Agent C bonus test surface bug: `_has_trailer()` reads HEAD commit body → trailer leaks per-branch-derivation. Worked incident: PR #1320 legit trailer trên `c09b675d` → branches derive từ commit đó (vd `wave-74-bucket-c-edge-tests`) → hook silent-allow `gh pr merge --admin` trên branch đó.

Severity P1 (không P0) do main HEAD `45efea57` đã move past trailer commit → hook works correctly trên main. Bug class: per-branch leak, not "stuck-on-main-forever".

GAP-529 filed for fix Wave 75: scope trailer per-PR (đọc `gh pr view <N> --json body`), không phải per-HEAD.

## Closure protocol completed

- [x] Audit artifact (this file)
- [x] GAP-529 filed + gap-status.csv row appended (CSV validator PASS)
- [ ] Wave 74 plan frontmatter `status: complete`
- [ ] `wave-history.jsonl` append
- [ ] `bash scripts/prune-merged-worktrees.sh --yes` (cleanup)
- [ ] PR with this audit + plan flip + history append → CI green → squash merge

## Recommendations

1. **Ship this closure PR** — docs-only, eligible auto-merge per `docs-only-pr-auto-merge.md`
2. **File 3 Wave 75 gaps:** hook ordering empirical test + concurrent race investigation + coverage measurement setup
3. **Quick follow-up PR (≤1 day):** extend `hook-review/reference/` với Nhóm 1 (5 HIGH-conf) + Nhóm 2 (5 sharpening). Per `release-fix-retry-budget.md` retry #1 = root-cause fix OK.
4. **Wave 75 P0:** GAP-529 hook trailer scope bug — Phase 1 (per-PR trailer detection) + Phase 2 (refactor `_has_trailer_in_pr` shared helper) + Phase 3 (regression test stub `_commit_body`)

## Real-measurement / Self-test verification

| Check | Status |
|---|---|
| 4 PR merged sequence (#1320 plan → #1321 B → #1322 A → #1323 C) | ✅ |
| All bucket PRs CI green except Vercel rate-limit (24h quota, infra issue) | ✅ |
| Local: `python3 .claude/hooks/tests/test-*.py` (4 suites) | ✅ ALL PASS (50 tests Bucket C run; B 23 tests own suite; A no tests) |
| `bash scripts/check-gap-status-csv.sh` 350 rows | ✅ PASS |
| `bash scripts/check-rule-frontmatter.sh` | ✅ PASS |
| Hook trailer scope bug verified main-HEAD-clean | ✅ (`gh pr merge --admin` BLOCKED on `45efea57`) |

## References

- Wave plan: `documents/03-planning/waves/wave-2026-05-14-74-hook-coverage.md`
- Outside-in benchmark: `documents/04-quality/audits/meta/2026-05-14-wave-74-outside-in-benchmark.md`
- Bug gap: `documents/04-quality/gaps/GAP-529-hook-trailer-scope-bug.md`
- Skill shipped: `.claude/skills/quality/hook-review/SKILL.md` + reference docs
- Rules invoked: `outside-in-coverage-trigger.md` (rule miss + retroactive fix), `pre-handoff-self-test-completeness.md`, `agent-background-spawn-default.md`, `docs-only-pr-auto-merge.md`, `admin-merge-discipline.md` (worked false-positive case)
