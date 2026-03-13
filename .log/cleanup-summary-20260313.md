# CI Cleanup Summary - 2026-03-13

**Execution Time:** 2026-03-13 06:14 UTC
**Performed By:** Automated script + manual verification
**Policy Applied:** `.github/CI-CLEANUP-POLICY.md`

---

## Results

### Overall Statistics
- **Total runs before:** 174
- **Total runs after:** 147
- **Runs deleted:** 27
- **Success rate:** 93% reduction in failed runs

### Detailed Breakdown

| Branch | Before | After | Deleted |
|--------|--------|-------|---------|
| fix/testcontainers-ide-warnings | 27 runs (15 failed) | 12 runs (0 failed) | 15 failed runs |
| main | 106 runs (12 failed) | 96 runs (2 failed) | 10 failed runs |
| chore/fix-remaining-todos | 2 runs (1 failed) | 1 runs (0 failed) | 1 failed run |
| chore/replace-todo-with-future | 3 runs (1 failed) | 2 runs (0 failed) | 1 failed run |

### Policy Applied

✅ **Kept:**
- Last successful run per branch
- Latest 3 runs on main (including 2 failed for recent activity)

❌ **Deleted:**
- All failed runs on feature branches
- Old failed runs on main (kept only latest)

---

## Files Generated

1. **Audit log:** `.log/ci-cleanup-audit-20260313-061410.json`
   - Full snapshot of all 174 runs before cleanup

2. **Deletion list:** `/tmp/runs-to-delete.txt`
   - 27 run IDs that were deleted

3. **Execution log:** `/tmp/cleanup-output.log`
   - Detailed deletion results

---

## Next Steps

1. ✅ Monitor for 24h to ensure no issues
2. ⚠️  Consider implementing automated cleanup (GitHub Action)
3. ⚠️  Review policy: should we keep ANY failed runs on main?

---

**Status:** ✅ COMPLETED SUCCESSFULLY
