---
title: Wave 75 Bucket C — Hook Ordering Empirical Test
status: complete
created: 2026-05-14
phase: wave-75
wave: 75
bucket: C
related_gaps: []
---

# Wave 75 Bucket C — Hook Ordering Empirical Test

## 1. Scope

Empirical investigation về behaviour của Claude Code khi nhiều hooks cùng listen 1 event. Per Wave 74 outside-in benchmark "Nhóm 3 CRITICAL #1" — concern rằng `pre-tool-guard.py` và `audit-gate.py` có race condition khi same event fire.

Wave plan: `documents/03-planning/waves/wave-2026-05-14-75-meta-finish.md` §"Bucket C — Hook ordering empirical test"

Outside-in source: `documents/04-quality/audits/meta/2026-05-14-wave-74-outside-in-benchmark.md`

## 2. Methodology

1. **Audit hiện trạng wiring** — đọc `.claude/settings.local.json`, đếm hooks per event
2. **Research Anthropic docs** — WebFetch `https://code.claude.com/docs/en/hooks` + WebSearch "Claude Code hooks execution order multiple hooks"
3. **GitHub feature request** — WebFetch issue #21533 (closed) về sequential hook execution
4. **Synthetic test** — `.claude/hooks/tests/test-hook-ordering.py` (11 tests, subprocess invocation pattern)

## 3. Findings

### 3.1 Wiring hiện tại (sau Wave 73 Bucket B + Wave 75)

Đọc `.claude/settings.local.json`:

| Event | Matcher | Số hooks | Hooks |
|-------|---------|---------|-------|
| `PreToolUse` | `Bash|Edit|Write` | **1** | `pre-tool-guard.py` |
| `PostToolUse` | `Bash` | **2** ⚠️ | `audit-gate.py` + `post-tool-guard.py` |
| `Stop` | (all) | 1 | `stop-handoff-check.py` |
| `UserPromptSubmit` | (all) | 1 | `inject-rule-digest.py` |

**Đính chính outside-in benchmark:** outside-in artifact 2026-05-14 nói `pre-tool-guard.py + audit-gate.py` cả 2 listen Bash — **không chính xác**. Thực tế:
- `pre-tool-guard.py` chỉ ở **PreToolUse** (fires TRƯỚC khi Claude execute tool)
- `audit-gate.py` chỉ ở **PostToolUse** (fires SAU khi tool execute xong)
- Hai hook này KHÁC event → không phải multi-hook same-event case → KHÔNG có race

**Multi-hook same-event case THỰC SỰ:** `PostToolUse Bash` có 2 hooks (`audit-gate.py` + `post-tool-guard.py`). Đây là case cần investigate.

`post-tool-guard.py` header comment xác nhận: *"Coexists with audit-gate.py (the existing PostToolUse hook). Anthropic supports multiple hooks per event — both run on PostToolUse."*

### 3.2 Behaviour theo Anthropic docs

**Quote chính thức từ `https://code.claude.com/docs/en/hooks`:**

> "All matching hooks run in parallel, and identical handlers are deduplicated automatically."

Implications:
- **Execution model:** PARALLEL (không serial theo array order)
- **Deduplication:** identical command strings deduped tự động
- **Stdin/stdout chain:** KHÔNG có — mỗi hook nhận stdin riêng từ Claude Code runtime, stdout là response cho Claude (không feed sang hook kế)
- **Order:** non-deterministic giữa multiple matching hooks

**Behaviour KHÔNG documented:**
- BLOCK từ hook A có skip hook B không? — docs không nói rõ
- Race condition khi 2 hooks update cùng tool input — outside-in source nói "last one to finish wins" (non-deterministic)

### 3.3 Sequential option (closed feature request)

GitHub issue [#21533](https://github.com/anthropics/claude-code/issues/21533) đề xuất `sequential: true` flag cho ordered execution. **Status:** *"Closed as not planned"* — Anthropic không có kế hoạch implement.

Implication: dự án phải design hooks **independent + idempotent**, không dựa vào order.

### 3.4 Empirical test results

`python3 .claude/hooks/tests/test-hook-ordering.py` — **11/11 PASS** (0.321s):

| Test class | Tests | Verdict |
|-----------|------|---------|
| `TestSettingsWiring` | 3 | ✅ — PreToolUse 1 hook, PostToolUse Bash 2 hooks confirmed |
| `TestHookIndependence` | 4 | ✅ — both hooks exit 0 independently dù invocation order |
| `TestHookStdoutShape` | 2 | ✅ — stdout là valid JSON or empty (không chain shape) |
| `TestFailSafe` | 2 | ✅ — malformed input không crash hook |

Test SYNTHETIC (subprocess invoke) — không integration với real Claude Code lifecycle.

## 4. Risk assessment

### 4.1 Race conditions trong code hiện tại

Phân tích `audit-gate.py` + `post-tool-guard.py` về shared state:

| Resource | audit-gate.py | post-tool-guard.py | Race? |
|----------|--------------|---------------------|-------|
| `documents/03-planning/pr-logs/PR-*.json` | WRITE | (không touch) | ❌ disjoint |
| `git` subprocess calls | READ (gh CLI) | READ (git diff/log) | ❌ read-only |
| `.claude/hooks/data/*` | (không touch) | (không touch trong PostToolUse path) | ❌ disjoint |
| stdout | independent JSON | independent JSON | ❌ parallel-safe |

**Verdict:** không có shared mutable state giữa 2 PostToolUse hooks. Parallel execution **không tạo race**.

### 4.2 Anthropic-side limitation

Parallel execution model có 5 implications được docs liệt kê (đã ghi nhận tại §3.2 + #21533):
1. Dependent transformations: Hook B không thể process Hook A's output
2. Ordered validation: security check phải finish trước format check
3. Pipeline processing: data không flow sequential
4. Priority-based execution: critical hooks không block optional
5. Resource conflicts: multiple hooks modifying cùng file concurrently

**Áp dụng cho dự án này:** chỉ #5 (resource conflicts) là risk thực — đã verify §4.1 không có shared writes.

### 4.3 PreToolUse vs PostToolUse semantic

`pre-tool-guard.py` chạy **TRƯỚC** Claude execute tool. Nếu nó BLOCK (return `permissionDecision: deny`), Claude **không execute tool** → `audit-gate.py` / `post-tool-guard.py` **không fire**. Đây là expected behaviour, không phải race.

## 5. Recommendation

**Ordering currently safe — close concern.**

Cụ thể:
- `PreToolUse` chỉ có 1 hook → trivially safe
- `PostToolUse Bash` 2 hooks → parallel execution **safe** vì không shared mutable state (§4.1)
- Anthropic docs xác nhận parallel-by-default; sequential option closed-as-not-planned

**Cross-link recommendation cho Bucket B agent:**

Hook-review rubric (`.claude/skills/quality/hook-review/reference/rubric-checklist.md`) nên có 1 rubric point mới (Bucket B agent quyết định wording):

> **R-X. Multi-hook same-event safety:** Nếu rule mới thêm hook listening event đã có hook khác, hook mới PHẢI:
> (a) Verify không share mutable state với hook hiện hữu (file writes, env vars, subprocess locks)
> (b) Idempotent — multiple invocations same payload cho same result
> (c) Fail-safe — error không crash; exit 0 silently
> (d) Document trong hook header comment: "Coexists with X.py" + liệt kê hooks đã có cùng event

Pattern này đã được `post-tool-guard.py` áp dụng (header comment minh hoạ §3.1).

## 6. Acceptance Criteria (Bucket C)

- [x] Current hook wiring catalogued (§3.1)
- [x] Anthropic doc behavior documented (§3.2 — "All matching hooks run in parallel")
- [x] Empirical test PASS — 11/11 (§3.4)
- [x] Recommendations explicit (§5)
- [x] Cross-link suggestion cho hook-review rubric (§5)
- [x] Vietnamese narrative + English identifiers

## 7. Limitations + future work

### 7.1 Test scope limitation

Synthetic test invoke subprocess — không reproduce:
- Actual Claude Code runtime parallel dispatch
- BLOCK propagation behavior (hook A BLOCK → hook B skip?)
- Stdin/stdout chaining (verified docs say "no" but no runtime reproduction)

### 7.2 Bucket D scope handoff

Concurrent race condition deep-dive (filesystem writes race khi 2 hooks parallel modify cùng PR log file) tracked Bucket D — agent đó sẽ deep-dive `pr-logs/PR-*.json` write semantics.

### 7.3 Wave 76 follow-up (nếu cần)

Không cần Wave 76 follow-up gap. Ordering hiện safe; Anthropic limitation acknowledged + recommended pattern documented.

## 8. References

- [Anthropic Claude Code Hooks docs](https://code.claude.com/docs/en/hooks)
- [GitHub Issue #21533 — Sequential Hook Execution Option (closed)](https://github.com/anthropics/claude-code/issues/21533)
- Wave 75 plan: `documents/03-planning/waves/wave-2026-05-14-75-meta-finish.md`
- Outside-in source: `documents/04-quality/audits/meta/2026-05-14-wave-74-outside-in-benchmark.md`
- Test: `.claude/hooks/tests/test-hook-ordering.py`
- Hooks under review:
  - `.claude/hooks/pre-tool-guard.py` (Wave 73 Bucket B)
  - `.claude/hooks/audit-gate.py` (pre-existing)
  - `.claude/hooks/post-tool-guard.py` (Wave 73 Bucket B Rules 6 + 7)
