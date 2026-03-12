# CI Cleanup Policy

**Last Updated:** 2026-03-12
**Purpose:** Maintain clean CI history, reduce clutter, optimize GitHub Actions storage

---

## 🎯 Cleanup Triggers

### **1. After PR Merge (MANDATORY)**
- ✅ **Keep:** Last successful run only
- ❌ **Delete:** All failed runs from that branch
- ❌ **Delete:** Duplicate successful runs (keep final only)
- ❌ **Delete:** All cancelled runs

**Example:**
```bash
# After merging PR #57
feature/PR-4.16-password-encryption:
  ✅ KEEP:   Run 23014115982 (success, final)
  ❌ DELETE: Run 23013818044 (failed)
  ❌ DELETE: Run 23013597106 (failed)
  ❌ DELETE: Run 23013472634 (failed)
  ... (all other failed runs)
```

### **2. Weekly Cleanup (RECOMMENDED)**
Run every Sunday to clean:
- Failed runs older than 7 days
- Cancelled runs older than 7 days
- Duplicate successful runs (keep 1 per PR)

### **3. Main Branch Cleanup (CRITICAL)**
**Problem:** Main branch accumulates duplicate runs from merged PRs

**Solution:**
- ❌ Delete failed runs on main (they duplicate PR failures)
- ✅ Keep successful runs on main (proof of stable state)
- Exception: Keep latest 3 runs regardless of status

---

## 📋 What to Keep vs Delete

### ✅ ALWAYS KEEP
1. **Last successful run per PR** - Proof of CI passing before merge
2. **Latest 3 runs on main** - Recent main branch history
3. **Successful deployment runs** - Production evidence
4. **Runs referenced in documentation** - Historical reference

### ❌ ALWAYS DELETE
1. **Failed runs on feature branches** - No value after debugging
2. **Cancelled runs** - Interrupted, no useful info
3. **Duplicate successful runs** - Waste of storage
4. **Failed runs on main branch** - Duplicates from PRs

### ⚠️ CONDITIONAL
1. **Recent failed runs (< 24h)** - Keep for active debugging
2. **Failed runs on main** - If unique failure not from PR

---

## 🤖 Automation Strategy

### **Option A: GitHub Action (RECOMMENDED)**

Create `.github/workflows/cleanup-ci.yml`:

```yaml
name: Cleanup CI Runs

on:
  pull_request:
    types: [closed]
  schedule:
    - cron: '0 2 * * 0'  # Every Sunday at 2 AM UTC
  workflow_dispatch:

jobs:
  cleanup:
    runs-on: ubuntu-latest
    permissions:
      actions: write

    steps:
      - uses: actions/checkout@v4

      - name: Cleanup PR runs after merge
        if: github.event.pull_request.merged == true
        run: |
          BRANCH="${{ github.event.pull_request.head.ref }}"
          echo "Cleaning up branch: $BRANCH"

          # Get all runs for this branch
          runs=$(gh run list --branch "$BRANCH" --json databaseId,conclusion,status --limit 100)

          # Keep only last successful run
          success_run=$(echo "$runs" | jq -r '[.[] | select(.conclusion=="success")] | .[0].databaseId')

          # Delete all other runs
          echo "$runs" | jq -r '.[] | select(.databaseId != '$success_run') | .databaseId' | while read run_id; do
            echo "y" | gh run delete "$run_id" 2>/dev/null || true
          done
        env:
          GH_TOKEN: ${{ github.token }}

      - name: Weekly cleanup (scheduled)
        if: github.event_name == 'schedule'
        run: |
          # Delete failed runs older than 7 days
          cutoff=$(date -u -d '7 days ago' +%Y-%m-%dT%H:%M:%SZ)

          gh run list --limit 200 --json databaseId,conclusion,createdAt,headBranch \
            | jq -r --arg cutoff "$cutoff" \
              '.[] | select(.conclusion=="failure" or .conclusion=="cancelled")
               | select(.createdAt < $cutoff)
               | .databaseId' \
            | while read run_id; do
                echo "y" | gh run delete "$run_id" 2>/dev/null || true
              done
        env:
          GH_TOKEN: ${{ github.token }}
```

### **Option B: Manual Script (CURRENT)**

Update `scripts/cleanup-ci-runs.sh`:

```bash
#!/bin/bash
# Fixed version with proper line endings

set -euo pipefail

BRANCH="${1:-}"

if [ -z "$BRANCH" ]; then
  echo "Usage: $0 <branch-name>"
  exit 1
fi

echo "Cleaning up CI runs for branch: $BRANCH"

# Get all runs for branch
runs=$(gh run list --branch "$BRANCH" --json databaseId,conclusion --limit 100)

# Find last successful run
success_run=$(echo "$runs" | jq -r '[.[] | select(.conclusion=="success")] | .[0].databaseId // empty')

if [ -n "$success_run" ]; then
  echo "✅ Keeping successful run: $success_run"
fi

# Delete all failed and cancelled runs
echo "$runs" | jq -r --arg keep "$success_run" \
  '.[] | select(.conclusion=="failure" or .conclusion=="cancelled")
   | select(.databaseId != ($keep | tonumber))
   | .databaseId' \
  | while read -r run_id; do
      echo "Deleting run: $run_id"
      echo "y" | gh run delete "$run_id" 2>/dev/null || true
    done

echo "✅ Cleanup complete!"
```

---

## 📊 Retention Policy

| Type | Retention | Reason |
|------|-----------|--------|
| Failed runs on PRs | Delete after merge | No value after debugging |
| Cancelled runs | Delete after 24h | Interrupted, incomplete |
| Successful PR runs | Keep 1 (final) | Proof of CI passing |
| Successful main runs | Keep all | Main branch history |
| Failed main runs | Delete if duplicate | Avoid clutter |
| Deployment runs | Keep all | Audit trail |

**Storage Estimate:**
- Before cleanup: ~100 runs × 50MB = 5GB
- After cleanup: ~30 runs × 50MB = 1.5GB
- **Savings: 70%**

---

## 🔧 Implementation Checklist

For each PR merge:
- [ ] Wait for final CI run to complete
- [ ] Verify CI is green ✅
- [ ] Run cleanup script: `bash scripts/cleanup-ci-runs.sh <branch>`
- [ ] Verify only 1 successful run remains
- [ ] Merge PR
- [ ] Delete branch (auto-deletes via GitHub settings)

---

## 📈 Metrics to Track

Monthly review:
- Total CI runs count
- Storage used by Actions
- Average runs per PR
- Failed runs not cleaned up

Target KPIs:
- ✅ < 50 total runs at any time
- ✅ < 2GB Actions storage
- ✅ Average 2-3 runs per PR
- ✅ 0 failed runs older than 7 days

---

## 🚨 Current State (2026-03-12)

**Issues Found:**
```
42 failed runs (need cleanup)
11 cancelled runs (need cleanup)
18 failed runs on main (duplicates)
```

**Action Required:**
1. ✅ Cleanup PR 4.16 (DONE)
2. ⚠️ Cleanup all historical failed runs
3. ⚠️ Cleanup main branch duplicates
4. ⚠️ Implement automation (GitHub Action or hook)

---

## 📚 References

- [GitHub Actions: Managing workflow runs](https://docs.github.com/en/actions/managing-workflow-runs)
- [gh run delete](https://cli.github.com/manual/gh_run_delete)
- [Best practices for CI/CD hygiene](https://docs.github.com/en/actions/learn-github-actions/usage-limits-billing-and-administration)
