---
gap_id: GAP-707
title: LoginAuditService duplicate-row warn every login — pre-existing repository bug
status: OPEN
priority: P2
domain: Backend
phase: phase-1-beta
completion_pct: 0
filed_date: 2026-05-22
last_updated: 2026-05-22
filed_by: Wave 103 Bucket C side-find
---

# GAP-707 — LoginAuditService duplicate-row warn every login

## Problem

Every successful login emits log entry:

```
WARN  c.k.s.audit.LoginAuditService.recordLogin — 
  Query did not return a unique result: N results were returned
```

The `recordLogin` method uses a JPA repository query that expects a unique result but receives multiple — likely a stale-data query OR missing `LIMIT 1` / `findTop1ByXxx`.

**Severity:** P2 — log noise, no functional break:
- Login still succeeds (catch + log + continue path)
- BUT audit log row may be inconsistent if multiple rows match query and only one returned/written
- Pollutes log volume — every login adds 1 WARN line
- Future on-call risks false-positive alerts from log analysis pipeline

## Context

- Pre-existing bug — not introduced by Wave 103 Bucket C, just surfaced during 2FA test runs (12+ login attempts during enrollment + challenge flow exposed pattern)
- Repository method likely uses `findByUserId(userId)` returning single object — but multiple rows match (user has multiple login_audit entries)
- Filed as separate gap (not bundled with GAP-516/702/703/704/705/706) because root cause unrelated to Wave 103 self-test scope

## Proposed Fix

1. **Find** the repository method in `LoginAuditService.recordLogin` call chain
2. **Change** query semantics:
   - Either: rename to `findFirstByXxx` (Spring Data auto-bounds to 1 result)
   - Or: explicit `@Query("... ORDER BY created_at DESC LIMIT 1")`
   - Or: change `findByXxx` → `findAllByXxx` and pick first if expected to have N matches
3. **Add unit test** that reproduces the multi-match scenario
4. **Verify** via curl login 3 times → grep `kitehub-subscription` log for absence of WARN

## Acceptance Criteria

- [ ] LoginAuditService.recordLogin no longer emits "Query did not return a unique result" WARN
- [ ] Unit test reproduces multi-row scenario + asserts new behavior (deterministic single row)
- [ ] 5 consecutive login attempts → 0 WARN in service log
- [ ] Audit log row written matches intent (1 row per login event)

## Related

- Wave 103 audit (side-find): `documents/04-quality/audits/local-stack/2026-05-22-wave-103-2fa-totp-walk.md` §Bugs found
- LoginAuditService — pre-existing class (origin wave unknown, predates GAP-516 work)
