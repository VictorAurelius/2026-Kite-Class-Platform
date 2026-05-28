---
audience: dev
---

# GAP-795 — `X-User-Id` UUID vs `Long` → UserContext null → JPA auditing (created_by) not populated

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-05-28 (Full regression RST walk — re-scoped after fix-time investigation per `audit-to-gap-pipeline.md` §2.8)
**Phase:** phase-1-beta
**Affects:** Mọi kiteclass-core write qua gateway — `created_by` / `updated_by` audit columns NULL (compliance/audit-trail)

## ⚠️ Re-scope note (per audit-to-gap-pipeline.md §2.8 — symptom diagnostic wrong from filing)

**Original filing (2026-05-28, P0):** "gateway tenant resolution vỡ cho CRUD routes — writes land `kiteclass_shared` not tenant DB `kiteclass_877dff9d`". **Misdiagnosis bởi RST agent** — assumed per-tenant-DB architecture + misread MDC `tenant=-` log artifact.

**Fix-time investigation 2026-05-28 (empirical, per §2.8) revealed premise FALSE:**
- Gateway logs: `Resolved tenant from JWT claim: 877dff9d` + `Routing to instance: sky-edu-test` → gateway sets `X-Tenant-Id` ĐÚNG
- kiteclass-core logs: `Tenant filter enabled for tenant: 877dff9d` → TenantContext + Hibernate filter active ĐÚNG
- DB: teacher id=1 `instance_id=877dff9d` ✅ tenant-tagged correctly; GET via gateway returns it (HTTP 200)
- **Kiến trúc thực = shared DB (`kiteclass_shared`) + Hibernate tenantFilter + RLS GUC** (NOT per-tenant DB). `kiteclass_877dff9d` empty = legacy unused DB. Teacher landing `kiteclass_shared` is CORRECT.
- MDC `tenant=-` = logging artifact (MDC field not wired to TenantContext) — red herring that misled agent.

→ **No P0 data-isolation bug.** Tenant resolution + tagging + filter all work. GAP-791/792 unblocked (no longer gated on this).

## Problem (corrected — the REAL bug surfaced same investigation)

`X-User-Id` header forwarded by gateway is a **UUID** (`b9fa3522-64e4-4ea8-93f4-d7aa43aea5c5`), nhưng kiteclass-core parse as `Long`:

```java
// TenantFilterInterceptor.java:106
Long userId = Long.parseLong(userIdHeader);   // UUID → NumberFormatException
UserContext.setCurrentUser(userId);            // never reached
// → log.warn("Invalid X-User-Id header format: b9fa3522-...")
```

`UserContext.CURRENT_USER` is `ThreadLocal<Long>` (UserContext.java:39) — legacy school numeric IDs. Gateway (post-JWT) forwards UUID `sub`/`userId`. Parse fails → UserContext NULL → JPA auditing `@CreatedBy`/`@LastModifiedBy` → NULL.

**Empirical:** `SELECT created_by FROM teachers WHERE id=1` → NULL (teacher created via gateway by Owner). Audit-trail compliance gap (PDPL Art 11 immutable audit needs actor id).

Recurrence: Wave meta-6 Bucket A #13 (same `UserContext ThreadLocal<Long>` vs gateway UUID class).

## Root Cause

Type mismatch: `UserContext.CURRENT_USER: ThreadLocal<Long>` (legacy numeric school user IDs) vs gateway forwarding UUID string from JWT `sub`/`userId` claim. `Long.parseLong(uuid)` throws → caught → UserContext stays null.

## Proposed Fix

Migrate `UserContext` from `Long` → `UUID` (or `String`):
- `UserContext.CURRENT_USER: ThreadLocal<UUID>` (or String)
- `TenantFilterInterceptor:106` `UUID.fromString(userIdHeader)` instead of `Long.parseLong`
- Sweep all `@RequestHeader("X-User-Id") Long` controller params + JpaConfig auditorProvider → UUID/String (~40+ touchpoints per Wave meta-6 #13 note)
- Per `cross-flow-bug-class-sweep.md`: grep all `X-User-Id` + `UserContext.getCurrentUser` consumers

## Acceptance Criteria

- [ ] `X-User-Id` UUID parsed without error; UserContext set to UUID
- [ ] teacher/student/course created via gateway → `created_by` = Owner UUID (not NULL)
- [ ] No `Invalid X-User-Id header format` warn in kiteclass-core logs
- [ ] Sweep all `X-User-Id Long` touchpoints reconciled to UUID
- [ ] RST re-walk: create resource via gateway → verify created_by populated

## Related

- Index: `documents/04-quality/audits/rst-html/2026-05-28-full-regression/INDEX.md`
- **GAP-790** (gateway TenantResolver staff-invitations) — tenant resolution works (this gap NOT about tenant)
- Recurrence: Wave meta-6 Bucket A #13 (UserContext UUID vs Long)
- `audit-to-gap-pipeline.md` §2.8 (fix-time state-check that caught the misdiagnosis)

## Log

- **2026-05-28 (re-scope):** Original P0 "gateway tenant resolution broken → shared DB" was misdiagnosis (RST agent assumed per-tenant-DB + misread MDC `tenant=-`). Fix-time investigation per §2.8 empirically verified: gateway resolves + core sets TenantContext + instance_id tagged + filter active — tenant isolation WORKS (shared-DB+filter architecture). Re-scoped to REAL bug surfaced same investigation: `X-User-Id` UUID vs `Long.parseLong` → UserContext null → `created_by` NULL. Downgraded P0→P1 (auditing, not data-isolation). GAP-791/792 unblocked. Gap-quality lesson: GAP-795 filed on agent symptom claim without empirical root-cause verify — §2.8 fix-time state-check caught it before wasted fix effort.
- **2026-05-28 (original filing):** Filed P0 from RST walk agent symptom (teacher count shared=1, tenant DB=0). See re-scope note.
