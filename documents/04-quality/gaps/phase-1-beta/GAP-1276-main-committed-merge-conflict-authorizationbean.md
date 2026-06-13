# GAP-1276: Committed git merge conflict on main — `AuthorizationBean.java` (kiteclass-core did not compile)

**Status:** 🟡 PARTIAL
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-14 (Wave rbac-lms-be-foundation — discovered while building kiteclass-core BE foundation)
**Affects:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/security/AuthorizationBean.java`

## Problem

`main` HEAD `16eb921c3` (== `origin/main`) shipped with **unresolved git conflict markers committed** in `AuthorizationBean.java` lines 334-383:

```
<<<<<<< HEAD
 * Check Spring Security context for an admin-equivalent role (bypass).
=======
 ... hasAccessToSession(...) method ...
>>>>>>> origin/main
```

This is a real compile blocker — `kiteclass-core` does NOT compile on `main`, so any kiteclass-core PR/CI run is broken. The conflict came from merging GAP-1165 (`hasAccessToSession`, PR #2303) against GAP-1139 (`isAdmin` javadoc, PR #2296); the squash/merge committed the markers unresolved.

Verified: `git show origin/main:.../AuthorizationBean.java | grep '<<<<<<<'` returns the markers (committed blob).

## Proposed Fix

Code conflict **resolved in this PR** (kept origin/main's `hasAccessToSession` GAP-1165 method + a single `isAdmin` javadoc). META follow-up (the PARTIAL slice): determine why a committed merge conflict reached `main` without CI catching it — add a compile/`merge-conflict-marker` gate so broken main cannot land (CI re-run or required status check on `main`).

## Acceptance Criteria

- [x] Conflict markers removed from `AuthorizationBean.java`; kiteclass-core compiles
- [ ] CI gate prevents committed conflict markers / non-compiling code from reaching `main` (META follow-up)

## Related

- Discovered in: branch `wave/rbac-lms-be-foundation` (this PR)
- Origin commits: `744807a71` (GAP-1165 hasAccessToSession), `c1920b6b8` (GAP-1139 OWNER tenant-admin)
- Rule: `admin-merge-discipline.md` (post-rebase wait + local verify) — recurrence-adjacent class
