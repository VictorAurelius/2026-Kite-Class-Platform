# Skill: CI Cleanup Workflow

**Version:** 1.0
**Last Updated:** 2026-03-12
**Purpose:** Systematic cleanup of CI runs after feature branch completion

---

## 📋 Overview

CI runs accumulate storage and clutter GitHub Actions dashboard. This skill provides workflow for cleaning up runs while maintaining audit trail.

**Key Principle:** Delete failed runs on merged branches, keep last successful run for reference.

---

## 🎯 When to Use This Skill

**Use AFTER:**
- ✅ Merging feature branch to main
- ✅ Closing stale PRs (> 30 days no activity)
- ✅ Completing milestone (cleanup all merged PRs)

**DON'T use:**
- ❌ On main/develop/release branches (preserve history)
- ❌ On active feature branches (ongoing work)
- ❌ When debugging CI failures (keep for investigation)

---

## 🚨 CRITICAL: Preservation Rules

### Always Keep
- ✅ All runs on main/develop/release branches
- ✅ Last run per workflow on merged branch (any status)
- ✅ Runs from last 7 days (recent activity)

### Always Delete
- ❌ Failed runs on merged branches (older than 7 days)
- ❌ Duplicate runs (keep only latest per commit)
- ❌ Cancelled runs (user-triggered)

### Audit Trail Requirement
**MUST create audit log before deletion:**
```bash
gh run list --branch <branch> \
  --json databaseId,conclusion,createdAt,workflowName \
  > .log/ci-cleanup-<branch>-$(date +%Y%m%d).json
```

---

## ✅ Cleanup Workflow

### Step 1: Identify Completed Branches
```bash
# List recently merged PRs
gh pr list --state merged --limit 20 \
  --json number,headRefName,mergedAt
```

### Step 2: Create Audit Log
```bash
branch="feature/PR-X.X-description"

gh run list --branch "$branch" --limit 100 \
  --json databaseId,conclusion,createdAt,workflowName \
  > ".log/ci-cleanup-$branch-$(date +%Y%m%d).json"
```

### Step 3: Delete Runs (Keep Last)
```bash
# Get all run IDs except first (most recent)
gh run list --branch "$branch" --limit 100 | tail -n +2 | \
  awk '{print $NF}' > /tmp/delete_runs.txt

# Delete runs
while read run_id; do
  echo "Deleting run $run_id..."
  echo "y" | gh run delete "$run_id" 2>&1
done < /tmp/delete_runs.txt
```

**Note:** `gh run delete` requires interactive confirmation (no `--confirm` flag), use `echo "y" |` to auto-confirm.

### Step 4: Verify
```bash
gh run list --branch "$branch" --limit 5
# Expected: 0-1 runs
```

---

## 📊 Common Scenarios

### Scenario 1: After PR Merge (Normal Flow)
```bash
# 1. Merge PR
gh pr merge <PR-number> --squash -d

# 2. Create audit log
gh run list --branch <branch> \
  --json databaseId,conclusion,createdAt,workflowName \
  > .log/audit-<branch>.json

# 3. Get runs to delete (skip first)
gh run list --branch <branch> --limit 100 | tail -n +2 | \
  awk '{print $NF}' > /tmp/delete_runs.txt

# 4. Delete runs
while read run_id; do
  echo "y" | gh run delete "$run_id"
done < /tmp/delete_runs.txt

# Result: 1 run kept for reference
```

### Scenario 2: Project-wide Cleanup
```bash
# Use helper script
./scripts/cleanup-ci-runs.sh --merged-only --older-than 30
```

---

## 🔧 Helper Script

**Location:** `scripts/cleanup-ci-runs.sh`

**Usage:**
```bash
# Cleanup specific branch
./scripts/cleanup-ci-runs.sh --branch feature/PR-2.15-fix

# Cleanup all merged PRs
./scripts/cleanup-ci-runs.sh --merged-only

# Dry run (preview only)
./scripts/cleanup-ci-runs.sh --merged-only --dry-run
```

---

## ⚠️ Common Issues

**Issue 1: "unknown flag: --confirm" error**
- `gh run delete` does NOT support `--confirm` flag
- Fix: Use `echo "y" | gh run delete <run-id>` for auto-confirm

**Issue 2: "Resource not accessible" error**
- Need `repo` and `workflow` scopes
- Fix: `gh auth refresh -s delete_repo,workflow`

**Issue 3: Runs still visible after deletion**
- GitHub UI caching
- Wait 5-10 minutes, hard refresh browser

---

## 📊 Example Output

**Before cleanup:**
```
$ gh run list --branch feature/PR-2.15-fix-remaining-test-failures
# 46 runs (all failed - Docker infrastructure issues)
```

**After cleanup:**
```
$ gh run list --branch feature/PR-2.15-fix-remaining-test-failures
# 1 run (most recent, kept for audit)
```

**Audit log:**
```json
[
  {"databaseId":22986571919,"conclusion":"failure","createdAt":"2026-03-12T04:26:42Z","workflowName":"Build and Push KiteClass Docker Images"},
  {"databaseId":22986571906,"conclusion":"failure","createdAt":"2026-03-12T04:26:42Z","workflowName":"Core Service CI/CD"},
  ...
]
```

---

## 📚 Related Skills
- `.claude/skills/ci-cd-best-practices.md`
- `.claude/skills/development-workflow.md`
- `scripts/cleanup-ci-runs.sh`

---

## 🔄 Maintenance Schedule

**Recommended:**
- **After each PR merge:** Cleanup that branch (1-2 min)
- **Weekly:** Cleanup all merged PRs from past week (10-15 min)
- **Quarterly:** Full project cleanup (1-2 hours)

**Storage Impact:**
- Average: ~1-2 MB per run (logs + artifacts)
- 50 failed runs = ~100 MB
- GitHub free tier: 500 MB artifact storage

---

**Last Updated:** 2026-03-12
**Author:** KiteClass Team + Claude Sonnet 4.5
**Status:** ✅ Active
