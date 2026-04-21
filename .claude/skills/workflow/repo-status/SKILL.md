---
name: repo-status
description: "Dùng khi user nói 'status', 'repo status', 'tình trạng repo', 'health check', 'repo có ổn không', 'security', 'CVE', 'vuln', hoặc khi bắt đầu conversation mới cần đánh giá nhanh trạng thái remote repo. Checks: CI, PRs/branches, audit gaps, GitHub Security (Dependabot + code-scanning + secret-scanning) → output level GREEN/YELLOW/ORANGE/RED/BLACK."
user-invocable: true
argument-hint: "[--quick]"
---

# /repo-status — Remote Repo Health Check

**Usage:** `/repo-status` hoặc `/repo-status --quick`

Đánh giá nhanh sức khỏe remote repo qua **4 nhân tố**, output **status level**.

---

## Instructions

### Bước 1: Thu thập data bằng script

```bash
# Full report (colored terminal output)
./scripts/repo-status.sh

# JSON output (để parse trong skill)
./scripts/repo-status.sh --json

# Chỉ level (cho quick check)
./scripts/repo-status.sh --level
```

**Nếu `--quick`:** Chạy `--level` only, báo kết quả 1 dòng, xong.

### Bước 2: Phân tích kết quả

Đọc output từ script. Nếu cần chi tiết hơn script cung cấp, bổ sung bằng cách:

**Factor 1 — CI:** Nếu CI failing, xác định root cause:
```bash
# Tìm workflow fail gần nhất
FAILED_RUN=$(gh run list --branch main --limit 5 \
  --json databaseId,workflowName,conclusion \
  --jq '.[] | select(.conclusion=="failure") | .databaseId' | head -1)

# Xem log lỗi
gh run view $FAILED_RUN --log-failed 2>/dev/null | tail -30
```

**Factor 2 — PRs/Branches:** Nếu có stale branches, check xem đó là work-in-progress hay bị bỏ quên:
```bash
# Xem last commit date cho mỗi branch
git for-each-ref --sort=-committerdate --format='%(refname:short) %(committerdate:relative)' refs/remotes/ | grep -v "main\|HEAD"
```

**Factor 3 — Audit Gaps:** Đọc latest audit report để hiểu context gaps:
- Quality audit: `documents/04-quality/audits/quality/quality-audit-*.md` (mới nhất)
- UI audit: `documents/04-quality/audits/ui/ui-audit-issues-*.md` (mới nhất)
- Cross-check: gaps đã có PR fix chưa? (check merged PRs since audit date)

**Factor 4 — GitHub Security:** 3 endpoints via `gh api`:
```bash
REPO=$(gh repo view --json nameWithOwner --jq .nameWithOwner)
gh api "repos/$REPO/dependabot/alerts?state=open"       # → 403 if disabled
gh api "repos/$REPO/code-scanning/alerts?state=open"    # → [] if no scanner
gh api "repos/$REPO/secret-scanning/alerts?state=open"  # → public/enterprise only
```

Severity mapping:
- Dependabot `critical` → BLACK level; `high` → RED; `medium` → YELLOW
- Code scanning `error` → HIGH (counts as CVE); `warning` → MEDIUM; `note` → LOW
- Any secret-scanning alert → BLACK (credential exposure)
- Dependabot **disabled** (HTTP 403) → minimum ORANGE (silent-drift risk)

Nếu skill hoặc script phát hiện Dependabot disabled, **prompt user to enable** ở repo Settings → Code security and analysis.

### Bước 3: Output Report

```markdown
## Repo Status: [LEVEL] — [Label]

**Ngày:** [date]
**Branch:** main @ [commit hash]

### Factor 1: CI
- Status: ✅/❌ [details]
- Days since green: [N]
- Root cause (nếu fail): [mô tả]

### Factor 2: PRs & Branches
- Open PRs: [N]
- Stale branches: [N]  
- Details: [list]

### Factor 3: Audit Gaps
- Latest audit: [date] — [score]
- Unfixed P0: [N]
- Unfixed P1: [N]
- Unfixed P2: [N]

### Factor 4: GitHub Security
- Dependabot: enabled/disabled — [critical/high/medium/low counts]
- Code scanning: [N errors / N warnings / N notes] — top finding: [rule.id + file]
- Secret scanning: [N open alerts] (BLACK if >0)

### Recommended Actions
1. [Highest priority action]
2. [Next action]
3. ...
```

### Bước 4: Đề xuất actions

Dựa trên level, đề xuất actions cụ thể:

| Level | Action |
|-------|--------|
| **GREEN** | Tiếp tục development bình thường |
| **YELLOW** | Note minor items, fix khi convenient |
| **ORANGE** | Fix trước khi tạo PRs mới |
| **RED** | Fix ngay — ưu tiên cao nhất |
| **BLACK** | Stop all work, fix CI trước |

---

## Level Definitions

Xem chi tiết: `reference/level-definitions.md`

| Level | Label | Trigger |
|-------|-------|---------|
| **GREEN** | Healthy | CI xanh + 0 open PRs/stale branches + 0 P0/P1 gaps + **0 CVE + Dependabot enabled** |
| **YELLOW** | Minor Issues | CI xanh + minor gaps (P2/P3) HOẶC 1-2 stale branches HOẶC code-scan warnings |
| **ORANGE** | Needs Attention | CI xanh + P1 gaps, HOẶC >2 stale items, HOẶC **Dependabot disabled**, HOẶC ≥3 warnings |
| **RED** | Degraded | CI đỏ trên main HOẶC P0 gaps chưa fix HOẶC **≥1 HIGH CVE** |
| **BLACK** | Broken | CI đỏ >7 ngày HOẶC **CRITICAL CVE** HOẶC **secret scanning alert** |

---

## Rules

- LUÔN chạy script trước — không tự suy diễn status
- LUÔN giao tiếp tiếng Việt
- Nếu script fail (GitHub API unavailable) → báo rõ, không đoán
- Cross-reference audit gaps với merged PRs — gap có thể đã fix nhưng chưa re-audit
- Đề xuất actions phải actionable (có PR scope, estimate)

## Gotchas

- Script dùng `gh` CLI — cần authenticated (`gh auth status`)
- `ci_days_red` dựa trên last success date, không phải first failure date
- Audit gaps section parse bằng grep P0/P1/P2 markers — nếu report format thay đổi, script cần update
- Stale branches = unmerged into main, có thể là WIP hợp lệ — kiểm tra commit date trước khi recommend delete
- Windows (Git Bash): `date -d` có thể không work — script có fallback cho macOS nhưng chưa có cho Git Bash on Windows
- **Security factor (F4):** `gh api` to security endpoints returns 403 if Dependabot disabled. Script dùng jq type-check (`type=="array"`) để phân biệt disabled object vs empty-but-enabled array. **KHÔNG** grep response body — CVE descriptions có thể chứa từ "disabled" gây false positive (bài học từ GAP-202 first-cut implementation)
- Secret scanning endpoint chỉ available cho repo public hoặc GitHub Enterprise — trả 404 cho private repos → treat as "disabled" status
- Code-scanning alerts chỉ populate khi có CodeQL/Trivy/SARIF upload workflow — repo không có scanner = empty `[]`, không phải disabled
