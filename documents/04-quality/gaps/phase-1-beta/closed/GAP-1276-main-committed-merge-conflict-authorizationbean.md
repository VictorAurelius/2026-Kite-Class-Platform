# GAP-1276: Committed git merge conflict on main — `AuthorizationBean.java` (kiteclass-core did not compile)

**Status:** 🟢 DONE
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

- [x] Conflict markers removed from `AuthorizationBean.java`; kiteclass-core compiles (PR #2385)
- [x] CI gate prevents committed conflict markers / non-compiling code from reaching `main` (META follow-up — `.github/workflows/compile-gate.yml`)

## Log

- **2026-06-14:** META follow-up closed. Added `.github/workflows/compile-gate.yml` — path-UNFILTERED compile gate running on every PR + every push to `main`. Two jobs: (1) `conflict-markers` — `git grep` for committed git conflict markers (`<<<<<<<`/`>>>>>>>` at line start) across tracked source incl. test files; (2) `compile-all` — `./mvnw clean compile -DskipTests` on the `kitehub` aggregator (6 modules: platform/subscription/branding/email/admin/gateway) + standalone `kiteclass-core`. Closes the path-filter gap: `core-ci.yml`/`kitehub-ci.yml` are path-scoped so a conflict/compile break carried by an unrelated PR (or introduced at squash-merge time) slipped past; this gate compiles ALL modules regardless of which files the PR touched. `push:main` retained on purpose (vs solo-dev no-push policy) because a merge-time break exists only on merged HEAD, never in a candidate diff — a post-merge net is the point. Self-test verified: synthetic `AuthorizationBean.java` with conflict markers → grep guard flags lines 3+8 AND `javac` errors `illegal start of type` (→ `mvn compile` BUILD FAILURE) — both layers would have caught the originating incident. YAML validated via `python3 -c "import yaml; yaml.safe_load(...)"`. No human-walk needed (CI infra, not user-facing).

## Related

- Discovered in: branch `wave/rbac-lms-be-foundation` (this PR)
- Origin commits: `744807a71` (GAP-1165 hasAccessToSession), `c1920b6b8` (GAP-1139 OWNER tenant-admin)
- Rule: `admin-merge-discipline.md` (post-rebase wait + local verify) — recurrence-adjacent class
