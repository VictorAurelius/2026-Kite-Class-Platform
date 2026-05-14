---
title: Wave 75 Bucket E — Hook Coverage Baseline (`.claude/hooks/*.py`)
status: complete
created: 2026-05-14
phase: wave-75
wave: 75
gaps: [GAP-529]
---

# Wave 75 Bucket E — Hook Coverage Baseline

## Scope

Đo baseline line + branch coverage cho 6 hook files Python trong `.claude/hooks/` sau khi Wave 74 đã ship +40 test cases (Bucket B 23 tests + Bucket C +17 tests). Mục tiêu là cung cấp số liệu khách quan để (a) phát hiện vùng untested có nguy cơ regression, (b) bias các wave sau (76+) vào việc nâng floor coverage cho hook nào còn thấp nhất, (c) chuẩn bị wiring CI threshold check.

Out of scope (sẽ ship wave sau):
- Wiring `.github/workflows/script-quality.yml` thêm step coverage threshold (deferred → GAP follow-up đề xuất §"Recommendations" dưới)
- Mutation testing (Wave 76+)
- Bash coverage cho `session-lock-guard.py` test path (`test-session-lock-guard.sh`) — bash test runner không trigger Python `coverage.process_startup()`

## Methodology

### Cấu hình

- `.coveragerc` ở repo root:
  - `source = .claude/hooks`
  - `branch = True` (đo cả line + branch)
  - `parallel = True` + `concurrency = multiprocessing` để gom data từ subprocess

### Subprocess support

Các test suite spawn hook qua `python3 hook.py`. Coverage subprocess được kích hoạt bằng cặp:

1. `COVERAGE_PROCESS_START=$REPO_ROOT/.coveragerc` (env var)
2. Một `sitecustomize.py` shim tạm gọi `coverage.process_startup()`, đặt trên `PYTHONPATH`

Wrapper `.claude/hooks/tests/run-coverage.sh` lo việc tạo shim trong `mktemp -d`, set env, và cleanup khi exit.

### Test suites đã chạy

| # | Suite | Strategy | Tests | Status |
|---|---|---|---|---|
| 1 | `test-audit-gate.py` | 100% in-process (importlib + direct call) | 23 | OK |
| 2 | `test-pre-tool-guard.py` | Subprocess | 25 | OK |
| 3 | `test-post-tool-guard.py` | Mixed (importlib + subprocess) | 16 | OK |
| 4 | `test-stop-handoff-check.py` | Subprocess + tempfile transcripts | 13 | OK |
| 5 | `test-inject-rule-digest.py` | Mixed (importlib + subprocess) | 20 | OK |
| 6 | `test-session-lock-guard.sh` | **bash** — không gom Python coverage | (bash) | N/A |

Total Python tests run: **97**. All passing.

### Lệnh chạy

```bash
bash .claude/hooks/tests/run-coverage.sh        # text report
bash .claude/hooks/tests/run-coverage.sh --html # + HTML drilldown
```

Sản phẩm phụ:
- `.coverage.*` data files (gitignore-eligible — không check vào repo)
- `.claude/hooks/tests/coverage-html/` (nếu `--html`, gitignore-eligible)

## Coverage report — baseline 2026-05-14

```
Name                                  Stmts   Miss Branch BrPart  Cover   Missing
---------------------------------------------------------------------------------
.claude/hooks/audit-gate.py             394    271    166      7  28.2%   140-145, 152-173, 182-185, 192-199, 209-224, 241-247, 251, 257-261, 265-266, 270-274, 279-292, 296-303, 309-315, 339-345, 349-354, 381-394, 399-403, 420, 455, 464, 474, 491, 521-524, 527-528, 545-580, 593-743, 749-775
.claude/hooks/inject-rule-digest.py     119     12     46      4  90.3%
.claude/hooks/post-tool-guard.py         87     13     32      4  85.7%
.claude/hooks/pre-tool-guard.py         136     31     44      6  75.0%
.claude/hooks/session-lock-guard.py      85     85     26      0   0.0%
.claude/hooks/stop-handoff-check.py      67     14     24      3  79.1%
---------------------------------------------------------------------------------
TOTAL                                   888    426    338     24  50.2%
```

### Tóm tắt

| Hook | Stmts | Line % | Branch partial | Verdict |
|---|---:|---:|---:|---|
| `audit-gate.py` (779 lines source) | 394 | **28.2%** | 7/166 | 🔴 LOW — file lớn nhất, untested nhiều nhất |
| `inject-rule-digest.py` | 119 | **90.3%** | 4/46 | 🟢 HIGH |
| `post-tool-guard.py` | 87 | **85.7%** | 4/32 | 🟢 HIGH |
| `pre-tool-guard.py` | 136 | **75.0%** | 6/44 | 🟡 MEDIUM |
| `session-lock-guard.py` | 85 | **0.0%** | 0/26 | 🔴 ZERO (bash test only) |
| `stop-handoff-check.py` | 67 | **79.1%** | 3/24 | 🟡 MEDIUM-HIGH |
| **TOTAL** | **888** | **50.2%** | 24/338 | 🟡 MEDIUM overall |

## Top untested branches / vùng risk cao

### 1. `audit-gate.py:_on_pr_merge_impl` (lines 593–743, ~150 lines)

Function chính của hook khi PR merge được phát hiện. Hiện tại **0% line coverage**. Lý do: function gọi `gh_run` shell-out tới `gh CLI`, viết PR log, đọc rules CSV — Bucket B (Wave 74) đã test các pure helpers nhưng chưa wrap `_on_pr_merge_impl` end-to-end.

**Risk:** đây là code path chạy thực tế trong production khi PR merge. Regression ở đây không có test guard.

**Follow-up đề xuất:** GAP-WAVE-76 "audit-gate.py:_on_pr_merge_impl end-to-end test với gh_run patched fixtures" — ước lượng +12 tests, raise audit-gate line coverage từ 28% → ~60%.

### 2. `audit-gate.py:main` (lines 749–775)

Entry point — đọc stdin, dispatch theo tool name. Untested.

**Follow-up đề xuất:** include trong wave 76 cùng `_on_pr_merge_impl`.

### 3. `audit-gate.py:check_ui_kits_integration` (lines 545–580)

Rule check UI kits integration. Untested. Lower priority vì rule này narrow scope.

### 4. `audit-gate.py` session telemetry & gh wrappers (lines 140–303 cụm rải rác)

- `get_session_started_at` (152–173)
- `get_turn_count` (182–185)
- `build_session_telemetry` (192–199)
- `run_session_lock_guard` (209–224)
- `gh_run` (257–261), `get_pr_files` (265–266), `get_pr_info` (270–274), `get_ci_status` (279–292), `has_recent_audit` (296–303)

**Risk:** medium — đa số là wrappers, nhưng `get_ci_status` parse JSON từ `gh CLI`, dễ regression nếu gh output schema thay đổi.

### 5. `session-lock-guard.py` (185 lines, 0% line coverage)

Bash test (`test-session-lock-guard.sh`) cover end-to-end smoke nhưng không gom được Python coverage. Nếu logic phức tạp lên (parsing lock JSON, stale detection, owner check), cần Python unit tests.

**Follow-up đề xuất:** GAP-WAVE-76 "session-lock-guard.py Python unit tests" — convert bash smoke tests sang Python `subprocess` + thêm in-process tests cho `is_stale` / `parse_lock` helpers.

### 6. `pre-tool-guard.py` (75.0% line — còn ~31 lines gap)

Branches untested rải rác ở các Rule 1–5 edge cases. Đa số là defensive guards (malformed input, exit early). Acceptable medium risk; bias wave 76 vào audit-gate trước.

### 7. `post-tool-guard.py` (85.7%) + `stop-handoff-check.py` (79.1%) + `inject-rule-digest.py` (90.3%)

Đã coverage tốt sau Wave 74. Một số defensive `try/except` paths chưa cover — chấp nhận được.

## Recommendations

### Wave 76 follow-up gap candidates

Ưu tiên theo blast radius × current coverage:

| Title | Priority | Lý do |
|---|---|---|
| **GAP-WAVE-76: audit-gate.py `_on_pr_merge_impl` + `main` end-to-end coverage** | 🔴 P0 | Hook chạy ở mọi PR merge event, hiện 0% coverage; raise audit-gate từ 28% → ~60% |
| **GAP-WAVE-76: session-lock-guard.py Python unit test conversion** | 🟠 P1 | 0% Python coverage; bash test chỉ smoke; convert/extend để tránh regression khi logic phức tạp lên |
| **GAP-WAVE-76: audit-gate.py session telemetry + gh wrapper edge cases** | 🟡 P2 | gh JSON parsing là điểm dễ regression khi gh CLI version bump |
| **GAP-WAVE-76: pre-tool-guard.py edge case fill (75% → 90%)** | 🟡 P2 | Nâng floor; ROI vừa phải |

Đề xuất Wave 76 ship cluster GAP-WAVE-76-A (P0) + GAP-WAVE-76-B (P1) cùng wave nếu disjoint scope (audit-gate vs session-lock-guard) — phù hợp wave-pack pattern.

### Coverage CI threshold (deferred)

Wiring `.github/workflows/script-quality.yml` thêm step:

```yaml
- name: Hook coverage threshold
  run: |
    pip install --user coverage
    bash .claude/hooks/tests/run-coverage.sh
    python3 -m coverage report --fail-under=50
```

Threshold đề xuất khởi điểm: **50%** (ngang baseline hôm nay). Mỗi wave bump nếu coverage tăng. Không ship CI gate trong wave này — tránh block PR trên một metric chưa stabilize ≥7 ngày (per `incident-to-rule-pipeline.md` premature-rule guard).

Tracking: GAP follow-up đề xuất "wave-76 CI coverage threshold wiring" P2.

### Gitignore artifacts

Cần thêm vào `.gitignore` (nếu chưa có):

```
.coverage
.coverage.*
.claude/hooks/tests/coverage-html/
.claude/hooks/tests/coverage.xml
```

Wave này không ship gitignore edit (out of scope Bucket E strict); tracking trong recommendations.

## Verdict

🟡 **MEDIUM overall coverage (50.2% line / branch partial coverage chưa đo riêng).**

- 3/6 hooks ≥75% (post-tool-guard, stop-handoff-check, inject-rule-digest, pre-tool-guard) — tốt, defensive về regression.
- 2/6 hooks là điểm risk chính: `audit-gate.py` (28%) và `session-lock-guard.py` (0% Python).
- Baseline đủ để bias Wave 76 vào audit-gate `_on_pr_merge_impl` (highest blast radius × lowest coverage).
- CI threshold không nên ship cùng wave này per premature-rule guard.

## References

- Wave plan: `documents/03-planning/waves/wave-2026-05-14-75-meta-finish.md` §"Bucket E"
- Outside-in benchmark: `documents/04-quality/audits/meta/2026-05-14-wave-74-outside-in-benchmark.md` "Nhóm 3 CRITICAL #3"
- Parent gap: GAP-529 (Wave 75 meta finish)
- Sister waves: Wave 74 Bucket B (audit-gate 23 tests) + Wave 74 Bucket C (+17 tests post/stop)
- Coverage docs: https://coverage.readthedocs.io/en/7.14.0/

## Pre-handoff verify per `pre-handoff-self-test-completeness.md`

Bucket E là internal tooling — không touch user-facing flow. Verify checklist N/A. Self-test artifact-level:

- [x] `.coveragerc` config valid (no syntax error)
- [x] `run-coverage.sh` executable + idempotent (`coverage erase` first)
- [x] 5 Python test suites chạy under coverage không error (97 tests pass)
- [x] Baseline report shows per-hook line + branch %
- [x] Top 5 untested vùng identified
- [x] audit-gate.py branch < 50% → follow-up gap title proposed
- [x] Vietnamese narrative + English identifiers per `dev-readable-doc-language.md`
- [x] Local verify: `bash .claude/hooks/tests/run-coverage.sh` succeeds; report printed (lưu trong §"Coverage report" trên)
