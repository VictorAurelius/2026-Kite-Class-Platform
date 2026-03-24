---
name: check-pr
description: Monitor CI và verify chất lượng PR — dùng scripts, không chạy lệnh trực tiếp
user-invocable: true
argument-hint: "[PR-number|branch-name]"
---

# /check-pr — Monitor CI & Verify PR Quality

## Usage

```
/check-pr 207              # Check PR #207
/check-pr wave/3           # Check branch wave/3
/check-pr                  # Check current branch
```

---

## Rules

1. **LUÔN dùng scripts** — không chạy `gh run list` trực tiếp nếu có script
2. **E2E verify ở local** — Docker phải up, chạy `./scripts/test-api-e2e.sh`
3. **CI monitor bằng script** — `bash scripts/check-ci.sh [branch]`
4. **Cleanup bằng script** — `bash scripts/cleanup-ci-runs.sh`

---

## Step 1: Identify Target

```bash
# Nếu PR number:
PR_NUMBER=$ARGUMENTS
BRANCH=$(gh pr view $PR_NUMBER --json headRefName --jq '.headRefName')

# Nếu branch name:
BRANCH=$ARGUMENTS

# Nếu không có argument:
BRANCH=$(git branch --show-current)
```

---

## Step 2: Monitor CI (bằng script)

```bash
# LUÔN dùng script
bash scripts/check-ci.sh $BRANCH

# Nếu timeout → tăng thời gian:
bash scripts/check-ci.sh $BRANCH 15  # 15 phút timeout
```

**Script location:** `scripts/check-ci.sh`
**Behavior:** Poll mỗi 15s, hiển thị status, exit 0 nếu all pass, exit 1 nếu fail.

**Lưu ý:** Script check TẤT CẢ runs trên branch (kể cả cũ). Nếu có failure cũ → check timestamp để phân biệt.

---

## Step 3: Analyze Failures (nếu CI fail)

```bash
# Tìm failed run ID
FAILED_RUN=$(gh run list --branch $BRANCH --limit 5 \
  --json databaseId,workflowName,conclusion \
  --jq '.[] | select(.conclusion=="failure") | .databaseId' | head -1)

# Xem log lỗi
gh run view $FAILED_RUN --log-failed 2>/dev/null | tail -30

# Tìm root cause
gh run view $FAILED_RUN --log-failed 2>/dev/null \
  | grep -E "ERROR|FAIL|Compilation|cannot find|Tests run.*Failures: [1-9]|Tests run.*Errors: [1-9]" \
  | head -10
```

---

## Step 4: Local Verification (TRƯỚC khi merge)

### 4a. E2E Tests (nếu Docker running)

```bash
# Check Docker status trước
cd kitehub && ./scripts/status.sh --health 2>/dev/null

# Nếu Docker up:
./scripts/wait-for-healthy.sh
./scripts/test-api-e2e.sh

# Nếu Docker down → ghi nhận:
# "⚠️ Docker not running — E2E skipped, rely on CI"
```

### 4b. Unit Tests (nếu JAVA_HOME available)

```bash
# KiteHub
cd kitehub && JAVA_HOME=/home/vkiet/jdk/jdk-21 ./mvnw clean test -pl kitehub-subscription -am -q

# KiteClass
cd kiteclass/kiteclass-core && JAVA_HOME=/home/vkiet/jdk/jdk-21 ./mvnw clean test -q
```

### 4c. Frontend Build

```bash
cd kitehub/kitehub-frontend && pnpm build
cd kiteclass/kiteclass-frontend && pnpm build
```

**Note:** Nếu không thể chạy local → ghi nhận và rely on CI. KHÔNG block merge vì thiếu local test — CI là minimum.

---

## Step 5: Output Report

```markdown
## PR Check Report: #[number] ([branch])

### CI Status
- [ ] All workflows: ✅/❌ (via `scripts/check-ci.sh`)
- Failed: [list if any]

### Local Verification
- [ ] E2E tests: ✅/❌/⏭️ skipped (Docker: up/down)
- [ ] Unit tests: ✅/❌/⏭️ skipped (JAVA_HOME: set/unset)
- [ ] Frontend build: ✅/❌/⏭️ skipped

### Issues Found
[Any failures with root cause]

### Verdict
✅ Ready to merge
❌ Fix needed: [description]
⚠️ Conditional: CI pass but local not verified
```

---

## Step 6: Cleanup (sau merge)

```bash
# Clean stale branches
git remote prune origin

# Clean worktrees
rm -rf .claude/worktrees/agent-* 2>/dev/null
git worktree prune

# Check remaining
git branch -r | grep -v "main\|HEAD\|wave/" | wc -l
```

---

## Integration với Wave Process

```
Per PR:  /check-pr [PR-number]          ← Sau agent tạo PR
Wave:    /check-pr wave/X               ← Sau merge tất cả PRs vào wave
Main:    /check-pr main                 ← Sau merge wave → main
Full:    /wave-completion-check [X]     ← 6-level check
```
